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
public class HospitalReservationTime extends BaseEntity {
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

    @Column(name = "user_id")
    private Long userId;

    private LocalDateTime reservedAt;

    public HospitalReservationTime(
            Hospital hospital,
            LocalDate date,
            LocalTime startTime,
            LocalTime endTime
    ) {
        this.hospital = hospital;
        this.date = date;
        this.startTime = startTime;
        this.endTime = endTime;
        this.userId = null;
    }

    public void reserve(Long userId) {
        this.isReserved = true;
        this.userId = userId;
        this.reservedAt = LocalDateTime.now();
    }

    public void cancel() {
        this.isReserved = false;
        this.userId = null;
        this.reservedAt = null;
    }

}
