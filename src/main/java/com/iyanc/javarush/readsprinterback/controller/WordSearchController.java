package com.iyanc.javarush.readsprinterback.controller;

import com.iyanc.javarush.readsprinterback.repository.WsWordBankRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/word-search")
@RequiredArgsConstructor
@Tag(name = "Word Search", description = "Word-search exercise data")
@SecurityRequirement(name = "bearerAuth")
public class WordSearchController {

    private final WsWordBankRepository wsWordBankRepository;

    /**
     * Returns the full word bank for the solo word-search exercise.
     * The client uses this list to generate the letter grid locally.
     */
    @GetMapping("/words")
    @Operation(summary = "Get all words available for the word-search exercise")
    public List<String> getWords() {
        return wsWordBankRepository.findAllWords();
    }
}

