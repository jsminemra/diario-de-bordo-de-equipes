package com.diariodebordo.diariobordo.repository;

import com.diariodebordo.diariobordo.model.Sprint;
import com.diariodebordo.diariobordo.model.SprintReport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SprintReportRepository extends JpaRepository<SprintReport, Long> {

    Optional<SprintReport> findBySprint(Sprint sprint);
}
