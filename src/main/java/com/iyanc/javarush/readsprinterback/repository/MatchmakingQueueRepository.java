package com.iyanc.javarush.readsprinterback.repository;

import com.iyanc.javarush.readsprinterback.entity.MatchmakingQueue;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface MatchmakingQueueRepository extends JpaRepository<MatchmakingQueue, Long> {

    Optional<MatchmakingQueue> findByUserId(Long userId);

    boolean existsByUserId(Long userId);

    void deleteByUserId(Long userId);

    /** Find oldest waiting opponent — uses Pageable to limit to 1 result (JPQL-portable) */
    @Query("""
        SELECT q FROM MatchmakingQueue q
        WHERE q.user.id <> :userId
          AND (q.exerciseType = :exerciseType OR q.exerciseType = 'any')
        ORDER BY q.joinedAt ASC
    """)
    List<MatchmakingQueue> findOpponents(Long userId, String exerciseType, Pageable pageable);

    /** Clean up stale entries older than given time */
    void deleteAllByJoinedAtBefore(LocalDateTime cutoff);
}

