package com.iyanc.javarush.readsprinterback.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Position of a hidden word inside the word-search grid. */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class WsWordPosition {
    private String word;
    private int row;
    private int startCol;
}

