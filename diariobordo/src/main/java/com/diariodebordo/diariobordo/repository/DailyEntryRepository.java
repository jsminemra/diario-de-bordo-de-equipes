package com.diariodebordo.diariobordo.repository;

import com.diariodebordo.diariobordo.model.DailyEntry;
import com.diariodebordo.diariobordo.model.Sprint;
import com.diariodebordo.diariobordo.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DailyEntryRepository extends JpaRepository<DailyEntry, Long> {

    List<DailyEntry> findByUserAndSprint(User user, Sprint sprint);

    List<DailyEntry> findBySprintAndEntryDate(Sprint sprint, LocalDate date);

    List<DailyEntry> findBySprint(Sprint sprint);

    // Métodos novos — US-05 e US-06
    Optional<DailyEntry> findByUserAndEntryDate(User user, LocalDate entryDate);

    List<DailyEntry> findBySprintAndEntryDateOrderByCreatedAtDesc(Sprint sprint, LocalDate entryDate);
}
