package com.diariodebordo.diariobordo.service;

import com.diariodebordo.diariobordo.model.DailyEntry;
import com.diariodebordo.diariobordo.model.Sprint;
import com.diariodebordo.diariobordo.model.Team;
import com.diariodebordo.diariobordo.model.User;
import com.diariodebordo.diariobordo.repository.DailyEntryRepository;
import com.diariodebordo.diariobordo.repository.SprintRepository;
import com.diariodebordo.diariobordo.repository.TeamRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class HistoryService {

    private final DailyEntryRepository dailyEntryRepository;
    private final SprintRepository sprintRepository;
    private final TeamRepository teamRepository;

    public HistoryService(DailyEntryRepository dailyEntryRepository,
                          SprintRepository sprintRepository,
                          TeamRepository teamRepository) {
        this.dailyEntryRepository = dailyEntryRepository;
        this.sprintRepository = sprintRepository;
        this.teamRepository = teamRepository;
    }

    public List<DailyEntry> getUserHistory(User user, LocalDate startDate, LocalDate endDate) {
        Team userTeam = getUserTeam(user);
        
        if (userTeam == null) {
            return new ArrayList<>();
        }

        List<Sprint> sprints = sprintRepository.findByTeam(userTeam);

        LocalDate effectiveStartDate = (startDate != null) ? startDate : LocalDate.now().minusDays(30);
        LocalDate effectiveEndDate = (endDate != null) ? endDate : LocalDate.now();

        List<DailyEntry> allEntries = new ArrayList<>();
        for (Sprint sprint : sprints) {
            allEntries.addAll(dailyEntryRepository.findByUserAndSprint(user, sprint));
        }

        if (allEntries.isEmpty()) {
            allEntries = dailyEntryRepository.findAll().stream()
                    .filter(e -> e.getUser().getId().equals(user.getId()))
                    .toList();
        }

        return allEntries.stream()
                .filter(e -> !e.getEntryDate().isBefore(effectiveStartDate) && !e.getEntryDate().isAfter(effectiveEndDate))
                .sorted((e1, e2) -> e2.getEntryDate().compareTo(e1.getEntryDate()))
                .toList();
    }

    public List<DailyEntry> getTeamHistory(Team team, LocalDate startDate, LocalDate endDate) {
        List<Sprint> sprints = sprintRepository.findByTeam(team);

        LocalDate effectiveStartDate = (startDate != null) ? startDate : LocalDate.now().minusDays(30);
        LocalDate effectiveEndDate = (endDate != null) ? endDate : LocalDate.now();

        List<DailyEntry> allEntries = new ArrayList<>();
        for (Sprint sprint : sprints) {
            allEntries.addAll(dailyEntryRepository.findBySprint(sprint));
        }

        if (allEntries.isEmpty()) {
            List<Long> memberIds = team.getMembers().stream().map(User::getId).toList();
            allEntries = dailyEntryRepository.findAll().stream()
                    .filter(e -> memberIds.contains(e.getUser().getId()))
                    .toList();
        }

        return allEntries.stream()
                .filter(e -> !e.getEntryDate().isBefore(effectiveStartDate) && !e.getEntryDate().isAfter(effectiveEndDate))
                .sorted((e1, e2) -> e2.getEntryDate().compareTo(e1.getEntryDate()))
                .toList();
    }

    private Team getUserTeam(User user) {
        List<Team> teams = teamRepository.findAll().stream()
                .filter(t -> t.getMembers().stream().anyMatch(m -> m.getId().equals(user.getId())))
                .toList();
        
        if (teams.isEmpty()) {
            return null;
        }
        return teams.get(0);
    }
}