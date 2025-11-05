package com.dentallink.common.lock;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.concurrent.TimeUnit;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class DistributedLockAspect {

    private final RedissonClient redissonClient;

    @Around("@annotation(distributedLock)")
    public Object applyLock(ProceedingJoinPoint joinPoint, DistributedLock distributedLock) throws Throwable {
        String key = distributedLock.key();
        RLock lock = redissonClient.getLock(key);
        boolean acquired = false;

        try {
            acquired = lock.tryLock(distributedLock.waitTime(), distributedLock.leaseTime(), TimeUnit.SECONDS);
            if (!acquired) {
                throw new IllegalStateException("다른 사용자가 예약 중입니다. 잠시 후 다시 시도해주세요.");
            }

            log.info("락 획득 성공: {}", key);

            Object result = joinPoint.proceed();

            return result;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("락 획득 중 인터럽트 발생", e);

        } finally {
            if (TransactionSynchronizationManager.isSynchronizationActive()) {
                // 트랜잭션이 존재하면 commit/rollback 모두 끝난 뒤 해제
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
                    @Override
                    public void afterCompletion(int status) {
                        if (lock.isHeldByCurrentThread()) {
                            lock.unlock();
                            if (status == STATUS_COMMITTED) {
                                log.info("락 해제 완료(afterCommit): {}", key);
                            } else {
                                log.warn("락 해제 완료(afterRollback): {}", key);
                            }
                        }
                    }
                });
            } else {
                // 트랜잭션이 없는 경우 즉시 해제
                if (acquired && lock.isHeldByCurrentThread()) {
                    lock.unlock();
                    log.info("락 해제 완료(noTx): {}", key);
                }
            }
        }
    }
}
