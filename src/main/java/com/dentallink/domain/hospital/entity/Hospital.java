package com.dentallink.domain.hospital.entity;

import com.dentallink.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

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

    @OneToOne(mappedBy = "hospital", cascade = CascadeType.ALL, orphanRemoval = true)
    private HospitalSchedule hospitalSchedule;

    public Hospital(
            Long userId,
            String hospitalName,
            String hospitalDescription,
            String hospitalAddress,
            Boolean hospitalIsOpen,
            String doctorName
    ) {
        this.userId = userId;
        this.hospitalName = hospitalName;
        this.hospitalDescription = hospitalDescription;
        this.hospitalAddress = hospitalAddress;
        this.hospitalIsOpen = hospitalIsOpen;
        this.doctorName = doctorName;
    }

    public void updateHospital(
            String name,
            String description,
            String address,
            Boolean isOpen,
            String doctorName
    ) {
        this.hospitalName = name;
        this.hospitalDescription = description;
        this.hospitalAddress = address;
        this.hospitalIsOpen = isOpen;
        this.doctorName = doctorName;
    }
}
