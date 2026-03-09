package com.iyanc.javarush.readsprinterback.repository;

import com.iyanc.javarush.readsprinterback.entity.Text;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TextRepository extends JpaRepository<Text, Long> {
    List<Text> findAllByOrderByIdAsc();
    List<Text> findByDifficulty(Text.Difficulty difficulty);
}

