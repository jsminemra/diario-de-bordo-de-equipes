package com.diariodebordo.diariobordo.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Data
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Column(name = "github_username")
    private String githubUsername;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    private static final java.util.regex.Pattern BCRYPT_PATTERN =
            java.util.regex.Pattern.compile("^\\$2[aby]\\$\\d{2}\\$[A-Za-z0-9./]{53}$");

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        validatePasswordIsBCryptHash();
    }

    @PreUpdate
    public void preUpdate() {
        validatePasswordIsBCryptHash();
    }

    private void validatePasswordIsBCryptHash() {
        if (password == null || !BCRYPT_PATTERN.matcher(password).matches()) {
            throw new IllegalStateException(
                    "A senha deve ser armazenada como hash BCrypt ($2a$/$2b$, 60 caracteres); texto claro não é permitido.");
        }
    }

    public enum Role {
        MEMBER, LEADER, PROFESSOR
    }
}
