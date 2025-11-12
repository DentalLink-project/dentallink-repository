package com.dentallink.domain.chatbot.service;

import com.dentallink.domain.chatbot.dto.GeminiFunction;
import com.dentallink.domain.hospital.entity.Hospital;
import com.dentallink.domain.hospital.repository.HospitalRepository;
import com.dentallink.domain.reservation.dto.AvailableTimeSlotResponse;
import com.dentallink.domain.reservation.dto.ReservationResponse;
import com.dentallink.domain.reservation.service.ReservationInternalService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
@DisplayName("FunctionCallHandler - Gemini 함수 호출 테스트")
class FunctionCallHandlerTest {

    @Mock
    private ReservationInternalService reservationService;

    @Mock
    private HospitalRepository hospitalRepository;

    @InjectMocks
    private FunctionCallHandler functionCallHandler;

    private Map<String, Object> arguments;

    @BeforeEach
    void setUp() {
        arguments = new HashMap<>();
    }

    // ===== 예약 가능 시간 조회 테스트 =====

    @Test
    @DisplayName("예약 가능한 시간대를 조회할 수 있다")
    void shouldGetAvailableTimes() {
        // Given
        arguments.put("hospital_id", 1L);
        arguments.put("date", "2024-12-20");

        AvailableTimeSlotResponse slot1 = AvailableTimeSlotResponse.of(
                LocalDateTime.of(2024, 12, 20, 10, 0),
                3
        );
        AvailableTimeSlotResponse slot2 = AvailableTimeSlotResponse.of(
                LocalDateTime.of(2024, 12, 20, 10, 30),
                2
        );

        given(reservationService.getAvailableTimePeriod(1L, LocalDate.of(2024, 12, 20)))
                .willReturn(List.of(slot1, slot2));

        GeminiFunction.FunctionCall functionCall = new GeminiFunction.FunctionCall(
                "get_available_times",
                arguments
        );

        // When
        Object result = functionCallHandler.executeFunction(functionCall, 1L);

        // Then
        assertThat(result).isInstanceOf(Map.class);
        Map<String, Object> resultMap = (Map<String, Object>) result;
        assertThat(resultMap.get("hospital_id")).isEqualTo(1L);
        assertThat(resultMap.get("date")).isEqualTo("2024-12-20");
        assertThat((List<?>) resultMap.get("available_slots")).hasSize(2);
        then(reservationService).should().getAvailableTimePeriod(1L, LocalDate.of(2024, 12, 20));
    }

    // ===== 예약 생성 테스트 =====

    @Test
    @DisplayName("예약을 생성할 수 있다")
    void shouldCreateReservation() {
        // Given
        arguments.put("hospital_id", 1L);
        arguments.put("appointment_date", "2024-12-20T10:00:00");

        ReservationResponse reservation = new ReservationResponse(
                1L, 1L, "테스트치과", 1L, "테스트사용자",
                LocalDateTime.of(2024, 12, 20, 10, 0),
                com.dentallink.domain.reservation.enums.ReservationStatus.PENDING,
                1000L, LocalDateTime.now()
        );

        given(reservationService.createReservation(any(), eq(1L)))
                .willReturn(reservation);

        GeminiFunction.FunctionCall functionCall = new GeminiFunction.FunctionCall(
                "create_reservation",
                arguments
        );

        // When
        Object result = functionCallHandler.executeFunction(functionCall, 1L);

        // Then
        assertThat(result).isInstanceOf(Map.class);
        Map<String, Object> resultMap = (Map<String, Object>) result;
        assertThat(resultMap.get("success")).isEqualTo(true);
        assertThat(resultMap.get("hospital_name")).isEqualTo("테스트치과");
    }

    // ===== 내 예약 조회 테스트 =====

