package com.dentallink.domain.review.entity;

import com.dentallink.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
public class Review extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "reservation_id", nullable = false)
    private Long reservationId;

    @Column(name = "hospital_id", nullable = false)
    private Long hospitalId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    private Integer point;
    private String content;

    public Review(
            Long reservationId,
            Long hospitalId,
            Long userId,
            Integer point,
            String content
    ) {
        this.reservationId = reservationId;
        this.hospitalId = hospitalId;
        this.userId = userId;
        this.point = point;
        this.content = content;
    }
}
