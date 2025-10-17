package com.dentallink.domain.pointLog.service;

import com.dentallink.domain.pointAccount.entity.PointAccount;
import com.dentallink.domain.pointAccount.enums.PointAccountType;
import com.dentallink.domain.pointAccount.repository.PointAccountRepository;
import com.dentallink.domain.pointLog.dto.response.PointLogResponse;
import com.dentallink.domain.pointLog.entity.PointLog;
import com.dentallink.domain.pointLog.enums.PointLogType;
import com.dentallink.domain.pointLog.repository.PointLogRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public class PointLogInternalService {
    private final PointLogRepository pointLogRepository;

    //페이지 develop

}
