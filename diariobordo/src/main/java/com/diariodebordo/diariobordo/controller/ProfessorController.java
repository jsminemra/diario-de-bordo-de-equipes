package com.diariodebordo.diariobordo.controller;

import com.diariodebordo.diariobordo.dto.HeatmapDataDTO;
import com.diariodebordo.diariobordo.dto.MemberHeatmapDTO;
import com.diariodebordo.diariobordo.dto.TeamProgressDTO;
import com.diariodebordo.diariobordo.model.DailyEntry;
import com.diariodebordo.diariobordo.model.Sprint;
import com.diariodebordo.diariobordo.model.Team;
import com.diariodebordo.diariobordo.model.User;
import com.diariodebordo.diariobordo.repository.DailyEntryRepository;
import com.diariodebordo.diariobordo.repository.SprintRepository;
import com.diariodebordo.diariobordo.repository.TeamRepository;
import com.diariodebordo.diariobordo.repository.UserRepository;
import com.diariodebordo.diariobordo.service.GitHubService;
import com.diariodebordo.diariobordo.service.HeatmapService;
import com.diariodebordo.diariobordo.service.SprintReportService;
import com.diariodebordo.diariobordo.service.TeamService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/professor")
public class ProfessorController {

    private final TeamRepository teamRepository;
    private final UserRepository userRepository;
    private final SprintRepository sprintRepository;
    private final DailyEntryRepository dailyEntryRepository;
    private final HeatmapService heatmapService;
    private final SprintReportService sprintReportService;
    private final TeamService teamService;
    private final GitHubService gitHubService;

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
                               HeatmapService heatmapService,
                               SprintReportService sprintReportService,
                               TeamService teamService,
                               GitHubService gitHubService) {
        this.teamRepository = teamRepository;
        this.userRepository = userRepository;
        this.sprintRepository = sprintRepository;
        this.dailyEntryRepository = dailyEntryRepository;
        this.heatmapService = heatmapService;
        this.sprintReportService = sprintReportService;
        this.teamService = teamService;
        this.gitHubService = gitHubService;
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
        
        Map<Long, Long> registradosHojeMap = new HashMap<>();
        for (Team team : allTeams) {
            Sprint activeSprint = sprintRepository.findByTeamAndStatus(team, Sprint.Status.ATIVA).orElse(null);
            long count = 0;
            if (activeSprint != null) {
                count = dailyEntryRepository.findBySprintAndEntryDate(activeSprint, LocalDate.now()).size();
            }
            registradosHojeMap.put(team.getId(), count);
        }

        model.addAttribute("teams", allTeams);
        model.addAttribute("teamProgressList", teamProgressList);
        model.addAttribute("totalTeams", allTeams.size());
        model.addAttribute("totalUsers", userRepository.findAll().size());
        model.addAttribute("avgProgress", String.format("%.0f", avgProgress));
        model.addAttribute("registradosHojeMap", registradosHojeMap);

        return "professor/panel";
    }

    @PostMapping("/team/{teamId}/delete")
    public String deleteTeam(@PathVariable Long teamId, RedirectAttributes redirectAttributes) {
        try {
            teamService.deleteTeam(teamId);
            redirectAttributes.addFlashAttribute("success", "Equipe excluída com sucesso!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Não foi possível excluir a equipe: " + e.getMessage());
        }
        return "redirect:/professor/panel";
    }
    
    @GetMapping("/team/{teamId}")
    public String viewTeam(@PathVariable Long teamId, Model model) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new RuntimeException("Equipe não encontrada"));

        List<Sprint> allSprints = sprintRepository.findByTeam(team);
        allSprints.sort(Comparator.comparing(Sprint::getStartDate).reversed());

        Sprint sprintAtiva = allSprints.stream()
                .filter(s -> s.getStatus() == Sprint.Status.ATIVA)
                .findFirst()
                .orElse(null);

        List<Long> sprintEntryCounts = new ArrayList<>();
        List<Integer> sprintAdesaoPcts = new ArrayList<>();
        long totalRegistros = 0;

        for (Sprint sprint : allSprints) {
            List<DailyEntry> entries = dailyEntryRepository.findBySprint(sprint);
            long count = entries.size();
            sprintEntryCounts.add(count);
            totalRegistros += count;

            long sprintDays = ChronoUnit.DAYS.between(sprint.getStartDate(), sprint.getEndDate()) + 1;
            int expectedEntries = team.getMembers().size() * (int) Math.max(1, sprintDays);
            int pct = expectedEntries > 0
                    ? (int) Math.min(100, Math.round(count * 100.0 / expectedEntries))
                    : 0;
            sprintAdesaoPcts.add(pct);
        }

        long encerradas = allSprints.stream()
                .filter(s -> s.getStatus() == Sprint.Status.ENCERRADA).count();
        int mediaAdesao = sprintAdesaoPcts.isEmpty() ? 0
                : (int) sprintAdesaoPcts.stream().mapToInt(Integer::intValue).average().orElse(0);

        long registradosHoje = 0;
        if (sprintAtiva != null) {
            registradosHoje = dailyEntryRepository
                    .findBySprintAndEntryDate(sprintAtiva, LocalDate.now()).size();
        }

        TeamProgressDTO progress = calculateTeamProgress(team);

        model.addAttribute("team", team);
        model.addAttribute("allSprints", allSprints);
        model.addAttribute("sprintAtiva", sprintAtiva);
        model.addAttribute("sprintEntryCounts", sprintEntryCounts);
        model.addAttribute("sprintAdesaoPcts", sprintAdesaoPcts);
        model.addAttribute("encerradas", encerradas);
        model.addAttribute("totalRegistros", totalRegistros);
        model.addAttribute("mediaAdesao", mediaAdesao);
        model.addAttribute("registradosHoje", registradosHoje);
        model.addAttribute("progress", progress);

        return "professor/team-detail";
    }

    @GetMapping("/team/{teamId}/sprint/{sprintId}/report")
    public String viewSprintReport(@PathVariable Long teamId,
                                   @PathVariable Long sprintId,
                                   Model model) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new RuntimeException("Equipe não encontrada"));

        Sprint sprint = sprintRepository.findById(sprintId)
                .orElseThrow(() -> new RuntimeException("Sprint não encontrada"));

        if (!sprint.getTeam().getId().equals(teamId)) {
            throw new RuntimeException("Sprint não encontrada");
        }

        List<DailyEntry> entries = dailyEntryRepository.findBySprint(sprint);

        Map<Long, List<DailyEntry>> entriesByMember = entries.stream()
                .sorted(Comparator.comparing(DailyEntry::getEntryDate))
                .collect(Collectors.groupingBy(e -> e.getUser().getId()));

        long totalDays = ChronoUnit.DAYS.between(sprint.getStartDate(), sprint.getEndDate()) + 1;
        long diasUteis = diasUteisNoPeriodo(sprint.getStartDate(), sprint.getEndDate());

        model.addAttribute("team", team);
        model.addAttribute("sprint", sprint);
        model.addAttribute("entries", entries);
        model.addAttribute("entriesByMember", entriesByMember);
        model.addAttribute("totalDays", totalDays);
        model.addAttribute("diasUteis", diasUteis);
        model.addAttribute("backUrl", "/professor/team/" + teamId);
        model.addAttribute("sprintReport",
                sprintReportService.findBySprint(sprint).orElse(null));

        return "report";
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
        int percentual = heatmapData.isEmpty() ? 0
                : (int) Math.round((double) diasComRegistro / heatmapData.size() * 100);
        long totalRegistros = heatmapData.stream().mapToLong(HeatmapDataDTO::getCount).sum();

        model.addAttribute("team", team);
        model.addAttribute("member", member);
        model.addAttribute("heatmapData", heatmapData);
        model.addAttribute("weeks", weeks);
        model.addAttribute("days", 90);
        model.addAttribute("diasComRegistro", diasComRegistro);
        model.addAttribute("percentual", percentual);
        model.addAttribute("totalRegistros", totalRegistros);
        
        return "professor/member-heatmap";
    }

    @GetMapping("/team/{teamId}/github/heatmap/{userId}")
    public String viewMemberGithubHeatmap(@PathVariable Long teamId,
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

        GitHubService.CommitFetchResult result = gitHubService.fetchCommits(member, team);
        List<HeatmapDataDTO> heatmapData = gitHubService.buildHeatmap(result.getCommits(), 90);

        List<List<HeatmapDataDTO>> weeks = new ArrayList<>();
        List<HeatmapDataDTO> currentWeek = new ArrayList<>();
        for (int i = 0; i < heatmapData.size(); i++) {
            currentWeek.add(heatmapData.get(i));
            if ((i + 1) % 7 == 0 || i == heatmapData.size() - 1) {
                weeks.add(currentWeek);
                currentWeek = new ArrayList<>();
            }
        }

        model.addAttribute("team", team);
        model.addAttribute("member", member);
        model.addAttribute("heatmapData", heatmapData);
        model.addAttribute("weeks", weeks);
        model.addAttribute("days", 90);
        model.addAttribute("totalRegistros", result.getCommits().size());
        model.addAttribute("semDadosMotivo",
                member.getGithubUsername() == null || member.getGithubUsername().isBlank()
                        ? "GitHub não vinculado por este usuário"
                        : (team.getGithubRepo() == null || team.getGithubRepo().isBlank()
                                ? "Repositório da equipe não configurado" : null));

        return "professor/member-github-heatmap";
    }

    @GetMapping("/team/{teamId}/heatmap")
    public String viewTeamHeatmap(@PathVariable Long teamId, Model model) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new RuntimeException("Equipe não encontrada"));

        List<MemberHeatmapDTO> memberHeatmaps = heatmapService.getTeamHeatmapData(team, 90);

        model.addAttribute("team", team);
        model.addAttribute("memberHeatmaps", memberHeatmaps);
        model.addAttribute("days", 90);

        return "professor/heatmap-geral";
    }

    @GetMapping("/team/{teamId}/github/heatmap")
    public String viewTeamGithubHeatmap(@PathVariable Long teamId, Model model) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new RuntimeException("Equipe não encontrada"));

        List<MemberHeatmapDTO> memberHeatmaps = gitHubService.getTeamGithubHeatmapData(team, 90);

        model.addAttribute("team", team);
        model.addAttribute("memberHeatmaps", memberHeatmaps);
        model.addAttribute("days", 90);

        return "professor/github-heatmap-geral";
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

    private long diasUteisNoPeriodo(LocalDate inicio, LocalDate fim) {
        return inicio.datesUntil(fim.plusDays(1))
                .filter(d -> d.getDayOfWeek() != DayOfWeek.SATURDAY && d.getDayOfWeek() != DayOfWeek.SUNDAY)
                .count();
    }
}