    @Test
    @DisplayName("내 예약 목록을 조회할 수 있다")
    void shouldGetMyReservations() {
        // Given
        ReservationResponse res1 = new ReservationResponse(
                1L, 1L, "테스트치과1", 1L, "테스트사용자",
                LocalDateTime.of(2024, 12, 20, 10, 0),
                com.dentallink.domain.reservation.enums.ReservationStatus.PENDING,
                1000L, LocalDateTime.now()
        );
        ReservationResponse res2 = new ReservationResponse(
                2L, 2L, "테스트치과2", 1L, "테스트사용자",
                LocalDateTime.of(2024, 12, 21, 14, 30),
                com.dentallink.domain.reservation.enums.ReservationStatus.APPROVED,
                1000L, LocalDateTime.now()
        );

        var pageable = PageRequest.of(0, 5);
        given(reservationService.getMyReservations(1L, pageable))
                .willReturn(new PageImpl<>(List.of(res1, res2), pageable, 2));

        GeminiFunction.FunctionCall functionCall = new GeminiFunction.FunctionCall(
                "get_my_reservations",
                new HashMap<>()
        );

        // When
        Object result = functionCallHandler.executeFunction(functionCall, 1L);

        // Then
        assertThat(result).isInstanceOf(Map.class);
        Map<String, Object> resultMap = (Map<String, Object>) result;
        assertThat(resultMap.get("total")).isEqualTo(2L);
        assertThat((List<?>) resultMap.get("reservations")).hasSize(2);
    }

    @Test
    @DisplayName("예약이 없으면 적절한 메시지를 반환한다")
    void shouldReturnMessageWhenNoReservations() {
        // Given
        var pageable = PageRequest.of(0, 5);
        given(reservationService.getMyReservations(1L, pageable))
                .willReturn(new PageImpl<>(List.of(), pageable, 0));

        GeminiFunction.FunctionCall functionCall = new GeminiFunction.FunctionCall(
                "get_my_reservations",
                new HashMap<>()
        );

        // When
        Object result = functionCallHandler.executeFunction(functionCall, 1L);

        // Then
        Map<String, Object> resultMap = (Map<String, Object>) result;
        assertThat(resultMap.get("message")).isEqualTo("예약 내역이 없습니다.");
    }

    // ===== 예약 취소 테스트 =====

    @Test
    @DisplayName("예약을 취소할 수 있다")
    void shouldCancelReservation() {
        // Given
        arguments.put("reservation_id", 1L);

        GeminiFunction.FunctionCall functionCall = new GeminiFunction.FunctionCall(
                "cancel_reservation",
                arguments
        );

        // When
        Object result = functionCallHandler.executeFunction(functionCall, 1L);

        // Then
        assertThat(result).isInstanceOf(Map.class);
        Map<String, Object> resultMap = (Map<String, Object>) result;
        assertThat(resultMap.get("success")).isEqualTo(true);
        assertThat(resultMap.get("reservation_id")).isEqualTo(1L);
        then(reservationService).should().cancelReservation(1L, 1L);
    }

    // ===== 병원 검색 (이름) 테스트 =====

    @Test
    @DisplayName("병원을 이름으로 검색할 수 있다")
    void shouldSearchHospitalsByName() {
        // Given
        arguments.put("keyword", "치과");

        Hospital hospital = new Hospital(
                "서울치과",
                "설명",
                "서울시 강남구",
                true,
                "김의사",
                1000L
        );
        ReflectionTestUtils.setField(hospital, "id", 1L);

        given(hospitalRepository.searchHospitalsByName(
                "치과",
                PageRequest.of(0, 5)
        )).willReturn(new PageImpl<>(List.of(hospital)));

        GeminiFunction.FunctionCall functionCall = new GeminiFunction.FunctionCall(
                "search_hospitals",
                arguments
        );

        // When
        Object result = functionCallHandler.executeFunction(functionCall, 1L);

        // Then
        assertThat(result).isInstanceOf(Map.class);
        Map<String, Object> resultMap = (Map<String, Object>) result;
        assertThat(resultMap.get("keyword")).isEqualTo("치과");
        assertThat((List<?>) resultMap.get("hospitals")).hasSize(1);
    }

