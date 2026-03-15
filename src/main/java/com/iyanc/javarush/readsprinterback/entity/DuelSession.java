package com.iyanc.javarush.readsprinterback.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "duel_sessions")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DuelSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "exercise_type", nullable = false)
    private String exerciseType;

    @Column(name = "grid_size", nullable = false)
    private int gridSize;

    @Column(name = "font_size", nullable = false)
    private int fontSize;

    /** JSON array string, e.g. "[3,17,5,...]" */
    @Column(name = "numbers_sequence", nullable = false, length = 2048)
    private String numbersSequence;

    @Column(name = "status", nullable = false)
    private String status; // WAITING | COUNTDOWN | ACTIVE | FINISHED

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "finished_at")
    private LocalDateTime finishedAt;

    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<DuelParticipant> participants = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (status == null) status = "WAITING";
    }
}

