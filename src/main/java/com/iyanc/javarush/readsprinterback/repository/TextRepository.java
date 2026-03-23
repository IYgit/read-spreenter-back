package com.iyanc.javarush.readsprinterback.repository;

import com.iyanc.javarush.readsprinterback.entity.Text;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface TextRepository extends JpaRepository<Text, Long> {
    List<Text> findAllByOrderByIdAsc();
    List<Text> findByDifficulty(Text.Difficulty difficulty);

    /** Returns all text IDs — used for random selection in Java to avoid JPQL RAND() dialect issues */
    @Query("SELECT t.id FROM Text t")
    List<Long> findAllIds();

    @Query("SELECT t FROM Text t ORDER BY RAND()")
    List<Text> findRandom(Pageable pageable);
}



