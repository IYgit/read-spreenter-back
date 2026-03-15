package com.iyanc.javarush.readsprinterback.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "duel_participants",
       uniqueConstraints = @UniqueConstraint(columnNames = {"session_id", "user_id"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DuelParticipant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private DuelSession session;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private int progress;   // numbers found so far

    @Column(nullable = false)
    private int errors;

    @Column(name = "duration_ms")
    private Long durationMs;

    private Integer score;

    @Column(nullable = false)
    private boolean finished;

    @Column(name = "finished_at")
    private LocalDateTime finishedAt;

    @Column(nullable = false)
    private boolean disconnected;
}

