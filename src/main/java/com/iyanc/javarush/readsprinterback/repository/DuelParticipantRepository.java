package com.iyanc.javarush.readsprinterback.repository;

import com.iyanc.javarush.readsprinterback.entity.DuelParticipant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DuelParticipantRepository extends JpaRepository<DuelParticipant, Long> {

    List<DuelParticipant> findAllBySessionId(Long sessionId);

    Optional<DuelParticipant> findBySessionIdAndUserId(Long sessionId, Long userId);

    List<DuelParticipant> findAllByUserId(Long userId);
}

