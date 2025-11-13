package com.dentallink.domain.review.service;

import com.dentallink.common.exception.GlobalException;
import com.dentallink.common.response.PageResponse;
import com.dentallink.domain.review.dto.request.ReviewCreateRequest;
import com.dentallink.domain.review.dto.request.ReviewUpdateRequest;
import com.dentallink.domain.review.dto.request.ReviewUpdateStatusRequest;
import com.dentallink.domain.review.dto.response.*;
import com.dentallink.domain.review.entity.Review;
import com.dentallink.domain.review.enums.ReviewStatus;
import com.dentallink.domain.review.exception.ReviewErrorCode;
import com.dentallink.domain.review.repository.ReviewRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewInternalServiceTest {

    @Mock
    private ReviewRepository reviewRepository;

    @InjectMocks
    private ReviewInternalService reviewInternalService;

    private Review review;

    @BeforeEach
    void setUp() {
        review = Review.of(1L, 1L, 1L, 5, "좋아요");
    }

    @Test
    @DisplayName("리뷰 등록 성공")
    void createReview_success() {
        ReviewCreateRequest request = new ReviewCreateRequest(5, "좋아요");

        when(reviewRepository.existsByReservationId(anyLong())).thenReturn(false);
        when(reviewRepository.save(any(Review.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ReviewCreateResponse response = reviewInternalService.createReview(1L, 1L, 1L, request);

        assertThat(response).isNotNull();
        assertThat(response.point()).isEqualTo(5);
        verify(reviewRepository, times(1)).save(any(Review.class));
    }

    @Test
    @DisplayName("리뷰 등록 실패 - 중복 리뷰")
    void createReview_fail_duplicate() {
        ReviewCreateRequest request = new ReviewCreateRequest(5, "좋아요");
        when(reviewRepository.existsByReservationId(anyLong())).thenReturn(true);

        assertThatThrownBy(() -> reviewInternalService.createReview(1L, 1L, 1L, request))
                .isInstanceOf(GlobalException.class)
                .hasFieldOrPropertyWithValue("errorCode", ReviewErrorCode.DUPLICATE_REVIEW);
    }

    @Test
    @DisplayName("리뷰 단건 조회 성공")
    void findReviewById_success() {
        when(reviewRepository.findById(anyLong())).thenReturn(Optional.of(review));

        ReviewDetailResponse response = reviewInternalService.findReviewById(1L);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(review.getId());
    }

    @Test
    @DisplayName("병원 리뷰 목록 조회 성공")
    void findAllReviews_success() {
        Review anotherReview = Review.of(2L, 1L, 2L, 4, "괜찮아요");

        Page<Review> reviewPage = new PageImpl<>(List.of(review, anotherReview),
                PageRequest.of(0, 10), 2);

        when(reviewRepository.findByHospitalId(anyLong(), any(Pageable.class))).thenReturn(reviewPage);

        PageResponse<ReviewListResponse> response = reviewInternalService.findAllReviews(1L, 0, 10);

        assertThat(response).isNotNull();
        assertThat(response.getContent()).hasSize(2);
    }

    @Test
    @DisplayName("리뷰 수정 성공")
    void updateReview_success() {
        ReviewUpdateRequest request = new ReviewUpdateRequest(4, "좋아요 수정");

        when(reviewRepository.findById(anyLong())).thenReturn(Optional.of(review));
        when(reviewRepository.save(any(Review.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ReviewUpdateResponse response = reviewInternalService.updateReview(1L, 1L, request);

        assertThat(response).isNotNull();
        assertThat(response.content()).isEqualTo("좋아요 수정");
        assertThat(response.point()).isEqualTo(4);
    }

    @Test
    @DisplayName("리뷰 삭제 성공")
    void deleteReview_success() {
        when(reviewRepository.findById(anyLong())).thenReturn(Optional.of(review));
        when(reviewRepository.save(any(Review.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ReviewDeleteResponse response = reviewInternalService.deleteReview(1L, 1L);

        assertThat(response).isNotNull();
        assertThat(review.isDeleted()).isTrue();
    }

    @Test
    @DisplayName("리뷰 상태 변경 성공 - PENDING → APPROVED")
    void updateReviewStatus_success() {
        when(reviewRepository.findById(anyLong())).thenReturn(Optional.of(review));
        when(reviewRepository.save(any(Review.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ReviewUpdateStatusRequest request = new ReviewUpdateStatusRequest(ReviewStatus.APPROVED);

        ReviewStatusResponse response = reviewInternalService.updateReviewStatus(1L, request);

        assertThat(response).isNotNull();
        assertThat(response.status()).isEqualTo(ReviewStatus.APPROVED);
        assertThat(review.getStatus()).isEqualTo(ReviewStatus.APPROVED);
        verify(reviewRepository, times(1)).save(review);
    }
}
