package com.iyanc.javarush.readsprinterback.repository;

import com.iyanc.javarush.readsprinterback.entity.WsWordBank;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface WsWordBankRepository extends JpaRepository<WsWordBank, Long> {

    /** Returns all words as plain strings (no entity overhead). */
    @Query("SELECT w.word FROM WsWordBank w")
    List<String> findAllWords();
}

