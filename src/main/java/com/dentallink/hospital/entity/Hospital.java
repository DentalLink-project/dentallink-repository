package com.dentallink.hospital.entity;

import com.dentallink.common.entity.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Auditable;

import java.time.LocalDateTime;

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
    private String hospitalImage;

    public Hospital(
            String hospitalName,
            String hospitalDescription,
            String hospitalAddress,
            Boolean hospitalIsOpen,
            String hospitalImage
    ) {
        this.hospitalName = hospitalName;
        this.hospitalDescription = hospitalDescription;
        this.hospitalAddress = hospitalAddress;
        this.hospitalIsOpen = hospitalIsOpen;
        this.hospitalImage = hospitalImage;
    }
}
