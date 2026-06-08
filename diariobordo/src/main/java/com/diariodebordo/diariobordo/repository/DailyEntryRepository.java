package com.diariodebordo.diariobordo.repository;

import com.diariodebordo.diariobordo.model.DailyEntry;
import com.diariodebordo.diariobordo.model.Sprint;
import com.diariodebordo.diariobordo.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface DailyEntryRepository extends JpaRepository<DailyEntry, Long> {

    List<DailyEntry> findByUserAndSprint(User user, Sprint sprint);

    List<DailyEntry> findBySprintAndEntryDate(Sprint sprint, LocalDate date);

    List<DailyEntry> findBySprint(Sprint sprint);
}
