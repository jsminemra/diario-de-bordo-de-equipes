package com.diariodebordo.diariobordo.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "sprint_reports")
@Data
public class SprintReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "sprint_id", nullable = false)
    private Sprint sprint;

    @Column(name = "generated_at")
    private LocalDateTime generatedAt;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(name = "total_entries")
    private Integer totalEntries;

    @Column(name = "total_members")
    private Integer totalMembers;

    @Column(name = "members_with_entries")
    private Integer membersWithEntries;

    @PrePersist
    public void prePersist() {
        this.generatedAt = LocalDateTime.now();
    }
}