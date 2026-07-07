package com.diariodebordo.diariobordo.service;

import com.diariodebordo.diariobordo.dto.SprintCreateDTO;
import com.diariodebordo.diariobordo.model.Sprint;
import com.diariodebordo.diariobordo.model.Team;
import com.diariodebordo.diariobordo.repository.SprintRepository;
import com.diariodebordo.diariobordo.repository.TeamRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SprintService {

    private final SprintRepository sprintRepository;
    private final TeamRepository teamRepository;

    public SprintService(SprintRepository sprintRepository, TeamRepository teamRepository) {
        this.sprintRepository = sprintRepository;
        this.teamRepository = teamRepository;
    }

    @Transactional
    public Sprint createSprint(SprintCreateDTO dto, Long teamId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new RuntimeException("Equipe não encontrada"));

        if (sprintRepository.existsByTeamAndStatus(team, Sprint.Status.ATIVA)) {
            throw new RuntimeException("Já existe uma sprint ativa para esta equipe. Encerre-a antes de criar uma nova.");
        }

        if (dto.getEndDate().isBefore(dto.getStartDate())) {
            throw new RuntimeException("A data de fim deve ser posterior à data de início.");
        }

        Sprint sprint = new Sprint();
        sprint.setName(dto.getName());
        sprint.setTeam(team);
        sprint.setStartDate(dto.getStartDate());
        sprint.setEndDate(dto.getEndDate());
        sprint.setStatus(Sprint.Status.ATIVA);

        return sprintRepository.save(sprint);
    }

    public List<Sprint> getSprintsByTeam(Long teamId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new RuntimeException("Equipe não encontrada"));
        return sprintRepository.findByTeam(team);
    }

    public Sprint getActiveSprint(Long teamId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new RuntimeException("Equipe não encontrada"));
        return sprintRepository.findByTeamAndStatus(team, Sprint.Status.ATIVA)
                .orElse(null);
    }
}