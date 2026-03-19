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

    /** For "numbers" exercise: number of digits per round (3..8) */
    @Column(name = "digit_count", nullable = false)
    private int digitCount;

    /** For "numbers" exercise: display time in ms (50..2000) */
    @Column(name = "display_time", nullable = false)
    private int displayTime;

    /** For "word-pairs" exercise: grid rows (3..5) */
    @Column(name = "wp_rows", nullable = false)
    private int wpRows;

    /** For "word-pairs" exercise: grid cols (3..5) */
    @Column(name = "wp_cols", nullable = false)
    private int wpCols;

    /** For "word-pairs" exercise: time limit in seconds (30..120) */
    @Column(name = "wp_time_limit", nullable = false)
    private int wpTimeLimit;

    /** For "word-pairs" exercise: font size in px (12..18) */
    @Column(name = "wp_font_size", nullable = false)
    private int wpFontSize;

    /** For "rsvp" exercise: number of words per syntagm (1..5) */
    @Column(name = "rsvp_syntagm_width", nullable = false)
    private int rsvpSyntagmWidth = 3;

    /** For "rsvp" exercise: display time per syntagm in ms (100..1000) */
    @Column(name = "rsvp_display_time", nullable = false)
    private int rsvpDisplayTime = 300;

    @Column(name = "joined_at", nullable = false)
    private LocalDateTime joinedAt;

    @PrePersist
    public void prePersist() {
        if (joinedAt == null) joinedAt = LocalDateTime.now();
    }
}

