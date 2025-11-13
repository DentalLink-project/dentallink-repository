package com.dentallink.domain.hospital.service;

import com.dentallink.common.exception.GlobalException;
import com.dentallink.common.response.PageResponse;
import com.dentallink.domain.hospital.dto.request.*;
import com.dentallink.domain.hospital.dto.response.*;
import com.dentallink.domain.hospital.entity.Hospital;
import com.dentallink.domain.hospital.entity.HospitalReservationTime;
import com.dentallink.domain.hospital.entity.HospitalSchedule;
import com.dentallink.domain.hospital.exception.HospitalErrorCode;
import com.dentallink.domain.hospital.repository.HospitalReservationTimeRepository;
import com.dentallink.domain.hospital.repository.HospitalRepository;
import com.dentallink.domain.hospital.repository.HospitalScheduleRepository;
import com.dentallink.domain.user.entity.User;
import com.dentallink.domain.user.exception.UserErrorCode;
import com.dentallink.domain.user.repository.UserRepository;
import com.dentallink.domain.user.enums.UserRole;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public class HospitalInternalService {
    private final HospitalRepository hospitalRepository;
    private final HospitalScheduleRepository hospitalScheduleRepository;
    private final HospitalReservationTimeRepository hospitalReservationTimeRepository;
    private final UserRepository userRepository;


    // -------------------- 공용 조회 메서드 --------------------

    @Transactional(readOnly = true)
    public Hospital getHospitalById(Long id) {
        return hospitalRepository.findById(id)
                .orElseThrow(() -> new GlobalException(HospitalErrorCode.HOSPITAL_NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public HospitalSchedule getScheduleByHospitalId(Long hospitalId) {
        return hospitalScheduleRepository.findByHospitalId(hospitalId)
                .orElseThrow(() -> new GlobalException(HospitalErrorCode.HOSPITAL_SCHEDULE_NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public Optional<HospitalSchedule> findScheduleByHospitalId(Long hospitalId) {
        return hospitalScheduleRepository.findByHospitalId(hospitalId);
    }

    @Transactional(readOnly = true)
    public PageResponse<HospitalListResponse> findAllHospitals(int page, int size, Long userId) {
        Pageable pageable = PageRequest.of(page > 0 ? page - 1 : 0, size);
        Page<HospitalListResponse> hospitals = hospitalRepository.findAllWithOptionalFavorite(userId, pageable);
        return PageResponse.fromPage(hospitals);
    }

    @Transactional(readOnly = true)
    public HospitalDetailResponse findHospitalById(Long id, Long userId) {
        return hospitalRepository.findHospitalDetailWithOptionalFavorite(id, userId)
                .orElseThrow(() -> new GlobalException(HospitalErrorCode.HOSPITAL_NOT_FOUND));
    }

    // -------------------- 병원 CRUD --------------------

    // 병원 등록
    @Transactional
    public HospitalCreateResponse createHospital(HospitalCreateRequest req) {
        Hospital hospital = new Hospital(
                req.hospitalName(),
                req.hospitalDescription(),
                req.hospitalAddress(),
                req.hospitalIsOpen(),
                req.doctorName(),
                req.reservationCost()
        );
        Hospital savedHospital = hospitalRepository.save(hospital);

        HospitalSchedule schedule = new HospitalSchedule(
                req.openTime(),
                req.closeTime(),
                req.breakStart(),
                req.breakEnd(),
                savedHospital
        );
        HospitalSchedule savedSchedule = hospitalScheduleRepository.save(schedule);

        return HospitalCreateResponse.of(savedHospital, savedSchedule);
    }

    // 병원 관계자 지정(등록)
    @Transactional
    public String assignHospitalMember(Long hospitalId, Long targetUserId, Long currentUserId) {
        Hospital hospital = getHospitalById(hospitalId);
        User currentUser = findUserByIdOrThrow(currentUserId);

        // 권한: 시스템 관리자(ADMIN) 또는 해당 병원의 등록자(병원 대표)만 관계자 지정 가능
        boolean isAdmin = currentUser.getUserRole() == UserRole.ROLE_ADMIN;
        boolean isHospitalOwner = currentUser.getHospitalId() != null && currentUser.getHospitalId().equals(hospital.getId());

        if (!isAdmin && !isHospitalOwner) {
            throw new GlobalException(HospitalErrorCode.NOT_HOSPITAL_OWNER);
        }

        User targetUser = findUserByIdOrThrow(targetUserId);
        targetUser.assignToHospital(hospital.getId()); // 병원 관계자 지정
        userRepository.save(targetUser);

        return "userId=" + targetUserId + " assigned to hospitalId=" + hospitalId;
    }

    // 병원 수정
    @Transactional
    public HospitalUpdateResponse updateHospital(Long id, Long userId, HospitalUpdateRequest req) {
        Hospital hospital = getHospitalById(id);
        validateAuthorizedUser(userId, hospital);

        hospital.updateHospital(
                req.hospitalName(),
                req.hospitalDescription(),
                req.hospitalAddress(),
                req.hospitalIsOpen(),
                req.doctorName(),
                req.reservationCost()
        );

        HospitalSchedule schedule = getScheduleByHospitalId(id);
        schedule.updateSchedule(
                req.openTime(),
                req.closeTime(),
                req.breakStart(),
                req.breakEnd()
        );

        return HospitalUpdateResponse.of(hospital, schedule);
    }

    // 병원 삭제
    @Transactional
    public void deleteHospital(Long id, Long userId) {
        Hospital hospital = getHospitalById(id);
        validateAuthorizedUser(userId, hospital);
        hospitalRepository.delete(hospital);
    }

    // -------------------- 병원 일정 CRUD --------------------

    // 병원 일정 등록
    @Transactional
    public HospitalScheduleCreateResponse createHospitalSchedule(Long hospitalId, Long userId, HospitalScheduleCreateRequest req) {
        Hospital hospital = getHospitalById(hospitalId);
        validateAuthorizedUser(userId, hospital);

        if (findScheduleByHospitalId(hospitalId).isPresent()) {
            throw new GlobalException(HospitalErrorCode.DUPLICATE_SCHEDULE);
        }

        HospitalSchedule schedule = new HospitalSchedule(
                req.openTime(),
                req.closeTime(),
                req.breakStart(),
                req.breakEnd(),
                hospital
        );

        HospitalSchedule savedSchedule = hospitalScheduleRepository.save(schedule);
        return HospitalScheduleCreateResponse.of(savedSchedule);
    }

    // 병원 일정 수정
    @Transactional
    public HospitalScheduleUpdateResponse updateHospitalSchedule(Long hospitalId, Long userId, HospitalScheduleUpdateRequest req) {
        Hospital hospital = getHospitalById(hospitalId);
        validateAuthorizedUser(userId, hospital);

        HospitalSchedule schedule = getScheduleByHospitalId(hospitalId);
        schedule.updateSchedule(
                req.openTime(),
                req.closeTime(),
                req.breakStart(),
                req.breakEnd()
        );

        return HospitalScheduleUpdateResponse.of(schedule);
    }

    // 병원 일정 삭제
    @Transactional
    public void deleteHospitalSchedule(Long hospitalId, Long userId) {
        Hospital hospital = getHospitalById(hospitalId);
        validateAuthorizedUser(userId, hospital);

        HospitalSchedule schedule = getScheduleByHospitalId(hospitalId);
        hospitalScheduleRepository.delete(schedule);
        hospital.deleteHospitalSchedule();
    }

    // -------------------- 유틸 --------------------

    private User findUserByIdOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new GlobalException(UserErrorCode.USER_NOT_FOUND));
    }

    private void validateAuthorizedUser(Long userId, Hospital hospital) {
        User user = findUserByIdOrThrow(userId);
        checkHospitalOwner(hospital, user);
    }

    private void checkHospitalOwner(Hospital hospital, User user) {
        // 시스템 관리자면 통과
        if (user.getUserRole() == UserRole.ROLE_ADMIN) return;

        // 유저의 hospitalId가 병원 id와 같으면 통과
        if (user.getHospitalId() != null && user.getHospitalId().equals(hospital.getId())) return;

        // 그 외는 권한 없음
        throw new GlobalException(HospitalErrorCode.NOT_HOSPITAL_OWNER);
    }
}
