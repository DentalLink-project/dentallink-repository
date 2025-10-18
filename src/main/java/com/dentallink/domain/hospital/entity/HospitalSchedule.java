package com.dentallink.domain.hospital.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

@Entity
@Getter
@NoArgsConstructor
public class HospitalSchedule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hospitalId")
    private Hospital hospital;

    private LocalTime openTime;
    private LocalTime closeTime;
    private LocalTime breakStart;
    private LocalTime breakEnd;

    public HospitalSchedule(
            LocalTime openTime,
            LocalTime closeTime,
            LocalTime breakStart,
            LocalTime breakEnd,
            Hospital  hospital
    ){
        this.openTime = openTime;
        this.closeTime = closeTime;
        this.breakStart = breakStart;
        this.breakEnd = breakEnd;
        this.hospital = hospital;
    }
}
