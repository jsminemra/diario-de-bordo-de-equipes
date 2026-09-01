package com.diariodebordo.diariobordo.repository;

import com.diariodebordo.diariobordo.model.Epic;
import com.diariodebordo.diariobordo.model.Team;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EpicRepository extends JpaRepository<Epic, Long> {

    List<Epic> findByTeamOrderByCreatedAtDesc(Team team);
}
