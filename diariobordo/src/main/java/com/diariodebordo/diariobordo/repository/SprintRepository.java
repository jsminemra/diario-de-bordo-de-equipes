package com.diariodebordo.diariobordo.repository;

import com.diariodebordo.diariobordo.model.Sprint;
import com.diariodebordo.diariobordo.model.Team;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SprintRepository extends JpaRepository<Sprint, Long> {

    List<Sprint> findByTeam(Team team);

    Optional<Sprint> findByTeamAndStatus(Team team, Sprint.Status status);

    Optional<Sprint> findFirstByOrderByIdDesc();

    boolean existsByTeamAndStatus(Team team, Sprint.Status status);
    
}
