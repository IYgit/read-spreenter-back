package com.iyanc.javarush.readsprinterback.repository;

import com.iyanc.javarush.readsprinterback.entity.WpDiffPair;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface WpDiffPairRepository extends JpaRepository<WpDiffPair, Long> {

    /** Returns `count` random different pairs using DB-level random ordering. */
    @Query(value = "SELECT * FROM wp_diff_pairs ORDER BY RANDOM() LIMIT :count", nativeQuery = true)
    List<WpDiffPair> findRandom(@Param("count") int count);
}

