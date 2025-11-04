package com.dentallink.domain.hospital.entity;

import com.dentallink.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
public class Hospital extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    private String hospitalName;
    private String hospitalDescription;
    private String hospitalAddress;
    private Boolean hospitalIsOpen;
    private String doctorName;
    private Long reservationCost;

    @OneToOne(mappedBy = "hospital", cascade = CascadeType.ALL, orphanRemoval = true)
    private HospitalSchedule hospitalSchedule;

    public Hospital(
            Long userId,
            String hospitalName,
            String hospitalDescription,
            String hospitalAddress,
            Boolean hospitalIsOpen,
            String doctorName,
            Long reservationCost
    ) {
        this.userId = userId;
        this.hospitalName = hospitalName;
        this.hospitalDescription = hospitalDescription;
        this.hospitalAddress = hospitalAddress;
        this.hospitalIsOpen = hospitalIsOpen;
        this.doctorName = doctorName;
        this.reservationCost = reservationCost;
    }

    public void deleteHospitalSchedule() {
        this.hospitalSchedule = null; // 케스케이드 삭제를 위한 관계 끊기
    }

    public void updateHospital(
            String hospitalName,
            String hospitalDescription,
            String hospitalAddress,
            Boolean hospitalIsOpen,
            String doctorName,
            Long reservationCost
    ) {
        this.hospitalName = hospitalName;
        this.hospitalDescription = hospitalDescription;
        this.hospitalAddress = hospitalAddress;
        this.hospitalIsOpen = hospitalIsOpen;
        this.doctorName = doctorName;
        this.reservationCost = reservationCost;
    }
}
