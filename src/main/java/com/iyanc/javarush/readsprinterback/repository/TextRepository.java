package com.iyanc.javarush.readsprinterback.repository;

import com.iyanc.javarush.readsprinterback.entity.Text;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface TextRepository extends JpaRepository<Text, Long> {
    List<Text> findAllByOrderByIdAsc();
    List<Text> findByDifficulty(Text.Difficulty difficulty);

    @Query("SELECT t FROM Text t ORDER BY RAND()")
    List<Text> findRandom(Pageable pageable);
}



