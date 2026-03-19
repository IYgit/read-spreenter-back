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

    // ── Schulte Table ─────────────────────────────────────────────────────────

    @Column(name = "grid_size")
    private Integer gridSize;

    @Column(name = "font_size")
    private Integer fontSize;

    // ── Numbers ───────────────────────────────────────────────────────────────

    @Column(name = "digit_count")
    private Integer digitCount;

    @Column(name = "display_time")
    private Integer displayTime;

    // ── Word Pairs ────────────────────────────────────────────────────────────

    @Column(name = "wp_rows")
    private Integer wpRows;

    @Column(name = "wp_cols")
    private Integer wpCols;

    @Column(name = "wp_time_limit")
    private Integer wpTimeLimit;

    @Column(name = "wp_font_size")
    private Integer wpFontSize;

    // ── RSVP ──────────────────────────────────────────────────────────────────

    @Column(name = "rsvp_syntagm_width")
    private Integer rsvpSyntagmWidth;

    @Column(name = "rsvp_display_time")
    private Integer rsvpDisplayTime;

    @Column(name = "joined_at", nullable = false)
    private LocalDateTime joinedAt;

    @PrePersist
    public void prePersist() {
        if (joinedAt == null) joinedAt = LocalDateTime.now();
    }
}
