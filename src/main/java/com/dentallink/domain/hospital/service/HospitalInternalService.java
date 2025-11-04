package com.dentallink.domain.hospital.service;

import com.dentallink.common.exception.GlobalException;
import com.dentallink.common.response.PageResponse;
import com.dentallink.domain.favorite.repository.FavoriteRepository;
import com.dentallink.domain.hospital.dto.request.*;
import com.dentallink.domain.hospital.dto.response.*;
import com.dentallink.domain.hospital.entity.Hospital;
import com.dentallink.domain.hospital.entity.HospitalReservationTime;
import com.dentallink.domain.hospital.entity.HospitalSchedule;
import com.dentallink.domain.hospital.exception.HospitalErrorCode;
import com.dentallink.domain.hospital.repository.HospitalReservationTimeRepository;
import com.dentallink.domain.hospital.repository.HospitalRepository;
import com.dentallink.domain.hospital.repository.HospitalScheduleRepository;
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
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public class HospitalInternalService {
    private final HospitalRepository hospitalRepository;
    private final HospitalScheduleRepository hospitalScheduleRepository;
    private final HospitalReservationTimeRepository hospitalReservationTimeRepository;
    private final FavoriteRepository favoriteRepository;


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

        // 병원 목록 조회 (페이징)
        Page<Hospital> hospitals = hospitalRepository.findAll(pageable);

        // 로그인하지 않은 사용자 → 즐겨찾기 정보 없음
        if (userId == null) {
            return PageResponse.fromPage(
                    hospitals.map(h -> HospitalListResponse.of(h, false))
            );
        }

        // 병원 ID 목록 추출
        List<Long> hospitalIds = hospitals.stream()
                .map(Hospital::getId)
                .toList();

        // 해당 유저가 즐겨찾기한 병원들 한 번에 조회
        Set<Long> favoriteHospitalIds = favoriteRepository.findAll().stream()
                .filter(f -> f.getUser().getId().equals(userId) && hospitalIds.contains(f.getHospital().getId()))
                .map(f -> f.getHospital().getId())
                .collect(Collectors.toSet());

        // 병원 + 즐겨찾기 여부 매핑
        Page<HospitalListResponse> responsePage = hospitals.map(
                h -> HospitalListResponse.of(h, favoriteHospitalIds.contains(h.getId()))
        );

        return PageResponse.fromPage(responsePage);
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
    public HospitalDetailResponse findHospitalById(Long id, Long userId) {
        Hospital hospital = getHospitalById(id);

        boolean isFavorite = false;

        // 로그인 사용자인 경우만 즐겨찾기 여부 확인
        if (userId != null) {
            isFavorite = favoriteRepository.findByHospitalIdAndUserId(id, userId).isPresent();
        }

        return HospitalDetailResponse.of(hospital, hospital.getHospitalSchedule(), isFavorite);
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
            try {
                hospitalAvailableTimes(hospital, schedule, targetDate);
            } catch (GlobalException e){
                log.warn("{}: 해당 병원에는 open 또는 close 시간이 없어 생성할 수 없습니다.", e.getMessage());
            }

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

        List<HospitalReservationTime> slots = new ArrayList<>();

        LocalTime time = open;

        while (time.plusMinutes(30).isBefore(close) || time.plusMinutes(30).equals(close)) {
            boolean isDuringBreak = breakStart != null && breakEnd != null &&
                    !time.isBefore(breakStart) && time.isBefore(breakEnd);

            if (!isDuringBreak) {
                LocalTime end = time.plusMinutes(30);
                slots.add(new HospitalReservationTime(hospital, date, time, end));
            }

            time = time.plusMinutes(30);
        }

        hospitalReservationTimeRepository.saveAll(slots);
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

    // 병원 예약 가능 시간 조회
    @Transactional(readOnly = true)
    public List<HospitalReservationTimeResponse> getAvailableTimes(Long hospitalId, LocalDate date) {
        LocalDate targetDate = date != null ? date : LocalDate.now();

        List<HospitalReservationTime> times = hospitalReservationTimeRepository
                .findByHospital_IdAndDateOrderByStartTime(hospitalId, targetDate);

        return times.stream()
                .filter(t -> !t.isDeleted() && !Boolean.TRUE.equals(t.getIsReserved()))
                .map(t -> HospitalReservationTimeResponse.of(t.getId(), t.getStartTime(), t.getEndTime()))
                .collect(java.util.stream.Collectors.toList());
    }

    // 병원 예약 등록
    @Transactional
    public HospitalReservationResponse createHospitalReservation(Long hospitalId, Long userId, @Valid HospitalReservationCreateRequest request) {
        HospitalReservationTime availableTime = hospitalReservationTimeRepository.findById(request.availableTimeId())
                .orElseThrow(() -> new GlobalException(HospitalErrorCode.HOSPITAL_SCHEDULE_NOT_FOUND));

        if (!availableTime.getHospital().getId().equals(hospitalId)) {
            throw new GlobalException(HospitalErrorCode.HOSPITAL_NOT_FOUND);
        }

        if (availableTime.getIsReserved()) {
            throw new GlobalException(HospitalErrorCode.ALREADY_RESERVED); // 이미 예약됨
        }

        availableTime.reserve(userId);

        return new HospitalReservationResponse(
                hospitalId,
                userId,
                availableTime.getDate(),
                availableTime.getStartTime(),
                availableTime.getEndTime()
        );
    }

    // 예약 취소
    @Transactional
    public void cancelHospitalReservation(Long hospitalId, Long reservationId, Long userId) {
        HospitalReservationTime availableTime = hospitalReservationTimeRepository
                .findById(reservationId)
                .orElseThrow(() -> new GlobalException(HospitalErrorCode.HOSPITAL_SCHEDULE_NOT_FOUND));

        if (!availableTime.getHospital().getId().equals(hospitalId)) {
            throw new GlobalException(HospitalErrorCode.HOSPITAL_NOT_FOUND);
        }

        if (!availableTime.getIsReserved()) {
            throw new GlobalException(HospitalErrorCode.RESERVATION_NOT_FOUND);
        }

        if (!availableTime.getUserId().equals(userId)) {
            throw new GlobalException(HospitalErrorCode.NOT_RESERVATION_OWNER); // 본인 예약 아님
        }

        availableTime.cancel();
    }

}