    @Test
    @DisplayName("검색 결과가 없으면 메시지를 반환한다")
    void shouldReturnMessageWhenNoHospitalsFound() {
        // Given
        arguments.put("keyword", "존재하지않는병원");

        given(hospitalRepository.searchHospitalsByName(
                "존재하지않는병원",
                PageRequest.of(0, 5)
        )).willReturn(new PageImpl<>(List.of()));

        given(hospitalRepository.searchHospitalsByName(
                "",
                PageRequest.of(0, 50)
        )).willReturn(new PageImpl<>(List.of()));

        GeminiFunction.FunctionCall functionCall = new GeminiFunction.FunctionCall(
                "search_hospitals",
                arguments
        );

        // When
        Object result = functionCallHandler.executeFunction(functionCall, 1L);

        // Then
        Map<String, Object> resultMap = (Map<String, Object>) result;
        assertThat(resultMap.get("hospitals")).isInstanceOf(List.class);
        assertThat(((List<?>) resultMap.get("hospitals"))).isEmpty();
        assertThat(resultMap.get("message")).asString()
                .contains("찾을 수 없습니다");
    }

    // ===== 병원 검색 (위치) 테스트 =====

    @Test
    @DisplayName("병원을 위치/주소로 검색할 수 있다")
    void shouldSearchHospitalsByLocation() {
        // Given
        arguments.put("location", "강남");

        Hospital hospital = new Hospital(
                "강남치과",
                "설명",
                "서울시 강남구",
                true,
                "이의사",
                1000L
        );
        ReflectionTestUtils.setField(hospital, "id", 1L);

        given(hospitalRepository.searchHospitalsByLocation(
                "강남",
                PageRequest.of(0, 5)
        )).willReturn(new PageImpl<>(List.of(hospital)));

        GeminiFunction.FunctionCall functionCall = new GeminiFunction.FunctionCall(
                "search_hospitals_by_location",
                arguments
        );

        // When
        Object result = functionCallHandler.executeFunction(functionCall, 1L);

        // Then
        assertThat(result).isInstanceOf(Map.class);
        Map<String, Object> resultMap = (Map<String, Object>) result;
        assertThat(resultMap.get("location")).isEqualTo("강남");
        assertThat((List<?>) resultMap.get("hospitals")).hasSize(1);
    }

    // ===== 병원 검색 (의사) 테스트 =====

    @Test
    @DisplayName("병원을 의사명으로 검색할 수 있다")
    void shouldSearchHospitalsByDoctor() {
        // Given
        arguments.put("doctor_name", "김의사");

        Hospital hospital = new Hospital(
                "우리치과",
                "설명",
                "서울시 강남구",
                true,
                "김의사",
                1000L
        );
        ReflectionTestUtils.setField(hospital, "id", 1L);

        given(hospitalRepository.searchHospitalsByDoctor(
                "김의사",
                PageRequest.of(0, 5)
        )).willReturn(new PageImpl<>(List.of(hospital)));

        GeminiFunction.FunctionCall functionCall = new GeminiFunction.FunctionCall(
                "search_hospitals_by_doctor",
                arguments
        );

        // When
        Object result = functionCallHandler.executeFunction(functionCall, 1L);

        // Then
        assertThat(result).isInstanceOf(Map.class);
        Map<String, Object> resultMap = (Map<String, Object>) result;
        assertThat(resultMap.get("doctorName")).isEqualTo("김의사");
        assertThat((List<?>) resultMap.get("hospitals")).hasSize(1);
    }

    // ===== 알 수 없는 함수 테스트 =====

    @Test
    @DisplayName("알 수 없는 함수 호출 시 에러를 반환한다")
    void shouldReturnErrorForUnknownFunction() {
        // Given
        GeminiFunction.FunctionCall functionCall = new GeminiFunction.FunctionCall(
                "unknown_function",
                new HashMap<>()
        );

        // When
        Object result = functionCallHandler.executeFunction(functionCall, 1L);

        // Then
        assertThat(result).isInstanceOf(Map.class);
        Map<String, Object> resultMap = (Map<String, Object>) result;
        assertThat(resultMap.get("error")).asString()
                .contains("알 수 없는 함수입니다");
    }

