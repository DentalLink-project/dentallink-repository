package com.dentallink.domain.hospital.service;

import com.dentallink.common.exception.GlobalException;
import com.dentallink.common.response.PageResponse;
import com.dentallink.domain.hospital.dto.request.HospitalCreateRequest;
import com.dentallink.domain.hospital.dto.request.HospitalScheduleCreateRequest;
import com.dentallink.domain.hospital.dto.request.HospitalScheduleUpdateRequest;
import com.dentallink.domain.hospital.dto.request.HospitalUpdateRequest;
import com.dentallink.domain.hospital.dto.response.*;
import com.dentallink.domain.hospital.entity.Hospital;
import com.dentallink.domain.hospital.entity.HospitalAvailableTime;
import com.dentallink.domain.hospital.entity.HospitalSchedule;
import com.dentallink.domain.hospital.exception.HospitalErrorCode;
import com.dentallink.domain.hospital.repository.HospitalAvailableTimeRepository;
import com.dentallink.domain.hospital.repository.HospitalRepository;
import com.dentallink.domain.hospital.repository.HospitalScheduleRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
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

@Service
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public class HospitalInternalService {
    private final HospitalRepository hospitalRepository;
    private final HospitalScheduleRepository hospitalScheduleRepository;
    private final HospitalAvailableTimeRepository hospitalAvailableTimeRepository;


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
    public PageResponse<HospitalListResponse> findAllHospitals(int page, int size) {
        Pageable pageable = PageRequest.of(page > 0 ? page - 1 : 0, size);
        Page<Hospital> hospitals = hospitalRepository.findAll(pageable);
        Page<HospitalListResponse> hospitalResponse = hospitals.map(HospitalListResponse::from);
        return PageResponse.fromPage(hospitalResponse);
    }

    // -------------------- 병원 CRUD --------------------

    // 병원 등록
    @Transactional
    public HospitalCreateResponse createHospital(Long userId, HospitalCreateRequest req) {
        Hospital hospital = new Hospital(
                userId,
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

    @Transactional(readOnly = true)
    public HospitalDetailResponse findHospitalById(Long id) {
        Hospital hospital = getHospitalById(id);
        return HospitalDetailResponse.of(hospital, hospital.getHospitalSchedule());
    }

    // 병원 수정
    @Transactional
    public HospitalUpdateResponse updateHospital(Long id, Long userId, HospitalUpdateRequest req) {
        Hospital hospital = getHospitalById(id);
        checkHospitalOwner(hospital, userId);

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
        checkHospitalOwner(hospital, userId);
        hospitalRepository.delete(hospital);
    }

    // -------------------- 병원 일정 CRUD --------------------

    // 병원 일정 등록
    @Transactional
    public HospitalScheduleCreateResponse createHospitalSchedule(Long hospitalId, Long userId, HospitalScheduleCreateRequest req) {
        Hospital hospital = getHospitalById(hospitalId);
        checkHospitalOwner(hospital, userId);

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
        checkHospitalOwner(hospital, userId);

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
        checkHospitalOwner(hospital, userId);

        HospitalSchedule schedule = getScheduleByHospitalId(hospitalId);
        hospitalScheduleRepository.delete(schedule);
        hospital.deleteHospitalSchedule();
    }

    // -------------------- 유틸 --------------------

    private void checkHospitalOwner(Hospital hospital, Long userId) {
        if (!hospital.getUserId().equals(userId)) {
            throw new GlobalException(HospitalErrorCode.NOT_HOSPITAL_OWNER);
        }
    }

    // -------------------- 병원 예약 시간 CRUD --------------------

    // 자정마다 다음날 예약 가능 시간 자동 생성
    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void updateHospitalAvailableTimes() {
        LocalDate targetDate = LocalDate.now().plusDays(1);
        List<Hospital> hospitals = hospitalRepository.findAll();

        for (Hospital hospital : hospitals) {
            var scheduleOpt = hospitalScheduleRepository.findByHospitalId(hospital.getId());
            if (scheduleOpt.isEmpty()) {
                continue;
            }

            var schedule = scheduleOpt.get();
            hospitalAvailableTimes(hospital, schedule, targetDate);
        }
    }

    // 병원 예약 가능 시간 자동 계산 로직
    @Transactional
    public void hospitalAvailableTimes (Hospital hospital, HospitalSchedule schedule, LocalDate date) {
        LocalTime open = schedule.getOpenTime();
        LocalTime close = schedule.getCloseTime();
        LocalTime breakStart = schedule.getBreakStart();
        LocalTime breakEnd = schedule.getBreakEnd();

        if (open == null || close == null) {
            throw new GlobalException(HospitalErrorCode.HOSPITAL_SCHEDULE_NOT_FOUND);
        }

        List<HospitalAvailableTime> slots = new ArrayList<>();

        LocalTime time = open;

        while (time.plusMinutes(30).isBefore(close) || time.plusMinutes(30).equals(close)) {
            boolean isDuringBreak = !time.isBefore(breakStart) && time.isBefore(breakEnd);

            if (!isDuringBreak) {
                LocalTime end = time.plusMinutes(30);
                slots.add(new HospitalAvailableTime(hospital, date, time, end));
            }

            time = time.plusMinutes(30);
        }

        hospitalAvailableTimeRepository.saveAll(slots);
    }

    // 병원 예약 가능 시간 자동 생성 테스트용
    @Transactional
    public void hospitalAvailableTimesTest(Long hospitalId, LocalDate date) {
        Hospital hospital = hospitalRepository.findById(hospitalId)
                .orElseThrow(() -> new GlobalException(HospitalErrorCode.HOSPITAL_NOT_FOUND));

        HospitalSchedule schedule = hospitalScheduleRepository.findByHospitalId(hospitalId)
                .orElseThrow(() -> new GlobalException(HospitalErrorCode.HOSPITAL_SCHEDULE_NOT_FOUND));

        hospitalAvailableTimes(hospital, schedule, date);
    }

    // TODO: 병원 예약 등록

    // TODO: 병원 예약 취소

}
