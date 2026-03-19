package com.iyanc.javarush.readsprinterback.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "ws_word_bank")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class WsWordBank {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String word;
}

