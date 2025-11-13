package com.dentallink.domain.review.controller;

import com.dentallink.common.response.CommonApiResponse;
import com.dentallink.common.response.PageResponse;
import com.dentallink.domain.review.dto.request.ReviewCreateRequest;
import com.dentallink.domain.review.dto.request.ReviewUpdateRequest;
import com.dentallink.domain.review.dto.request.ReviewUpdateStatusRequest;
import com.dentallink.domain.review.dto.response.*;
import com.dentallink.domain.review.enums.ReviewStatus;
import com.dentallink.domain.review.service.ReviewInternalService;
import com.dentallink.domain.user.dto.security.AuthUser;
import com.dentallink.domain.user.enums.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReviewControllerTest {

    @Mock
    private ReviewInternalService reviewInternalService;

    @InjectMocks
    private ReviewController reviewController;

    private AuthUser authUser;
    private ReviewCreateResponse createResponse;
    private ReviewListResponse reviewList1;
    private ReviewListResponse reviewList2;
    private ReviewUpdateResponse updateResponse;
    private ReviewDeleteResponse deleteResponse;
    private ReviewStatusResponse statusResponse;

    @BeforeEach
    void setUp() {
        authUser = new AuthUser(1L, "user@example.com", UserRole.ROLE_USER);
        LocalDateTime now = LocalDateTime.now();

        createResponse = new ReviewCreateResponse(
                1L, 1L, 1L, 1L, 5, "좋은 병원", now, now
        );

        reviewList1 = new ReviewListResponse(1L, 1L, 1L, 1L, 5, "좋아요");
        reviewList2 = new ReviewListResponse(2L, 1L, 2L, 2L, 4, "괜찮아요");

        updateResponse = new ReviewUpdateResponse(1L, 5, "수정 완료", now);

        deleteResponse = new ReviewDeleteResponse(1L, 1L, now);

        statusResponse = new ReviewStatusResponse(1L, ReviewStatus.APPROVED, now);
    }

    @Test
    @DisplayName("리뷰 등록 성공")
    void createReview_success() {
        ReviewCreateRequest request = new ReviewCreateRequest(5, "좋은 병원");
        when(reviewInternalService.createReview(anyLong(), anyLong(), anyLong(), any()))
                .thenReturn(createResponse);

        ResponseEntity<CommonApiResponse<ReviewCreateResponse>> response =
                reviewController.createReview(1L, 1L, authUser, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData().id()).isEqualTo(1L);
        assertThat(response.getBody().getMessage()).contains("리뷰가 작성되었습니다.");
    }

    @Test
    @DisplayName("병원 리뷰 목록 조회 성공")
    void getAllReviews_success() {
        PageResponse<ReviewListResponse> pageResponse =
                new PageResponse<>(List.of(reviewList1, reviewList2), 2L, 1, 10, 0);

        when(reviewInternalService.findAllReviews(anyLong(), anyInt(), anyInt()))
                .thenReturn(pageResponse);

        ResponseEntity<CommonApiResponse<PageResponse<ReviewListResponse>>> response =
                reviewController.getAllReviews(1L, 1, 10);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData().getContent()).hasSize(2);
        assertThat(response.getBody().getMessage()).contains("리뷰 목록이 조회되었습니다.");
    }

    @Test
    @DisplayName("리뷰 단건 조회 성공")
    void getReviewById_success() {
        ReviewDetailResponse detailResponse = new ReviewDetailResponse(
                1L, 1L, 1L, 1L, 5, "좋아요", LocalDateTime.now(), LocalDateTime.now()
        );

        when(reviewInternalService.findReviewById(1L))
                .thenReturn(detailResponse);

        ResponseEntity<CommonApiResponse<ReviewDetailResponse>> response =
                reviewController.getReviewById(1L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData().id()).isEqualTo(1L);
        assertThat(response.getBody().getMessage()).contains("리뷰가 조회되었습니다.");
    }

    @Test
    @DisplayName("리뷰 수정 성공")
    void updateReview_success() {
        ReviewUpdateRequest request = new ReviewUpdateRequest(5, "수정 완료");
        when(reviewInternalService.updateReview(anyLong(), anyLong(), any()))
                .thenReturn(updateResponse);

        ResponseEntity<CommonApiResponse<ReviewUpdateResponse>> response =
                reviewController.updateReview(1L, authUser, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData().id()).isEqualTo(1L);
        assertThat(response.getBody().getMessage()).contains("리뷰가 수정되었습니다.");
    }

    @Test
    @DisplayName("리뷰 삭제 성공")
    void deleteReviewById_success() {
        when(reviewInternalService.deleteReview(anyLong(), anyLong()))
                .thenReturn(deleteResponse);

        ResponseEntity<CommonApiResponse<ReviewDeleteResponse>> response =
                reviewController.deleteReviewById(1L, authUser);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData().deletedAt()).isNotNull();
        assertThat(response.getBody().getMessage()).contains("리뷰가 삭제되었습니다.");
    }

    @Test
    @DisplayName("리뷰 상태 변경 성공")
    void updateReviewStatus_success() {
        ReviewUpdateStatusRequest request = new ReviewUpdateStatusRequest(ReviewStatus.APPROVED);
        when(reviewInternalService.updateReviewStatus(anyLong(), any()))
                .thenReturn(statusResponse);

        ResponseEntity<CommonApiResponse<ReviewStatusResponse>> response =
                reviewController.updateReviewStatus(1L, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData().status()).isEqualTo(ReviewStatus.APPROVED);
        assertThat(response.getBody().getMessage()).contains("리뷰 상태가 변경되었습니다.");
    }
}
