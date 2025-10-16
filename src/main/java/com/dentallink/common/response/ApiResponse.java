package com.dentallink.common.response;

import com.dentallink.common.exception.ErrorCode;
import lombok.Getter;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;

@Getter
public class ApiResponse<T> {

    private final boolean success;
    private final String message;
    private final T data;
    private final LocalDateTime timestamp;

    private ApiResponse(boolean success, String message, T data, LocalDateTime timestamp) {
        this.success = success;
        this.message = message;
        this.data = data;
        this.timestamp = timestamp;
    }

    /**
     * 생성된 리소스에 대한 응답을 반환하는 메서드
     * 주어진 데이터를 포함하여 HTTP 201 Created 상태 코드와 메세지를 함께 응답을 반환
     *
     * @param data    생성된 리소스의 데이터
     * @param message 응답 메세지
     * @return HTTP 201 Created 응답과 함께 생성된 데이터가 포함된 ApiResponseDto
     */
    public static <T> ResponseEntity<ApiResponse<T>> created(T data, String message) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(new ApiResponse<>(true, message, data, LocalDateTime.now()));
    }

    /**
     * 성공적인 요청에 대한 응답을 반환하는 메서드
     * 주어진 데이터를 포함하여 HTTP 200 OK 상태 코드와 메세지를 함께 응답을 반환
     *
     * @param data    요청 성공 시 반환할 데이터
     * @param message 응답 메세지
     * @return HTTP 200 OK 응답과 함께 성공 데이터가 포함된 ApiResponseDto
     */
    public static <T> ResponseEntity<ApiResponse<T>> success(T data, String message) {
        return ResponseEntity.ok(new ApiResponse<>(true, message, data, LocalDateTime.now()));
    }

    /**
     * 성공적인 삭제 요청에 대한 응답을 반환하는 메서드
     * `data`는 항상 `null` 값을 반환
     *
     * @param message 응답 메세지
     * @return HTTP 200 OK 응답과 함께 응답 메세지만 반환
     */
    public static <T> ResponseEntity<ApiResponse<T>> deleteSuccess(String message) {
        return ResponseEntity.ok(new ApiResponse<>(true, message, null, LocalDateTime.now()));
    }

    /**
     * 에러에 대한 응답을 반환하는 메서드
     * 이 메서드는 `ErrorCode` 객체에서 제공하는 메세지를 사용하여 메세지를 반환
     * `data`는 항상 `null' 값을 반환하며 `success` 값은 `false`로 반환
     */
    public static <T> ApiResponse<T> error(ErrorCode error) {
        return new ApiResponse<>(false, error.getMessage(), null, LocalDateTime.now());
    }

    /**
     * 에러에 대한 응답을 반환하는 메서드
     * 이 메서드는 DTO 필드에 지정된 Valid 어노테이션에서의 메세지를 반환
     * `data`는 항상 `null' 값을 반환하며 `success` 값은 `false`로 반환
     */
    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(false, message, null, LocalDateTime.now());
    }

    /**
     * 성공적인 요청에 대한 페이지 응답을 반환하는 메서드
     * 주어진 Page 데이터를 PageResponse로 변환하여 HTTP 200 OK 상태 코드와 메세지를 함께 응답을 반환
     *
     * @param page    페이지로 조회된 데이터
     * @param message 응답 메세지
     * @return HTTP 200 OK 응답과 함께 PageResponse가 포함된 ApiResponse
     */
    public static <T> ResponseEntity<ApiResponse<PageResponse<T>>> pageSuccess(Page<T> page, String message) {
        PageResponse<T> data = PageResponse.fromPage(page);
        return ResponseEntity.ok(new ApiResponse<>(true, message, data, LocalDateTime.now()));
    }

    public static <T> ResponseEntity<ApiResponse<PageResponse<T>>> pageSuccess(PageResponse<T> response, String message) {
        return ResponseEntity.ok(new ApiResponse<>(true, message, response, LocalDateTime.now()));
    }
}


