package com.dentallink.domain.search.controller;

import com.dentallink.common.response.CommonApiResponse;
import com.dentallink.common.response.PageResponse;
import com.dentallink.domain.search.dto.response.SearchHospitalResponse;
import com.dentallink.domain.search.service.SearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static com.dentallink.common.response.CommonApiResponse.success;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/search")
public class SearchController {

    private final SearchService searchService;

    @Operation(summary = "병원 검색",
            description = "키워드를 중심으로 병원을 검색합니다..",
            responses = {
                    @ApiResponse(responseCode = "200", description = "조회 성공"),
                    @ApiResponse(responseCode = "403", description = "권한 없음")
            })
    @GetMapping
    public ResponseEntity<CommonApiResponse<PageResponse<SearchHospitalResponse>>> getSearchResult(
            @Parameter(description = "페이지") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "검색 키워드") @RequestParam String keyword
    ){
        return success(
                searchService.getSearchHospital(page, size, keyword),
                "성공적으로 조회되었습니다."
        );
    }

}
