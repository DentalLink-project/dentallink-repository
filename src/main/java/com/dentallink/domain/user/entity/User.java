package com.dentallink.domain.user.entity;


import com.dentallink.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import com.dentallink.domain.user.enums.UserRole;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Table(name = "users")
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String email;
    private String password;
    private String username;

    @Enumerated(EnumType.STRING)
    private UserRole userRole;

    private User (String email, String password, String username, UserRole userRole) {
        this.email = email;
        this.password = password;
        this.username = username;
        this.userRole = userRole;
    }

    public static User of(String email, String password, String username, UserRole userRole) {
        return new User(email, password, username, userRole);
    }

    public void update(String username, String email) {
        if(username != null) {
            this.username = username;
        }
        if(email != null) {
            this.email = email;
        }
    }

    public void updatePassword(String password) {
        this.password = password;
    }
}
