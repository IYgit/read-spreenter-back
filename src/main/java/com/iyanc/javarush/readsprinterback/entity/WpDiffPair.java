package com.iyanc.javarush.readsprinterback.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "wp_diff_pairs")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class WpDiffPair {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String word1;

    @Column(nullable = false, length = 50)
    private String word2;
}

