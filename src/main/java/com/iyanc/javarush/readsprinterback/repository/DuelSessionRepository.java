package com.iyanc.javarush.readsprinterback.repository;

import com.iyanc.javarush.readsprinterback.entity.DuelSession;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DuelSessionRepository extends JpaRepository<DuelSession, Long> {
}

