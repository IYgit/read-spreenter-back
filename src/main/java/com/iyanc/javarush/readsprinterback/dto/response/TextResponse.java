package com.iyanc.javarush.readsprinterback.dto.response;

import lombok.*;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TextResponse {
    private Long id;
    private String title;
    private String content;
    private String difficulty;
    private List<QuestionResponse> questions;
}

