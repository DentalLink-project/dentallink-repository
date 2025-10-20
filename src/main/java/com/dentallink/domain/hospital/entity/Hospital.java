package com.dentallink.domain.hospital.entity;

import com.dentallink.common.entity.BaseEntity;
import com.dentallink.domain.hospital.dto.request.HospitalUpdateRequest;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
public class Hospital extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String hospitalName;
    private String hospitalDescription;
    private String hospitalAddress;
    private Boolean hospitalIsOpen;
    private String doctorName;

    public Hospital(
            String hospitalName,
            String hospitalDescription,
            String hospitalAddress,
            Boolean hospitalIsOpen,
            String doctorName
    ) {
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
