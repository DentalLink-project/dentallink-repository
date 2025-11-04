package com.dentallink.domain.pointLog.service;

import com.dentallink.common.response.PageResponse;
import com.dentallink.domain.pointAccount.entity.PointAccount;
import com.dentallink.domain.pointAccount.service.PointAccountExternalService;
import com.dentallink.domain.pointLog.dto.response.PointLogResponse;
import com.dentallink.domain.pointLog.repository.PointLogRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public class PointLogInternalService {

    private final PointLogRepository pointLogRepository;
    private final PointAccountExternalService pointAccountExternalService;

    @Transactional(readOnly = true)
    public PageResponse<PointLogResponse> getLogsByUser(
            Long userId,
            int page,
            int size,
            String sort,
            LocalDateTime startDate,
            LocalDateTime endDate
    ) {
        PointAccount account = pointAccountExternalService.getPointAccountByUserId(userId);

        Sort order = "oldest".equalsIgnoreCase(sort)
                ? Sort.by("createdAt").ascending()
                : Sort.by("createdAt").descending();

        Pageable pageable = PageRequest.of(page, size, order);

        Page<PointLogResponse> pageResult =
                pointLogRepository.findByPointAccountAndCreatedAtBetween(account, startDate, endDate, pageable)
                        .map(PointLogResponse::from);

        return PageResponse.fromPage(pageResult);
    }
}
