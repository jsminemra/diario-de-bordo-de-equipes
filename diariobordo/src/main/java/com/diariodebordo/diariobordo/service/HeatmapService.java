package com.diariodebordo.diariobordo.service;

import com.diariodebordo.diariobordo.dto.HeatmapDataDTO;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class HeatmapService {

    private final DailyEntryRepository dailyEntryRepository;
    private final SprintRepository sprintRepository;
    private final TeamRepository teamRepository;

    public HeatmapService(DailyEntryRepository dailyEntryRepository,
                          SprintRepository sprintRepository,
                          TeamRepository teamRepository) {
        this.dailyEntryRepository = dailyEntryRepository;
        this.sprintRepository = sprintRepository;
        this.teamRepository = teamRepository;
    }

    public List<HeatmapDataDTO> getHeatmapData(User user, int days) {
        List<HeatmapDataDTO> heatmapData = new ArrayList<>();
        LocalDate today = LocalDate.now();
        LocalDate startDate = today.minusDays(days);

        Team userTeam = getUserTeam(user);
        
        if (userTeam == null) {
            return generateEmptyHeatmap(startDate, today);
        }

        List<Sprint> sprints = sprintRepository.findByTeam(userTeam);
        
        if (sprints.isEmpty()) {
            return generateEmptyHeatmap(startDate, today);
        }

        List<DailyEntry> allEntries = new ArrayList<>();
        for (Sprint sprint : sprints) {
            allEntries.addAll(dailyEntryRepository.findByUserAndSprint(user, sprint));
        }

        Map<LocalDate, String> entrySummary = new HashMap<>();
        for (DailyEntry entry : allEntries) {
            LocalDate date = entry.getEntryDate();
            if (!date.isBefore(startDate) && !date.isAfter(today)) {
                String summary = entry.getWhatWasDone();
                if (summary != null && summary.length() > 30) {
                    summary = summary.substring(0, 30) + "...";
                }
                entrySummary.put(date, summary != null ? summary : "Registro realizado");
            }
        }

        for (LocalDate date = startDate; !date.isAfter(today); date = date.plusDays(1)) {
            HeatmapDataDTO data = new HeatmapDataDTO();
            data.setDate(date);
            
            if (entrySummary.containsKey(date)) {
                data.setCount(1);
                data.setSummary(entrySummary.get(date));
                data.setType("entry");
            } else {
                data.setCount(0);
                data.setSummary("Nenhum registro");
                data.setType("none");
            }
            
            heatmapData.add(data);
        }

        return heatmapData;
    }

    private List<HeatmapDataDTO> generateEmptyHeatmap(LocalDate startDate, LocalDate today) {
        List<HeatmapDataDTO> emptyData = new ArrayList<>();
        for (LocalDate date = startDate; !date.isAfter(today); date = date.plusDays(1)) {
            HeatmapDataDTO data = new HeatmapDataDTO();
            data.setDate(date);
            data.setCount(0);
            data.setSummary("Nenhum registro");
            data.setType("none");
            emptyData.add(data);
        }
        return emptyData;
    }

    private Team getUserTeam(User user) {
        return teamRepository.findAll().stream()
                .filter(t -> t.getMembers().stream()
                        .anyMatch(m -> m.getId().equals(user.getId())))
                .findFirst()
                .orElse(null);
    }
}