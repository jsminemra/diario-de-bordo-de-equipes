package com.diariodebordo.diariobordo.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Cache local de commits buscados na API do GitHub (US-19b/US-19c).
 * Um registro por commit; sha é único por usuário para tornar a
 * re-busca idempotente (upsert por existência).
 */
@Entity
@Table(name = "github_commits", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "sha"}))
@Data
public class GithubCommit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 40)
    private String sha;

    @Column(name = "commit_date", nullable = false)
    private LocalDateTime commitDate;

    @Column(columnDefinition = "TEXT")
    private String message;

    @Column(name = "fetched_at", nullable = false)
    private LocalDateTime fetchedAt;

    @PrePersist
    public void prePersist() {
        if (this.fetchedAt == null) {
            this.fetchedAt = LocalDateTime.now();
        }
    }
}
