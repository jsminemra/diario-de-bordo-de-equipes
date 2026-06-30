package com.diariodebordo.diariobordo.controller;

import com.diariodebordo.diariobordo.dto.HeatmapDataDTO;
import com.diariodebordo.diariobordo.dto.TeamProgressDTO;
import com.diariodebordo.diariobordo.model.DailyEntry;
import com.diariodebordo.diariobordo.model.Sprint;
import com.diariodebordo.diariobordo.model.Team;
import com.diariodebordo.diariobordo.model.User;
import com.diariodebordo.diariobordo.repository.DailyEntryRepository;
import com.diariodebordo.diariobordo.repository.SprintRepository;
import com.diariodebordo.diariobordo.repository.TeamRepository;
import com.diariodebordo.diariobordo.repository.UserRepository;
import com.diariodebordo.diariobordo.service.HeatmapService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/professor")
public class ProfessorController {

    private final TeamRepository teamRepository;
    private final UserRepository userRepository;
    private final SprintRepository sprintRepository;
    private final DailyEntryRepository dailyEntryRepository;
    private final HeatmapService heatmapService;

    private static final int[][] RELEASE_SPRINTS = {
        {1, 2},   // Release I: Sprints 1-2
        {3, 4},   // Release II: Sprints 3-4
        {5, 6},   // Release III: Sprints 5-6
        {7, 8}    // Release IV: Sprints 7-8
    };
    
    private static final String[] RELEASE_NAMES = {
        "Release I - Plano de Trabalho",
        "Release II - Base do Sistema",
        "Release III - Gestão e Relatório",
        "Release IV - Empacotamento"
    };

    public ProfessorController(TeamRepository teamRepository, 
                               UserRepository userRepository,
                               SprintRepository sprintRepository,
                               DailyEntryRepository dailyEntryRepository,
                               HeatmapService heatmapService) {
        this.teamRepository = teamRepository;
        this.userRepository = userRepository;
        this.sprintRepository = sprintRepository;
        this.dailyEntryRepository = dailyEntryRepository;
        this.heatmapService = heatmapService;
    }

    @GetMapping("/panel")
    public String panel(Model model) {
        List<Team> allTeams = teamRepository.findAll();
        List<TeamProgressDTO> teamProgressList = new ArrayList<>();
        
        for (Team team : allTeams) {
            teamProgressList.add(calculateTeamProgress(team));
        }
        
        double avgProgress = 0.0;
        if (!teamProgressList.isEmpty()) {
            double sum = 0.0;
            for (TeamProgressDTO p : teamProgressList) {
                sum += p.getProgressPercentage();
            }
            avgProgress = sum / teamProgressList.size();
        }
        
        model.addAttribute("teams", allTeams);
        model.addAttribute("teamProgressList", teamProgressList);
        model.addAttribute("totalTeams", allTeams.size());
        model.addAttribute("totalUsers", userRepository.findAll().size());
        model.addAttribute("avgProgress", String.format("%.0f", avgProgress));
        
        return "professor/panel";
    }
    
