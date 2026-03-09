package com.iyanc.javarush.readsprinterback.repository;

import com.iyanc.javarush.readsprinterback.entity.ExerciseResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ExerciseResultRepository extends JpaRepository<ExerciseResult, Long> {

    List<ExerciseResult> findByUserIdOrderByCompletedAtDesc(Long userId);

    List<ExerciseResult> findByUserIdAndExerciseTypeOrderByCompletedAtDesc(Long userId, String exerciseType);

    @Query("""
            SELECT r.exerciseType, COUNT(r), AVG(r.score), AVG(r.wpm), AVG(r.durationSec)
            FROM ExerciseResult r
            WHERE r.user.id = :userId
            GROUP BY r.exerciseType
            """)
    List<Object[]> findSummaryByUserId(@Param("userId") Long userId);

    long countByUserId(Long userId);
}

