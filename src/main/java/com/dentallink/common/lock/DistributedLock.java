package com.dentallink.common.lock;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DistributedLock {
    String key(); // 락 키
    long waitTime() default 10L; // 락 대기 시간(초)
    long leaseTime() default 15L; // 락 유지 시간(초)
}