    @GetMapping("/team/{teamId}")
    public String viewTeam(@PathVariable Long teamId, Model model) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new RuntimeException("Equipe não encontrada"));
        
        Sprint sprint = sprintRepository.findFirstByOrderByIdDesc().orElse(null);
        
        List<DailyEntry> recentEntries = new ArrayList<>();
        if (sprint != null) {
            recentEntries = dailyEntryRepository.findBySprint(sprint);
        }
        
        TeamProgressDTO progress = calculateTeamProgress(team);
        
        model.addAttribute("team", team);
        model.addAttribute("progress", progress);
        model.addAttribute("recentEntries", recentEntries.stream().limit(10).toList());
        model.addAttribute("sprintAtiva", sprint);
        
        return "professor/team-detail";
    }

    @GetMapping("/team/{teamId}/heatmap/{userId}")
    public String viewMemberHeatmap(@PathVariable Long teamId,
                                    @PathVariable Long userId,
                                    Model model) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new RuntimeException("Equipe não encontrada"));
        
        User member = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
        
        boolean isMember = team.getMembers().stream()
                .anyMatch(m -> m.getId().equals(member.getId()));
        
        if (!isMember) {
            throw new RuntimeException("Usuário não pertence a esta equipe");
        }
        
        List<HeatmapDataDTO> heatmapData = heatmapService.getHeatmapData(member, 90);
        
        List<List<HeatmapDataDTO>> weeks = new ArrayList<>();
        List<HeatmapDataDTO> currentWeek = new ArrayList<>();
        for (int i = 0; i < heatmapData.size(); i++) {
            currentWeek.add(heatmapData.get(i));
            if ((i + 1) % 7 == 0 || i == heatmapData.size() - 1) {
                weeks.add(currentWeek);
                currentWeek = new ArrayList<>();
            }
        }
        
        long diasComRegistro = heatmapData.stream().filter(d -> d.getCount() > 0).count();
        double percentual = (double) diasComRegistro / heatmapData.size() * 100;
        long totalRegistros = heatmapData.stream().mapToLong(HeatmapDataDTO::getCount).sum();
        
        model.addAttribute("team", team);
        model.addAttribute("member", member);
        model.addAttribute("heatmapData", heatmapData);
        model.addAttribute("weeks", weeks);
        model.addAttribute("days", 90);
        model.addAttribute("diasComRegistro", diasComRegistro);
        model.addAttribute("percentual", String.format("%.0f", percentual));
        model.addAttribute("totalRegistros", totalRegistros);
        
        return "professor/member-heatmap";
    }
    
    private TeamProgressDTO calculateTeamProgress(Team team) {
        TeamProgressDTO progress = new TeamProgressDTO();
        progress.setTeamId(team.getId());
        progress.setTeamName(team.getName());
        progress.setLeaderName(team.getLeader().getName());
        progress.setTotalMembers(team.getMembers().size());
        
        List<Sprint> sprints = sprintRepository.findByTeam(team);
        progress.setTotalSprints(sprints.size());

        long completed = sprints.stream()
                .filter(s -> s.getStatus() == Sprint.Status.ENCERRADA)
                .count();
        progress.setCompletedSprints((int) completed);
        
        Sprint activeSprint = sprints.stream()
                .filter(s -> s.getStatus() == Sprint.Status.ATIVA)
                .findFirst()
                .orElse(null);
        
        if (activeSprint != null) {
            progress.setCurrentSprintNumber(getSprintNumber(activeSprint.getName()));
            progress.setCurrentSprintName(activeSprint.getName());
            progress.setCurrentSprintStartDate(activeSprint.getStartDate());
            progress.setCurrentSprintEndDate(activeSprint.getEndDate());
        }
        
        progress.setProgressPercentage(Math.min((double) completed / 8 * 100, 100));
        
        List<TeamProgressDTO.ReleaseProgressDTO> releases = new ArrayList<>();
        
        for (int r = 0; r < RELEASE_SPRINTS.length; r++) {
            TeamProgressDTO.ReleaseProgressDTO release = new TeamProgressDTO.ReleaseProgressDTO();
            release.setReleaseNumber(r + 1);
            release.setReleaseName(RELEASE_NAMES[r]);
            
            int sprintStart = RELEASE_SPRINTS[r][0];
            int sprintEnd = RELEASE_SPRINTS[r][1];
            int totalSprintsInRelease = sprintEnd - sprintStart + 1;
            release.setTotalSprints(totalSprintsInRelease);
            
            List<TeamProgressDTO.SprintProgressDTO> sprintProgressList = new ArrayList<>();
            int completedInRelease = 0;
            
            for (int s = sprintStart; s <= sprintEnd; s++) {
                TeamProgressDTO.SprintProgressDTO sprintProgress = new TeamProgressDTO.SprintProgressDTO();
                sprintProgress.setSprintNumber(s);
                sprintProgress.setSprintName("Sprint " + s);
                
                boolean sprintCompleted = false;
                
                for (Sprint sprint : sprints) {
                    if (getSprintNumber(sprint.getName()) == s) {
                        if (sprint.getStatus() == Sprint.Status.ENCERRADA) {
                            sprintCompleted = true;
                        }
                        sprintProgress.setStartDate(sprint.getStartDate());
                        sprintProgress.setEndDate(sprint.getEndDate());
                        break;
                    }
                }
                
                sprintProgress.setCompleted(sprintCompleted);
                sprintProgressList.add(sprintProgress);
                
                if (sprintCompleted) {
                    completedInRelease++;
                }
            }
            
            release.setSprints(sprintProgressList);
            release.setCompletedSprints(completedInRelease);
            
            double releaseProgress = 0.0;
            if (totalSprintsInRelease > 0) {
                releaseProgress = (double) completedInRelease / totalSprintsInRelease * 100;
            }
            release.setProgressPercentage(releaseProgress);
            
            releases.add(release);
        }
        
        progress.setReleases(releases);
        return progress;
    }
    
    private int getSprintNumber(String sprintName) {
        if (sprintName == null) {
            return 0;
        }
        try {
            String[] parts = sprintName.split(" ");
            for (String part : parts) {
                try {
                    return Integer.parseInt(part);
                } catch (NumberFormatException e) {
                    
                }
            }
        } catch (Exception e) {
            
        }
        return 0;
    }
}