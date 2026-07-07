package com.diariodebordo.diariobordo.repository;

import com.diariodebordo.diariobordo.model.Team;
import com.diariodebordo.diariobordo.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TeamRepository extends JpaRepository<Team, Long> {

    List<Team> findByLeader(User leader);
}
