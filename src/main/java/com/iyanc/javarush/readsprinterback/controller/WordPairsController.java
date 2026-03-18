package com.iyanc.javarush.readsprinterback.controller;

import com.iyanc.javarush.readsprinterback.dto.WordPairDto;
import com.iyanc.javarush.readsprinterback.repository.WpDiffPairRepository;
import com.iyanc.javarush.readsprinterback.repository.WpSameWordRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

@RestController
@RequestMapping("/api/word-pairs")
@RequiredArgsConstructor
@Tag(name = "Word Pairs", description = "Word-pairs exercise data")
@SecurityRequirement(name = "bearerAuth")
public class WordPairsController {

    private static final Random RANDOM = new Random();

    private final WpDiffPairRepository wpDiffPairRepository;
    private final WpSameWordRepository wpSameWordRepository;

    /**
     * Returns a shuffled grid of word-pairs for the solo exercise.
     * ~50% are different pairs (isDifferent=true), rest are identical.
     *
     * @param rows grid rows (default 4)
     * @param cols grid cols (default 4)
     */
    @GetMapping
    @Operation(summary = "Get a shuffled word-pairs grid for solo exercise")
    public List<WordPairDto> getGrid(
            @RequestParam(defaultValue = "4") int rows,
            @RequestParam(defaultValue = "4") int cols) {

        int totalCells = rows * cols;
        int diffCount = totalCells / 2;
        int sameCount = totalCells - diffCount;

        List<WordPairDto> result = new ArrayList<>();

        wpDiffPairRepository.findRandom(diffCount)
                .forEach(p -> result.add(new WordPairDto(p.getWord1(), p.getWord2(), true)));

        wpSameWordRepository.findRandom(sameCount)
                .forEach(p -> result.add(new WordPairDto(p.getWord(), p.getWord(), false)));

        Collections.shuffle(result, RANDOM);
        return result;
    }
}

