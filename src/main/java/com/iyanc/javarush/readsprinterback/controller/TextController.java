package com.iyanc.javarush.readsprinterback.controller;

import com.iyanc.javarush.readsprinterback.dto.response.TextResponse;
import com.iyanc.javarush.readsprinterback.service.TextService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/texts")
@RequiredArgsConstructor
@Tag(name = "Texts", description = "Reading texts with comprehension questions")
public class TextController {

    private final TextService textService;

    @GetMapping
    @Operation(summary = "Get all reading texts")
    public List<TextResponse> getAllTexts() {
        return textService.getAllTexts();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a single text by ID")
    public TextResponse getTextById(@PathVariable Long id) {
        return textService.getTextById(id);
    }
}

