package com.dentallink.domain.chatbot.service;

import com.dentallink.domain.chatbot.dto.GeminiFunction;
import com.dentallink.domain.chatbot.util.KoreanSearchUtil;
import com.dentallink.domain.hospital.repository.HospitalRepository;
import com.dentallink.domain.reservation.dto.AvailableTimeSlotResponse;
import com.dentallink.domain.reservation.dto.ReservationCreateRequest;
import com.dentallink.domain.reservation.dto.ReservationResponse;
import com.dentallink.domain.reservation.service.ReservationInternalService;
import com.google.gson.Gson;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class FunctionCallHandler {

    private final ReservationInternalService reservationService;
    private final HospitalRepository hospitalRepository;
    private final Gson gson = new Gson();

    //개선: 검색 결과 개수 제한 제거 (또는 대폭 증가)
    private static final int MAX_SEARCH_RESULTS = 100;  // 5개 → 100개

    /**
     * Function Call 실행
     */
    public Object executeFunction(GeminiFunction.FunctionCall functionCall, Long userId) {
        String functionName = functionCall.name();
        Map<String, Object> arguments = functionCall.arguments();

        log.info("Function Call 실행: {} with args: {}", functionName, arguments);

        try {
            return switch (functionName) {
                case "get_available_times" -> handleGetAvailableTimes(arguments);
                case "create_reservation" -> handleCreateReservation(arguments, userId);
                case "get_my_reservations" -> handleGetMyReservations(userId);
                case "cancel_reservation" -> handleCancelReservation(arguments, userId);
                case "search_hospitals" -> handleSearchHospitals(arguments);
                case "search_hospitals_by_location" -> handleSearchHospitalsByLocation(arguments);
                case "search_hospitals_by_doctor" -> handleSearchHospitalsByDoctor(arguments);
                default -> Map.of("error", "알 수 없는 함수입니다: " + functionName);
            };
        } catch (Exception e) {
            log.error("Function 실행 중 오류 발생: {}", functionName, e);
            return Map.of("error", e.getMessage());
        }
    }

    /**
     * 예약 가능 시간 조회
     */
    private Object handleGetAvailableTimes(Map<String, Object> arguments) {
        Long hospitalId = getLongValue(arguments, "hospital_id");
        String dateStr = getStringValue(arguments, "date");

        LocalDate date = LocalDate.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE);

        List<AvailableTimeSlotResponse> availableSlots =
                reservationService.getAvailableTimePeriod(hospitalId, date);

        Map<String, Object> result = new HashMap<>();
        result.put("hospital_id", hospitalId);
        result.put("date", dateStr);
        result.put("available_slots", availableSlots);
        result.put("summary", String.format("총 %d개의 예약 가능한 시간대가 있습니다.", availableSlots.size()));

        return result;
    }

    /**
     * 예약 생성
     */
    private Object handleCreateReservation(Map<String, Object> arguments, Long userId) {
        Long hospitalId = getLongValue(arguments, "hospital_id");
        String appointmentDateStr = getStringValue(arguments, "appointment_date");

        LocalDateTime appointmentDate = LocalDateTime.parse(
                appointmentDateStr,
                DateTimeFormatter.ISO_LOCAL_DATE_TIME
        );

        ReservationCreateRequest request = new ReservationCreateRequest(
                hospitalId,
                appointmentDate
        );

        ReservationResponse reservation = reservationService.createReservation(request, userId);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("reservation_id", reservation.id());
        result.put("appointment_date", reservation.appointmentDate());
        result.put("hospital_name", reservation.hospitalName());
        result.put("status", reservation.status());
        result.put("message", "예약이 완료되었습니다. 포인트 1000P가 차감되었습니다.");

        return result;
    }

    /**
     * 내 예약 목록 조회
     */
    private Object handleGetMyReservations(Long userId) {
        var reservations = reservationService.getMyReservations(
                userId,
                PageRequest.of(0, 10)  // 최근 10개
        );

        Map<String, Object> result = new HashMap<>();
        result.put("total", reservations.getTotalElements());
        result.put("reservations", reservations.getContent());

        if (reservations.isEmpty()) {
            result.put("message", "예약 내역이 없습니다.");
        } else {
            result.put("message", String.format("총 %d개의 예약이 있습니다.", reservations.getTotalElements()));
        }

        return result;
    }

    /**
     * 예약 취소
     */
    private Object handleCancelReservation(Map<String, Object> arguments, Long userId) {
        Long reservationId = getLongValue(arguments, "reservation_id");

        reservationService.cancelReservation(reservationId, userId);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("reservation_id", reservationId);
        result.put("message", "예약이 취소되었습니다. 포인트가 환불됩니다.");

        return result;
    }

    /**
     *개선: 병원 검색 (이름으로 검색) - 모든 결과 반환
     */
    private Object handleSearchHospitals(Map<String, Object> arguments) {
        String keyword = getStringValue(arguments, "keyword");

        log.info("병원 검색 (이름): keyword={}", keyword);

        //개선: 최대 결과 개수 대폭 증가 (5개 → 100개)
        var hospitals = hospitalRepository.searchHospitalsByName(keyword, PageRequest.of(0, MAX_SEARCH_RESULTS))
                .stream()
                .map(h -> Map.of(
                        "id", h.getId(),
                        "name", h.getHospitalName(),
                        "address", h.getHospitalAddress() != null ? h.getHospitalAddress() : "",
                        "doctorName", h.getDoctorName() != null ? h.getDoctorName() : ""
                ))
                .toList();

        // 검색 결과가 없으면 초성 검색 시도
        if (hospitals.isEmpty()) {
            log.info("정확 검색 결과 없음, 초성 검색 시도: keyword={}", keyword);

            var potentialMatches = hospitalRepository.searchHospitalsByName("", PageRequest.of(0, 200))
                    .stream()
                    .filter(h -> h.getHospitalName() != null &&
                            KoreanSearchUtil.matches(h.getHospitalName(), keyword))
                    .limit(MAX_SEARCH_RESULTS)
                    .map(h -> Map.of(
                            "id", h.getId(),
                            "name", h.getHospitalName(),
                            "address", h.getHospitalAddress() != null ? h.getHospitalAddress() : "",
                            "doctorName", h.getDoctorName() != null ? h.getDoctorName() : ""
                    ))
                    .toList();
            hospitals = potentialMatches;
        }

        Map<String, Object> result = new HashMap<>();
        result.put("keyword", keyword);
        result.put("hospitals", hospitals);

        if (hospitals.isEmpty()) {
            result.put("message", String.format("'%s' 병원을 찾을 수 없습니다. 다른 키워드로 검색해주세요.", keyword));
        } else {
            //개선: "5개" → "실제 검색된 개수" 표시
            result.put("message", String.format("'%s' 검색 결과: 총 %d개의 병원을 찾았습니다.", keyword, hospitals.size()));
            result.put("total_count", hospitals.size());
        }

        return result;
    }

    /**
     *개선: 병원 검색 (위치/주소로 검색) - 모든 결과 반환
     */
    private Object handleSearchHospitalsByLocation(Map<String, Object> arguments) {
        String location = getStringValue(arguments, "location");

        log.info("병원 검색 (위치): location={}", location);

        //개선: 최대 결과 개수 대폭 증가
        var hospitals = hospitalRepository.searchHospitalsByLocation(location, PageRequest.of(0, MAX_SEARCH_RESULTS))
                .stream()
                .map(h -> Map.of(
                        "id", h.getId(),
                        "name", h.getHospitalName(),
                        "address", h.getHospitalAddress() != null ? h.getHospitalAddress() : "",
                        "doctorName", h.getDoctorName() != null ? h.getDoctorName() : ""
                ))
                .toList();

        // 검색 결과가 없으면 부분 매칭 시도
        if (hospitals.isEmpty()) {
            log.info("정확 검색 결과 없음, 부분 매칭 시도: location={}", location);

            var potentialMatches = hospitalRepository.searchHospitalsByLocation("", PageRequest.of(0, 200))
                    .stream()
                    .filter(h -> h.getHospitalAddress() != null &&
                            (h.getHospitalAddress().contains(location) ||
                                    KoreanSearchUtil.matches(h.getHospitalAddress(), location)))
                    .limit(MAX_SEARCH_RESULTS)
                    .map(h -> Map.of(
                            "id", h.getId(),
                            "name", h.getHospitalName(),
                            "address", h.getHospitalAddress() != null ? h.getHospitalAddress() : "",
                            "doctorName", h.getDoctorName() != null ? h.getDoctorName() : ""
                    ))
                    .toList();
            hospitals = potentialMatches;
        }

        Map<String, Object> result = new HashMap<>();
        result.put("location", location);
        result.put("hospitals", hospitals);

        if (hospitals.isEmpty()) {
            result.put("message", String.format("'%s' 지역에서 병원을 찾을 수 없습니다. 다른 지역명으로 검색해주세요.", location));
        } else {
            //개선: 실제 검색된 개수 표시
            result.put("message", String.format("'%s' 지역 검색 결과: 총 %d개의 병원을 찾았습니다.", location, hospitals.size()));
            result.put("total_count", hospitals.size());
        }

        return result;
    }

    /**
     *개선: 병원 검색 (의사명으로 검색) - 모든 결과 반환
     */
    private Object handleSearchHospitalsByDoctor(Map<String, Object> arguments) {
        String doctorName = getStringValue(arguments, "doctor_name");

        log.info("병원 검색 (의사명): doctorName={}", doctorName);

        //개선: 최대 결과 개수 대폭 증가
        var hospitals = hospitalRepository.searchHospitalsByDoctor(doctorName, PageRequest.of(0, MAX_SEARCH_RESULTS))
                .stream()
                .map(h -> Map.of(
                        "id", h.getId(),
                        "name", h.getHospitalName(),
                        "address", h.getHospitalAddress() != null ? h.getHospitalAddress() : "",
                        "doctorName", h.getDoctorName()
                ))
                .toList();

        // 검색 결과가 없으면 초성 검색 시도
        if (hospitals.isEmpty()) {
            log.info("정확 검색 결과 없음, 초성 검색 시도: doctorName={}", doctorName);

            var potentialMatches = hospitalRepository.searchHospitalsByDoctor("", PageRequest.of(0, 200))
                    .stream()
                    .filter(h -> h.getDoctorName() != null &&
                            KoreanSearchUtil.matches(h.getDoctorName(), doctorName))
                    .limit(MAX_SEARCH_RESULTS)
                    .map(h -> Map.of(
                            "id", h.getId(),
                            "name", h.getHospitalName(),
                            "address", h.getHospitalAddress() != null ? h.getHospitalAddress() : "",
                            "doctorName", h.getDoctorName()
                    ))
                    .toList();
            hospitals = potentialMatches;
        }

        Map<String, Object> result = new HashMap<>();
        result.put("doctorName", doctorName);
        result.put("hospitals", hospitals);

        if (hospitals.isEmpty()) {
            result.put("message", String.format("'%s' 의사를 찾을 수 없습니다. 정확한 의사명으로 다시 검색해주세요.", doctorName));
        } else {
            // 개선: 실제 검색된 개수 표시
            result.put("message", String.format("'%s' 의사 검색 결과: 총 %d개의 병원을 찾았습니다.", doctorName, hospitals.size()));
            result.put("total_count", hospitals.size());
        }

        return result;
    }

    /**
     * 개선: Function 선언 목록 - 설명 강화
     */
    public List<GeminiFunction.FunctionDeclaration> getFunctionDeclarations() {
        return List.of(
                // 1. 예약 가능 시간 조회
                GeminiFunction.FunctionDeclaration.builder()
                        .name("get_available_times")
                        .description("특정 병원의 특정 날짜에 예약 가능한 시간대를 조회합니다. " +
                                "병원 ID와 날짜(YYYY-MM-DD)가 필요합니다.")
                        .parameters(Map.of(
                                "type", "object",
                                "properties", Map.of(
                                        "hospital_id", Map.of(
                                                "type", "number",
                                                "description", "병원 ID (검색 결과에서 얻은 ID)"
                                        ),
                                        "date", Map.of(
                                                "type", "string",
                                                "description", "조회할 날짜 (YYYY-MM-DD 형식, 예: 2025-12-25)"
                                        )
                                ),
                                "required", List.of("hospital_id", "date")
                        ))
                        .build(),

                // 2. 예약 생성
                GeminiFunction.FunctionDeclaration.builder()
                        .name("create_reservation")
                        .description("새로운 예약을 생성합니다. 포인트 1000P가 차감됩니다. " +
                                "병원 ID와 예약 날짜/시간이 필요합니다.")
                        .parameters(Map.of(
                                "type", "object",
                                "properties", Map.of(
                                        "hospital_id", Map.of(
                                                "type", "number",
                                                "description", "병원 ID"
                                        ),
                                        "appointment_date", Map.of(
                                                "type", "string",
                                                "description", "예약 날짜 및 시간 (YYYY-MM-DDTHH:mm:ss 형식, 예: 2025-12-25T14:30:00)"
                                        )
                                ),
                                "required", List.of("hospital_id", "appointment_date")
                        ))
                        .build(),

                // 3. 내 예약 조회
                GeminiFunction.FunctionDeclaration.builder()
                        .name("get_my_reservations")
                        .description("사용자의 모든 예약 목록을 조회합니다. 별도의 파라미터 없이 호출 가능합니다.")
                        .parameters(Map.of(
                                "type", "object",
                                "properties", Map.of()
                        ))
                        .build(),

                // 4. 예약 취소
                GeminiFunction.FunctionDeclaration.builder()
                        .name("cancel_reservation")
                        .description("예약을 취소합니다. 포인트가 환불됩니다. 예약 ID가 필요합니다.")
                        .parameters(Map.of(
                                "type", "object",
                                "properties", Map.of(
                                        "reservation_id", Map.of(
                                                "type", "number",
                                                "description", "취소할 예약 ID"
                                        )
                                ),
                                "required", List.of("reservation_id")
                        ))
                        .build(),

                // 5. 병원 검색 (이름)
                GeminiFunction.FunctionDeclaration.builder()
                        .name("search_hospitals")
                        .description("병원을 이름으로 검색합니다. DB에 있는 모든 결과를 반환합니다. " +
                                "예: '김해병원', '서울치과', '연세치과' 등")
                        .parameters(Map.of(
                                "type", "object",
                                "properties", Map.of(
                                        "keyword", Map.of(
                                                "type", "string",
                                                "description", "검색할 병원 이름 키워드 (예: 김해병원, 서울치과)"
                                        )
                                ),
                                "required", List.of("keyword")
                        ))
                        .build(),

                // 6. 병원 검색 (위치/주소)
                GeminiFunction.FunctionDeclaration.builder()
                        .name("search_hospitals_by_location")
                        .description("병원을 위치/주소로 검색합니다. DB에 있는 모든 결과를 반환합니다. " +
                                "지역명(예: 강남, 서초, 김해, 부산)이나 구체적인 주소(예: 강남구, 서초동)를 입력할 수 있습니다.")
                        .parameters(Map.of(
                                "type", "object",
                                "properties", Map.of(
                                        "location", Map.of(
                                                "type", "string",
                                                "description", "검색할 지역명 또는 주소 (예: 강남구, 서초동, 김해, 송파)"
                                        )
                                ),
                                "required", List.of("location")
                        ))
                        .build(),

                // 7. 병원 검색 (의사)
                GeminiFunction.FunctionDeclaration.builder()
                        .name("search_hospitals_by_doctor")
                        .description("특정 의사가 근무하는 병원을 검색합니다. DB에 있는 모든 결과를 반환합니다. " +
                                "의사 이름(예: 김철수, 이영희)을 입력합니다.")
                        .parameters(Map.of(
                                "type", "object",
                                "properties", Map.of(
                                        "doctor_name", Map.of(
                                                "type", "string",
                                                "description", "검색할 의사 이름 (예: 김철수, 이영희)"
                                        )
                                ),
                                "required", List.of("doctor_name")
                        ))
                        .build()
        );
    }

    // ===== 유틸리티 메서드 =====

    private Long getLongValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) {
            throw new IllegalArgumentException("필수 파라미터가 없습니다: " + key);
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return Long.parseLong(value.toString());
    }

    private String getStringValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) {
            throw new IllegalArgumentException("필수 파라미터가 없습니다: " + key);
        }
        return value.toString();
    }
}