package com.iyanc.javarush.readsprinterback.service;

import com.iyanc.javarush.readsprinterback.dto.response.TextResponse;

import java.util.List;

public interface TextService {
    List<TextResponse> getAllTexts();
    TextResponse getTextById(Long id);
}

