package com.dentallink.domain.hospital.service;

import com.dentallink.common.exception.GlobalException;
import com.dentallink.common.response.PageResponse;
import com.dentallink.domain.hospital.dto.request.*;
import com.dentallink.domain.hospital.dto.response.*;
import com.dentallink.domain.hospital.entity.Hospital;
import com.dentallink.domain.hospital.entity.HospitalSchedule;
import com.dentallink.domain.hospital.exception.HospitalErrorCode;
import com.dentallink.domain.hospital.repository.HospitalRepository;
import com.dentallink.domain.hospital.repository.HospitalScheduleRepository;
import com.dentallink.domain.user.entity.User;
import com.dentallink.domain.user.enums.UserRole;
import com.dentallink.domain.user.repository.UserRepository;
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

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("HospitalInternalService 단위 테스트 - CRUD, 권한, 엣지 케이스 포함")
class HospitalInternalServiceTest {

    @Mock
    private HospitalRepository hospitalRepository;

    @Mock
    private HospitalScheduleRepository hospitalScheduleRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private HospitalInternalService hospitalInternalService;

    private Hospital hospital;
    private HospitalSchedule schedule;
    private User adminUser;
    private User hospitalUser;
    private User normalUser;

    @BeforeEach
    void setUp() {
        hospital = new Hospital(
                "테스트치과",
                "좋은 치과",
                "서울시 강남구",
                true,
                "김원장",
                1000L
        );
        ReflectionTestUtils.setField(hospital, "id", 1L);

        schedule = HospitalSchedule.create(
                hospital,
                LocalTime.of(9, 0),
                LocalTime.of(18, 0),
                LocalTime.of(12, 0),
                LocalTime.of(13, 0)
        );
        ReflectionTestUtils.setField(schedule, "id", 1L);

        adminUser = User.of("admin@test.com", "pw", "관리자", UserRole.ROLE_ADMIN);
        ReflectionTestUtils.setField(adminUser, "id", 1L);

        hospitalUser = User.of("staff@test.com", "pw", "직원", UserRole.ROLE_HOSPITAL);
        ReflectionTestUtils.setField(hospitalUser, "id", 2L);
        hospitalUser.assignToHospital(1L);

        normalUser = User.of("user@test.com", "pw", "일반사용자", UserRole.ROLE_USER);
        ReflectionTestUtils.setField(normalUser, "id", 3L);
    }

    // ===================== 병원 CRUD =====================

    @Test
    @DisplayName("병원 등록 성공")
    void createHospital_Success() {
        HospitalCreateRequest req = new HospitalCreateRequest(
                "테스트치과",
                "좋은 치과",
                "서울 강남구",
                true,
                "김원장",
                1000L,
                LocalTime.of(9, 0),
                LocalTime.of(18, 0),
                LocalTime.of(12, 0),
                LocalTime.of(13, 0)
        );

        given(hospitalRepository.save(any(Hospital.class)))
                .willReturn(hospital);

        given(hospitalScheduleRepository.save(any(HospitalSchedule.class)))
                .willReturn(schedule);

        HospitalCreateResponse response = hospitalInternalService.createHospital(req);

        assertThat(response.hospitalName())
                .isEqualTo("테스트치과");

        assertThat(response.openTime())
                .isEqualTo(LocalTime.of(9, 0));

        then(hospitalRepository).should(times(1))
                .save(any(Hospital.class));

        then(hospitalScheduleRepository).should(times(1))
                .save(any(HospitalSchedule.class));
    }

    @Test
    @DisplayName("병원 수정 성공 - 병원 소속 유저")
    void updateHospital_Success_HospitalUser() {
        HospitalUpdateRequest req = new HospitalUpdateRequest(
                "새이름",
                "수정설명",
                "서울시 서초구",
                true,
                "이의사",
                2000L,
                LocalTime.of(10, 0),
                LocalTime.of(19, 0),
                LocalTime.of(13, 0),
                LocalTime.of(14, 0)
        );

        given(hospitalRepository.findById(1L))
                .willReturn(Optional.of(hospital));

        given(userRepository.findById(2L))
                .willReturn(Optional.of(hospitalUser));

        given(hospitalScheduleRepository.findByHospitalId(1L))
                .willReturn(Optional.of(schedule));

        HospitalUpdateResponse response = hospitalInternalService.updateHospital(1L, 2L, req);

        assertThat(response.hospitalName())
                .isEqualTo("새이름");

        assertThat(response.doctorName())
                .isEqualTo("이의사");
    }

