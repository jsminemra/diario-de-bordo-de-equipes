package com.diariodebordo.diariobordo.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "daily_entries")
@Data
public class DailyEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "sprint_id", nullable = false)
    private Sprint sprint;

    @Column(name = "what_was_done", nullable = false, columnDefinition = "TEXT")
    private String whatWasDone;

    @Column(name = "what_will_be_done", nullable = false, columnDefinition = "TEXT")
    private String whatWillBeDone;

    @Column(columnDefinition = "TEXT")
    private String impediments;

    @Column(name = "entry_date", nullable = false)
    private LocalDate entryDate;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.entryDate = LocalDate.now();
    }
}