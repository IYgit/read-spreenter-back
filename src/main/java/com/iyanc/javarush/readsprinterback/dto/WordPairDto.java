package com.iyanc.javarush.readsprinterback.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** A single word-pair cell for the "word-pairs" duel exercise. */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class WordPairDto {
    private String w1;
    private String w2;
    private boolean diff;   // true = words are different (player must click this cell)
}

