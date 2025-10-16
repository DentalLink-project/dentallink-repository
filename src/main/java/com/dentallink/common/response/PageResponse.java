package com.dentallink.common.response;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import org.springframework.data.domain.Page;

import java.util.List;

@Getter
public class PageResponse <T> {

    private final List<T> content;
    private final long totalElements;
    private final int totalPages;
    private final int size;
    private final int number;

    @JsonCreator // Jackson에게 역직렬화 시 이 생성자를 사용하라고 명시
    public PageResponse(
            @JsonProperty("content") List<T> content,           // JSON의 "content" 필드와 매핑
            @JsonProperty("totalElements") long totalElements,  // JSON의 "totalElements" 필드와 매핑
            @JsonProperty("totalPages") int totalPages,         // JSON의 "totalPages" 필드와 매핑
            @JsonProperty("size") int size,                     // JSON의 "size" 필드와 매핑
            @JsonProperty("number") int number                  // JSON의 "number" 필드와 매핑
    ) {
        this.content = content;
        this.totalElements = totalElements;
        this.totalPages = totalPages;
        this.size = size;
        this.number = number;
    }

    // Page -> PageResponse 변환 메서드
    public static <T> PageResponse<T> fromPage(Page<T> pagedData) {
        return new PageResponse<>(
                pagedData.getContent(),
                pagedData.getTotalElements(),
                pagedData.getTotalPages(),
                pagedData.getPageable().getPageSize(),
                pagedData.getPageable().getPageNumber()
        );
    }
}