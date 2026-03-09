package com.iyanc.javarush.readsprinterback.dto.response;

import lombok.*;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class QuestionResponse {
    private Long id;
    private String text;
    private List<String> options;
    private Integer correctIndex;
}

