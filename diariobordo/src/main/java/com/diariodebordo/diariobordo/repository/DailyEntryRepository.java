package com.diariodebordo.diariobordo.repository;

import com.diariodebordo.diariobordo.model.DailyEntry;
import com.diariodebordo.diariobordo.model.Sprint;
import com.diariodebordo.diariobordo.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DailyEntryRepository extends JpaRepository<DailyEntry, Long> {

    List<DailyEntry> findByUserAndSprint(User user, Sprint sprint);

    List<DailyEntry> findBySprintAndEntryDate(Sprint sprint, LocalDate date);

    List<DailyEntry> findBySprint(Sprint sprint);

    Optional<DailyEntry> findByUserAndEntryDate(User user, LocalDate entryDate);

    List<DailyEntry> findBySprintAndEntryDateOrderByCreatedAtDesc(Sprint sprint, LocalDate entryDate);

        @Query("SELECT d FROM DailyEntry d WHERE d.user = :user AND d.sprint = :sprint " +
           "AND d.entryDate BETWEEN :startDate AND :endDate ORDER BY d.entryDate DESC")
    List<DailyEntry> findByUserAndSprintAndDateBetween(
            @Param("user") User user,
            @Param("sprint") Sprint sprint,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );
}
