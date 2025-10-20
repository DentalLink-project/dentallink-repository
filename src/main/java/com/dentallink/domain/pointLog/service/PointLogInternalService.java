package com.dentallink.domain.pointLog.service;

import com.dentallink.common.response.PageResponse;
import com.dentallink.domain.pointAccount.entity.PointAccount;
import com.dentallink.domain.pointAccount.exception.InvalidPointAccountException;
import com.dentallink.domain.pointAccount.exception.PointAccountErrorCode;
import com.dentallink.domain.pointAccount.repository.PointAccountRepository;
import com.dentallink.domain.pointAccount.service.PointAccountExternalService;
import com.dentallink.domain.pointLog.dto.response.PointLogResponse;
import com.dentallink.domain.pointLog.entity.PointLog;
import com.dentallink.domain.pointLog.repository.PointLogRepository;
import com.dentallink.domain.user.entity.User;
import com.dentallink.domain.user.service.query.UserQueryService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public class PointLogInternalService {
    private final PointLogRepository pointLogRepository;
    private final PointAccountExternalService pointAccountExternalService;
    private final UserQueryService userQueryService;

    @Transactional(readOnly = true)
    public PageResponse<PointLogResponse> getLogsByUser(Long userId, int page, int size, String sort) {
        User user = userQueryService.getUserById(userId);
        PointAccount account = pointAccountExternalService.getPointAccountByUser(user);
        Sort order = "oldest".equalsIgnoreCase(sort)
                ? Sort.by("createdAt").ascending()
                : Sort.by("createdAt").descending();

        Pageable pageable = PageRequest.of(page, size, order);

        Page<PointLogResponse> pageResult = pointLogRepository.findByPointAccount(account, pageable)
                .map(PointLogResponse::from);

        return PageResponse.fromPage(pageResult);
    }

}
