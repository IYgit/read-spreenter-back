package com.iyanc.javarush.readsprinterback.repository;

import com.iyanc.javarush.readsprinterback.entity.WpSameWord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface WpSameWordRepository extends JpaRepository<WpSameWord, Long> {

    /** Returns `count` random same words using DB-level random ordering. */
    @Query(value = "SELECT * FROM wp_same_words ORDER BY RANDOM() LIMIT :count", nativeQuery = true)
    List<WpSameWord> findRandom(@Param("count") int count);
}

