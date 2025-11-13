package com.dentallink.domain.hospital.controller;

import com.dentallink.common.response.CommonApiResponse;
import com.dentallink.common.response.PageResponse;
import com.dentallink.domain.hospital.dto.request.*;
import com.dentallink.domain.hospital.dto.response.*;
import com.dentallink.domain.hospital.service.HospitalInternalService;
import com.dentallink.domain.user.dto.security.AuthUser;
import com.dentallink.domain.user.enums.UserRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.ArgumentMatchers.*;

@ExtendWith(MockitoExtension.class)
class HospitalControllerTest {

    @Mock
    private HospitalInternalService hospitalInternalService;

    @InjectMocks
    private HospitalController hospitalController;

    @Test
    @DisplayName("병원 목록 조회 성공")
    void getAllHospitals_success() {
        AuthUser mockUser = new AuthUser(1L, "user@example.com", UserRole.ROLE_USER);

        HospitalListResponse hospitalResponse =
                new HospitalListResponse(1L, "테스트치과", "김원장", true, false);

        PageResponse<HospitalListResponse> mockPage =
                new PageResponse<>(List.of(hospitalResponse), 1L, 1, 10, 0);

        given(hospitalInternalService.findAllHospitals(1, 10, 1L))
                .willReturn(mockPage);

        ResponseEntity<CommonApiResponse<PageResponse<HospitalListResponse>>> response =
                hospitalController.getAllHospitals(1, 10, mockUser);

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData().getContent()).hasSize(1);
        assertThat(response.getBody().getData().getContent().get(0).hospitalName())
                .isEqualTo("테스트치과");
    }

    @Test
    @DisplayName("병원 단건 조회 성공")
    void getHospitalById_success() {
        AuthUser mockUser = new AuthUser(1L, "user@example.com", UserRole.ROLE_USER);

        HospitalDetailResponse detailResponse =
                new HospitalDetailResponse(
                        1L, "테스트치과", "좋은 치과", "서울 강남구",
                        true, "김원장", 1000L,
                        LocalTime.of(9,0), LocalTime.of(18,0),
                        LocalTime.of(12,0), LocalTime.of(13,0),
                        false
                );

        given(hospitalInternalService.findHospitalById(1L, 1L))
                .willReturn(detailResponse);

        ResponseEntity<CommonApiResponse<HospitalDetailResponse>> response =
                hospitalController.getHospitalById(1L, mockUser);

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData().hospitalName())
                .isEqualTo("테스트치과");
    }

    @Test
    @DisplayName("병원 등록 성공")
    void createHospital_success() {
        AuthUser mockUser = new AuthUser(1L, "admin@example.com", UserRole.ROLE_ADMIN);

        HospitalCreateRequest request =
                new HospitalCreateRequest(
                        "테스트치과", "좋은 치과", "서울 강남구",
                        true, "김원장", 1000L,
                        LocalTime.of(9,0), LocalTime.of(18,0),
                        LocalTime.of(12,0), LocalTime.of(13,0)
                );

        HospitalCreateResponse responseDto =
                new HospitalCreateResponse(
                        1L, "테스트치과", "좋은 치과", "서울 강남구",
                        true, "김원장", 1000L,
                        null, null,
                        LocalTime.of(9,0), LocalTime.of(18,0),
                        LocalTime.of(12,0), LocalTime.of(13,0)
                );

        given(hospitalInternalService.createHospital(request))
                .willReturn(responseDto);

        ResponseEntity<CommonApiResponse<HospitalCreateResponse>> response =
                hospitalController.createHospital(request);

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData().hospitalName())
                .isEqualTo("테스트치과");
    }

    @Test
    @DisplayName("병원 수정 성공")
    void updateHospital_success() {
        AuthUser mockUser = new AuthUser(1L, "hospital@example.com", UserRole.ROLE_HOSPITAL);

        HospitalUpdateRequest request =
                new HospitalUpdateRequest(
                        "테스트치과 수정", "좋은 치과", "서울 강남구",
                        true, "김원장", 1200L,
                        LocalTime.of(9,0), LocalTime.of(18,0),
                        LocalTime.of(12,0), LocalTime.of(13,0)
                );

        HospitalUpdateResponse responseDto =
                new HospitalUpdateResponse(
                        1L, "테스트치과 수정", "좋은 치과", "서울 강남구",
                        true, "김원장", 1200L,
                        LocalTime.of(9,0), LocalTime.of(18,0),
                        LocalTime.of(12,0), LocalTime.of(13,0)
                );

        given(hospitalInternalService.updateHospital(1L, mockUser.getUserId(), request))
                .willReturn(responseDto);

        ResponseEntity<CommonApiResponse<HospitalUpdateResponse>> response =
                hospitalController.updateHospital(1L, mockUser, request);

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData().hospitalName())
                .isEqualTo("테스트치과 수정");
    }

    @Test
    @DisplayName("병원 삭제 성공")
    void deleteHospital_success() {
        AuthUser mockUser = new AuthUser(1L, "admin@example.com", UserRole.ROLE_ADMIN);

        willDoNothing()
                .given(hospitalInternalService)
                .deleteHospital(1L, mockUser.getUserId());

        ResponseEntity<CommonApiResponse<Void>> response =
                hospitalController.deleteHospital(1L, mockUser);

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).contains("삭제되었습니다.");
    }

    @Test
    @DisplayName("병원 일정 등록 성공")
    void createHospitalSchedule_success() {
        AuthUser mockUser = new AuthUser(1L, "hospital@example.com", UserRole.ROLE_HOSPITAL);

        HospitalScheduleCreateRequest request =
                new HospitalScheduleCreateRequest(
                        LocalTime.of(9,0), LocalTime.of(18,0),
                        LocalTime.of(12,0), LocalTime.of(13,0)
                );

        HospitalScheduleCreateResponse responseDto =
                new HospitalScheduleCreateResponse(
                        1L, 1L,
                        LocalTime.of(9,0), LocalTime.of(18,0),
                        LocalTime.of(12,0), LocalTime.of(13,0)
                );

        given(hospitalInternalService.createHospitalSchedule(1L, mockUser.getUserId(), request))
                .willReturn(responseDto);

        ResponseEntity<CommonApiResponse<HospitalScheduleCreateResponse>> response =
                hospitalController.createHospitalSchedule(1L, mockUser, request);

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData().hospitalId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("병원 일정 수정 성공")
    void updateHospitalSchedule_success() {
        AuthUser mockUser = new AuthUser(1L, "hospital@example.com", UserRole.ROLE_HOSPITAL);

        HospitalScheduleUpdateRequest request =
                new HospitalScheduleUpdateRequest(
                        LocalTime.of(9,0), LocalTime.of(18,0),
                        LocalTime.of(12,0), LocalTime.of(13,0)
                );

        HospitalScheduleUpdateResponse responseDto =
                new HospitalScheduleUpdateResponse(
                        1L, LocalTime.of(9,0), LocalTime.of(18,0),
                        LocalTime.of(12,0), LocalTime.of(13,0)
                );

        given(hospitalInternalService.updateHospitalSchedule(1L, mockUser.getUserId(), request))
                .willReturn(responseDto);

        ResponseEntity<CommonApiResponse<HospitalScheduleUpdateResponse>> response =
                hospitalController.updateHospitalSchedule(1L, mockUser, request);

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData().openTime()).isEqualTo(LocalTime.of(9,0));
    }

    @Test
    @DisplayName("병원 일정 삭제 성공")
    void deleteHospitalSchedule_success() {
        AuthUser mockUser = new AuthUser(1L, "hospital@example.com", UserRole.ROLE_HOSPITAL);

        willDoNothing()
                .given(hospitalInternalService)
                .deleteHospitalSchedule(1L, mockUser.getUserId());

        ResponseEntity<CommonApiResponse<Void>> response =
                hospitalController.deleteHospitalSchedule(1L, mockUser);

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).contains("삭제되었습니다.");
    }
}
