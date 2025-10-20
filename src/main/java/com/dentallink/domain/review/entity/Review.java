package com.dentallink.domain.review.entity;

import com.dentallink.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

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

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;

    private Review(
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

    public static Review of(
            Long reservationId,
            Long hospitalId,
            Long userId,
            Integer point,
            String content
    ) {
        return new Review(
                reservationId,
                hospitalId,
                userId,
                point,
                content
        );
    }

    public void update(Integer point, String content) {
        this.point = point;
        this.content = content;
    }

    public void delete() {
        this.updatedAt = LocalDateTime.now();
        this.deletedAt = LocalDateTime.now();
    }

    public boolean isDeleted() {
        return this.deletedAt != null;
    }
}
