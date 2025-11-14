package com.dentallink.domain.review.controller;

import com.dentallink.common.response.CommonApiResponse;
import com.dentallink.common.response.PageResponse;
import com.dentallink.domain.review.dto.request.ReviewCreateRequest;
import com.dentallink.domain.review.dto.request.ReviewUpdateRequest;
import com.dentallink.domain.review.dto.request.ReviewUpdateStatusRequest;
import com.dentallink.domain.review.dto.response.*;
import com.dentallink.domain.review.service.ReviewInternalService;
import com.dentallink.domain.user.dto.security.AuthUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import static com.dentallink.common.response.CommonApiResponse.*;

@Tag(name = "리뷰 관리", description = "병원 리뷰 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class ReviewController {
    private final ReviewInternalService reviewInternalService;

    // 리뷰 등록
    @Operation(summary = "리뷰 등록", description = "사용자가 병원 리뷰를 등록합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "리뷰 등록 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "409", description = "중복 리뷰")
    })
    @PostMapping("/hospitals/{hospitalId}/reviews")
    public ResponseEntity<CommonApiResponse<ReviewCreateResponse>> createReview(
            @PathVariable Long hospitalId,
            @RequestParam Long reservationId,
            @AuthenticationPrincipal AuthUser authUser,
            @RequestBody ReviewCreateRequest reviewCreateRequest
    ) {
        ReviewCreateResponse response = reviewInternalService.createReview(
                reservationId, hospitalId, authUser.getUserId(), reviewCreateRequest
        );
        return created (
                response, "리뷰가 작성되었습니다."
        );
    }

    // 병원 별 리뷰 목록 조회
    @Operation(summary = "병원 리뷰 목록 조회", description = "특정 병원의 리뷰 목록을 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "병원을 찾을 수 없음")
    })
    @GetMapping("/hospitals/{hospitalId}/reviews")
    public ResponseEntity<CommonApiResponse<PageResponse<ReviewListResponse>>> getAllReviews(
            @PathVariable Long hospitalId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return success (
                reviewInternalService.findAllReviews(hospitalId, page, size),
                "리뷰 목록이 조회되었습니다."
        );
    }

    // 리뷰 상세 조회
    @Operation(summary = "리뷰 상세 조회", description = "리뷰 단건 정보를 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "리뷰를 찾을 수 없음")
    })
    @GetMapping("/reviews/{id}")
    public ResponseEntity<CommonApiResponse<ReviewDetailResponse>> getReviewById(
            @PathVariable Long id
    ) {
        ReviewDetailResponse review = reviewInternalService.findReviewById(id);
        return success (
                review, "리뷰가 조회되었습니다."
        );
    }

    // 리뷰 수정
    @Operation(summary = "리뷰 수정", description = "사용자가 자신의 리뷰를 수정합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "수정 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "리뷰를 찾을 수 없음")
    })
    @PatchMapping("/reviews/{id}")
    public ResponseEntity<CommonApiResponse<ReviewUpdateResponse>> updateReview(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthUser authUser,
            @RequestBody ReviewUpdateRequest reviewUpdateRequest
    ) {
        ReviewUpdateResponse review = reviewInternalService.updateReview(id, authUser.getUserId(), reviewUpdateRequest);
        return success (
                review, "리뷰가 수정되었습니다."
        );
    }

    // 리뷰 삭제
    @Operation(summary = "리뷰 삭제", description = "사용자가 자신의 리뷰를 삭제합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "삭제 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "리뷰를 찾을 수 없음")
    })
    @DeleteMapping("/reviews/{id}")
    public ResponseEntity<CommonApiResponse<ReviewDeleteResponse>> deleteReviewById(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthUser authUser
    ) {
        ReviewDeleteResponse review = reviewInternalService.deleteReview(id, authUser.getUserId());
        return success (
                review, "리뷰가 삭제되었습니다."
        );
    }

    // 리뷰 상태 변경
    @Operation(summary = "리뷰 상태 변경", description = "관리자가 리뷰 상태를 변경합니다. PENDING → APPROVED 등")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "상태 변경 성공"),
            @ApiResponse(responseCode = "400", description = "유효하지 않은 상태 변경"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "리뷰를 찾을 수 없음")
    })
    @PatchMapping("/reviews/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<CommonApiResponse<ReviewStatusResponse>> updateReviewStatus(
            @PathVariable Long id,
            @RequestBody ReviewUpdateStatusRequest request
    ) {
        return success (
                reviewInternalService.updateReviewStatus(id, request),
                "리뷰 상태가 변경되었습니다."
        );
    }
}
