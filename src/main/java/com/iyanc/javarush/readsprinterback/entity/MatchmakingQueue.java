package com.iyanc.javarush.readsprinterback.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "matchmaking_queue")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MatchmakingQueue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "exercise_type", nullable = false)
    private String exerciseType;

    @Column(name = "grid_size", nullable = false)
    private int gridSize;

    @Column(name = "font_size", nullable = false)
    private int fontSize;

    @Column(name = "joined_at", nullable = false)
    private LocalDateTime joinedAt;

    @PrePersist
    public void prePersist() {
        if (joinedAt == null) joinedAt = LocalDateTime.now();
    }
}

