package com.dentallink.domain.pointLog.service;

import com.dentallink.domain.pointAccount.entity.PointAccount;
import com.dentallink.domain.pointLog.dto.response.PointLogResponse;
import com.dentallink.domain.pointLog.entity.PointLog;
import com.dentallink.domain.pointLog.enums.PointLogType;
import com.dentallink.domain.pointLog.repository.PointLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PointLogExternalService {
    private final PointLogRepository pointLogRepository;

    @Transactional
    public PointLogResponse createLog(PointAccount account, PointLogType type, Long amount) {
        PointLog log = PointLog.create(account, type, amount);
        pointLogRepository.save(log);
        return PointLogResponse.from(log);
    }
}
