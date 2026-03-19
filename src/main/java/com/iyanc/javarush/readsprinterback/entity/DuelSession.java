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

    @Column(name = "status", nullable = false)
    private String status; // WAITING | COUNTDOWN | ACTIVE | FINISHED

    /**
     * Exercise-specific parameters (1:1, shared PK).
     * Always non-null after session creation.
     * EAGER so DuelDbService can read params without a separate query.
     */
    @OneToOne(mappedBy = "session", cascade = CascadeType.ALL, fetch = FetchType.EAGER, optional = false)
    private DuelSessionParams params;

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