    @Test
    @DisplayName("병원 수정 실패 - 권한 없음")
    void updateHospital_Fail_Unauthorized() {
        HospitalUpdateRequest req = new HospitalUpdateRequest(
                "새이름",
                "설명",
                "서울시",
                true,
                "김의사",
                2000L,
                LocalTime.of(9, 0),
                LocalTime.of(18, 0),
                LocalTime.of(12, 0),
                LocalTime.of(13, 0)
        );

        given(hospitalRepository.findById(1L))
                .willReturn(Optional.of(hospital));

        given(userRepository.findById(3L))
                .willReturn(Optional.of(normalUser));

        assertThatThrownBy(() ->
                hospitalInternalService.updateHospital(1L, 3L, req)
        ).isInstanceOf(GlobalException.class)
                .hasFieldOrPropertyWithValue("errorCode", HospitalErrorCode.NOT_HOSPITAL_OWNER);
    }

    @Test
    @DisplayName("병원 삭제 성공 - 관리자")
    void deleteHospital_Success_Admin() {
        given(hospitalRepository.findById(1L))
                .willReturn(Optional.of(hospital));

        given(userRepository.findById(1L))
                .willReturn(Optional.of(adminUser));

        hospitalInternalService.deleteHospital(1L, 1L);

        then(hospitalRepository).should(times(1))
                .delete(hospital);
    }

    @Test
    @DisplayName("병원 삭제 실패 - 권한 없음")
    void deleteHospital_Fail_Unauthorized() {
        given(hospitalRepository.findById(1L))
                .willReturn(Optional.of(hospital));

        given(userRepository.findById(3L))
                .willReturn(Optional.of(normalUser));

        assertThatThrownBy(() ->
                hospitalInternalService.deleteHospital(1L, 3L)
        ).isInstanceOf(GlobalException.class)
                .hasFieldOrPropertyWithValue("errorCode", HospitalErrorCode.NOT_HOSPITAL_OWNER);
    }

    // ===================== 병원 관계자 지정 =====================

    @Test
    @DisplayName("병원 관계자 등록 성공 - 관리자 권한")
    void assignHospitalMember_Success_Admin() {
        given(hospitalRepository.findById(1L))
                .willReturn(Optional.of(hospital));

        given(userRepository.findById(1L))
                .willReturn(Optional.of(adminUser));

        given(userRepository.findById(3L))
                .willReturn(Optional.of(normalUser));

        given(userRepository.save(any(User.class)))
                .willReturn(normalUser);

        String result = hospitalInternalService.assignHospitalMember(1L, 3L, 1L);

        assertThat(result)
                .contains("assigned to hospitalId=1");

        then(userRepository).should(times(1))
                .save(any(User.class));
    }

    @Test
    @DisplayName("병원 관계자 등록 실패 - 권한 없음")
    void assignHospitalMember_Fail_Unauthorized() {
        // 병원 조회 stub
        given(hospitalRepository.findById(1L))
                .willReturn(Optional.of(hospital));

        // 현재 사용자(normalUser) 조회 stub
        given(userRepository.findById(3L))
                .willReturn(Optional.of(normalUser));

        // assignHospitalMember 실행 시 권한 없음으로 예외 발생
        assertThatThrownBy(() ->
                hospitalInternalService.assignHospitalMember(1L, 2L, 3L)
        ).isInstanceOf(GlobalException.class)
                .hasFieldOrPropertyWithValue("errorCode", HospitalErrorCode.NOT_HOSPITAL_OWNER);
    }

    // ===================== 병원 일정 CRUD =====================

    @Test
    @DisplayName("병원 일정 등록 성공")
    void createHospitalSchedule_Success() {
        HospitalScheduleCreateRequest req = new HospitalScheduleCreateRequest(
                LocalTime.of(9, 0),
                LocalTime.of(18, 0),
                LocalTime.of(12, 0),
                LocalTime.of(13, 0)
        );

        given(hospitalRepository.findById(1L))
                .willReturn(Optional.of(hospital));

        given(userRepository.findById(2L))
                .willReturn(Optional.of(hospitalUser));

        given(hospitalScheduleRepository.findByHospitalId(1L))
                .willReturn(Optional.empty());

        given(hospitalScheduleRepository.save(any(HospitalSchedule.class)))
                .willReturn(schedule);

        HospitalScheduleCreateResponse response =
                hospitalInternalService.createHospitalSchedule(1L, 2L, req);

        assertThat(response.openTime())
                .isEqualTo(LocalTime.of(9, 0));

        then(hospitalScheduleRepository).should(times(1))
                .save(any(HospitalSchedule.class));
    }

