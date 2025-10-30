package com.dentallink.domain.hospital.entity;

import com.dentallink.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Getter
@NoArgsConstructor
public class HospitalAvailableTime extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hospital_id", nullable = false)
    private Hospital hospital;

    private LocalDate date;
    private LocalTime startTime;
    private LocalTime endTime;
    private Boolean isReserved = false;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    private LocalDateTime reservedAt;

}
