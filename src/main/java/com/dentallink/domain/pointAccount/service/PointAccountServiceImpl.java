package com.dentallink.domain.pointAccount.service;

import com.dentallink.domain.pointAccount.entity.PointAccount;
import com.dentallink.domain.pointAccount.repository.PointAccountRepository;
import com.dentallink.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class PointAccountServiceImpl implements PointAccountService {
    private final PointAccountRepository pointAccountRepository;

    @Override
    public void createPointAccount(User user) {
        PointAccount account = PointAccount.create(user, 0L);
        pointAccountRepository.save(account);

    }
}