    @Test
    @DisplayName("병원 일정 등록 실패 - 중복 스케줄")
    void createHospitalSchedule_Fail_Duplicate() {
        HospitalScheduleCreateRequest req = new HospitalScheduleCreateRequest(
                LocalTime.of(9, 0),
                LocalTime.of(18, 0),
                LocalTime.of(12, 0),
                LocalTime.of(13, 0)
        );

        given(hospitalRepository.findById(1L))
                .willReturn(Optional.of(hospital));

        given(userRepository.findById(2L))
                .willReturn(Optional.of(hospitalUser));

        given(hospitalScheduleRepository.findByHospitalId(1L))
                .willReturn(Optional.of(schedule));

        assertThatThrownBy(() ->
                hospitalInternalService.createHospitalSchedule(1L, 2L, req)
        ).isInstanceOf(GlobalException.class)
                .hasFieldOrPropertyWithValue("errorCode", HospitalErrorCode.DUPLICATE_SCHEDULE);
    }

    @Test
    @DisplayName("병원 일정 삭제 성공")
    void deleteHospitalSchedule_Success() {
        given(hospitalRepository.findById(1L))
                .willReturn(Optional.of(hospital));

        given(userRepository.findById(2L))
                .willReturn(Optional.of(hospitalUser));

        given(hospitalScheduleRepository.findByHospitalId(1L))
                .willReturn(Optional.of(schedule));

        hospitalInternalService.deleteHospitalSchedule(1L, 2L);

        then(hospitalScheduleRepository).should(times(1))
                .delete(schedule);
    }

    @Test
    @DisplayName("병원 일정 삭제 실패 - 스케줄 없음")
    void deleteHospitalSchedule_Fail_NoSchedule() {
        given(hospitalRepository.findById(1L))
                .willReturn(Optional.of(hospital));

        given(userRepository.findById(2L))
                .willReturn(Optional.of(hospitalUser));

        given(hospitalScheduleRepository.findByHospitalId(1L))
                .willReturn(Optional.empty());

        assertThatThrownBy(() ->
                hospitalInternalService.deleteHospitalSchedule(1L, 2L)
        ).isInstanceOf(GlobalException.class)
                .hasFieldOrPropertyWithValue("errorCode", HospitalErrorCode.HOSPITAL_SCHEDULE_NOT_FOUND);
    }

    // ===================== 병원 조회 =====================

    @Test
    @DisplayName("병원 목록 조회 성공")
    void findAllHospitals_Success() {
        PageRequest pageable = PageRequest.of(0, 10);

        // boolean isFavorite 인자 추가
        HospitalListResponse hospitalResponse = HospitalListResponse.of(hospital, false);

        // PageImpl 생성자에 pageable과 총 요소 수 전달
        PageImpl<HospitalListResponse> page = new PageImpl<>(
                List.of(hospitalResponse),
                pageable,
                1
        );

        given(hospitalRepository.findAllWithOptionalFavorite(adminUser.getId(), pageable))
                .willReturn(page);

        PageResponse<HospitalListResponse> response = hospitalInternalService.findAllHospitals(Math.toIntExact(adminUser.getId()), 10, 1L);

        assertThat(response.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("병원 상세 조회 성공")
    void findHospitalById_Success() {
        // boolean isFavorite 인자 추가
        HospitalDetailResponse detailResponse = HospitalDetailResponse.of(hospital, schedule, false);

        given(hospitalRepository.findHospitalDetailWithOptionalFavorite(hospital.getId(), adminUser.getId()))
                .willReturn(Optional.of(detailResponse));
        HospitalDetailResponse response = hospitalInternalService.findHospitalById(hospital.getId(), adminUser.getId());

        assertThat(response.hospitalName()).isEqualTo("테스트치과");
    }

    @Test
    @DisplayName("병원 상세 조회 실패 - 없는 병원")
    void findHospitalById_Fail_NotFound() {
        given(hospitalRepository.findHospitalDetailWithOptionalFavorite(999L, 1L))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> hospitalInternalService.findHospitalById(999L, 1L))
                .isInstanceOf(GlobalException.class)
                .hasFieldOrPropertyWithValue("errorCode", HospitalErrorCode.HOSPITAL_NOT_FOUND);
    }

}