    // ===== 함수 선언 테스트 =====

    @Test
    @DisplayName("함수 선언 목록을 조회할 수 있다")
    void shouldGetFunctionDeclarations() {
        // When
        List<GeminiFunction.FunctionDeclaration> declarations = functionCallHandler.getFunctionDeclarations();

        // Then
        assertThat(declarations).hasSize(7);
        assertThat(declarations.stream().map(GeminiFunction.FunctionDeclaration::name).toList())
                .containsExactlyInAnyOrder(
                        "get_available_times",
                        "create_reservation",
                        "get_my_reservations",
                        "cancel_reservation",
                        "search_hospitals",
                        "search_hospitals_by_location",
                        "search_hospitals_by_doctor"
                );
    }

    @Test
    @DisplayName("각 함수 선언에 필수 파라미터가 정의되어 있다")
    void shouldHaveFunctionParameters() {
        // When
        List<GeminiFunction.FunctionDeclaration> declarations = functionCallHandler.getFunctionDeclarations();

        // Then
        var getAvailableTimes = declarations.stream()
                .filter(d -> d.name().equals("get_available_times"))
                .findFirst();

        assertThat(getAvailableTimes).isPresent();
        var params = getAvailableTimes.get().parameters();
        var required = (List<?>) params.get("required");
        assertThat(required).hasSize(2);
    }

    // ===== 유틸리티 메서드 테스트 =====

    @Test
    @DisplayName("Long 값을 추출할 수 있다")
    void shouldExtractLongValue() {
        // Given
        arguments.put("hospital_id", 1L);

        GeminiFunction.FunctionCall functionCall = new GeminiFunction.FunctionCall(
                "get_my_reservations",
                arguments
        );

        // When & Then
        // 함수 실행 시 내부적으로 Long 추출이 일어남
        assertDoesNotThrow(() -> functionCallHandler.executeFunction(functionCall, 1L));
    }

    @Test
    @DisplayName("문자열 값을 추출할 수 있다")
    void shouldExtractStringValue() {
        // Given
        arguments.put("keyword", "병원이름");

        given(hospitalRepository.searchHospitalsByName(
                "병원이름",
                PageRequest.of(0, 5)
        )).willReturn(new PageImpl<>(List.of()));

        given(hospitalRepository.searchHospitalsByName(
                "",
                PageRequest.of(0, 50)
        )).willReturn(new PageImpl<>(List.of()));

        GeminiFunction.FunctionCall functionCall = new GeminiFunction.FunctionCall(
                "search_hospitals",
                arguments
        );

        // When
        Object result = functionCallHandler.executeFunction(functionCall, 1L);

        // Then
        assertThat(result).isInstanceOf(Map.class);
    }

    @Test
    @DisplayName("필수 파라미터가 없으면 예외를 발생시킨다")
    void shouldThrowExceptionWhenMissingRequiredParameter() {
        // Given
        arguments.put("keyword", "병원");
        // hospital_id를 빼먹음

        GeminiFunction.FunctionCall functionCall = new GeminiFunction.FunctionCall(
                "get_available_times",
                arguments
        );

        // When & Then
        assertThatThrownBy(() -> functionCallHandler.executeFunction(functionCall, 1L))
                .isInstanceOf(Exception.class);
    }

    // ===== 예외 처리 테스트 =====

    @Test
    @DisplayName("함수 실행 중 예외가 발생하면 에러 맵을 반환한다")
    void shouldReturnErrorMapWhenExceptionOccurs() {
        // Given
        arguments.put("hospital_id", 1L);
        arguments.put("date", "invalid-date");

        GeminiFunction.FunctionCall functionCall = new GeminiFunction.FunctionCall(
                "get_available_times",
                arguments
        );

        // When
        Object result = functionCallHandler.executeFunction(functionCall, 1L);

        // Then
        assertThat(result).isInstanceOf(Map.class);
        Map<String, Object> resultMap = (Map<String, Object>) result;
        assertThat(resultMap).containsKey("error");
    }
}