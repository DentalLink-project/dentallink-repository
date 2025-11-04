package com.dentallink.domain.paymentLog.repository;

import com.dentallink.domain.payment.entity.Payment;
import com.dentallink.domain.paymentLog.entity.PaymentLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentLogRepository extends JpaRepository<PaymentLog, Long> {
    Page<PaymentLog> findByPayment(Payment payment, Pageable pageable);
}
