package com.diariodebordo.diariobordo.controller;

import com.diariodebordo.diariobordo.dto.AddMemberDTO;
import com.diariodebordo.diariobordo.dto.DailyEntryDTO;
import com.diariodebordo.diariobordo.dto.SprintCreateDTO;
import com.diariodebordo.diariobordo.dto.TeamCreateDTO;
import com.diariodebordo.diariobordo.model.DailyEntry;
import com.diariodebordo.diariobordo.model.Sprint;
import com.diariodebordo.diariobordo.model.Team;
import com.diariodebordo.diariobordo.model.User;
import com.diariodebordo.diariobordo.repository.DailyEntryRepository;
import com.diariodebordo.diariobordo.repository.SprintRepository;
import com.diariodebordo.diariobordo.repository.UserRepository;
import com.diariodebordo.diariobordo.service.DailyEntryService;
import com.diariodebordo.diariobordo.service.HistoryService;
import com.diariodebordo.diariobordo.service.PdfExportService;
import com.diariodebordo.diariobordo.service.SprintReportService;
import com.diariodebordo.diariobordo.service.SprintService;
import com.diariodebordo.diariobordo.service.TeamService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/leader")
public class TeamController {

    private final TeamService teamService;
    private final UserRepository userRepository;
    private final DailyEntryService dailyEntryService;
    private final DailyEntryRepository dailyEntryRepository;
    private final SprintRepository sprintRepository;
    private final SprintService sprintService;
    private final HistoryService historyService;
    private final SprintReportService sprintReportService;
    private final PdfExportService pdfExportService;

    public TeamController(TeamService teamService,
                          UserRepository userRepository,
                          DailyEntryService dailyEntryService,
                          DailyEntryRepository dailyEntryRepository,
                          SprintRepository sprintRepository,
                          SprintService sprintService,
                          HistoryService historyService,
                          SprintReportService sprintReportService,
                          PdfExportService pdfExportService) {
        this.teamService = teamService;
        this.userRepository = userRepository;
        this.dailyEntryService = dailyEntryService;
        this.dailyEntryRepository = dailyEntryRepository;
        this.sprintRepository = sprintRepository;
        this.sprintService = sprintService;
        this.historyService = historyService;
        this.sprintReportService = sprintReportService;
        this.pdfExportService = pdfExportService;
    }
    
    @GetMapping("/team/create")
    public String showCreateForm(Model model) {
        model.addAttribute("teamCreateDTO", new TeamCreateDTO());
        return "team/create-team";
    }

    @PostMapping("/team/create")
    public String createTeam(@Valid @ModelAttribute("teamCreateDTO") TeamCreateDTO dto,
                             BindingResult result,
                             Authentication auth,
                             RedirectAttributes redirectAttributes) {
        
        if (result.hasErrors()) {
            return "team/create-team";
        }
        
        try {
            User leader = getAuthenticatedUser(auth);
            Team team = teamService.createTeam(dto, leader);
            redirectAttributes.addFlashAttribute("success", "Equipe '" + team.getName() + "' criada com sucesso!");
            return "redirect:/leader/team/" + team.getId();
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/leader/team/create";
        }
    }
    
    @GetMapping("/team/{teamId}")
    public String viewTeam(@PathVariable Long teamId, 
                          Authentication auth, 
                          Model model,
                          RedirectAttributes redirectAttributes) {
        try {
            User leader = getAuthenticatedUser(auth);
            Team team = teamService.getTeamWithMembers(teamId);
            
            if (!team.getLeader().getId().equals(leader.getId())) {
                redirectAttributes.addFlashAttribute("error", "Você não tem permissão para ver esta equipe");
                return "redirect:/leader/dashboard";
            }
            
            Sprint sprintAtiva = sprintService.getActiveSprint(teamId);
            
            DailyEntryDTO entryDTO = new DailyEntryDTO();
            boolean jaRegistrou = false;
            boolean temSprint = (sprintAtiva != null);
            
            if (temSprint) {
                Optional<DailyEntry> registroHoje = dailyEntryService.buscarRegistroDeHoje(leader);
                if (registroHoje.isPresent()) {
                    DailyEntry entry = registroHoje.get();
                    entryDTO.setWhatWasDone(entry.getWhatWasDone() != null ? entry.getWhatWasDone() : "");
                    entryDTO.setWhatWillBeDone(entry.getWhatWillBeDone() != null ? entry.getWhatWillBeDone() : "");
                    entryDTO.setImpediments(entry.getImpediments() != null ? entry.getImpediments() : "");
                    jaRegistrou = true;
                }
            }
            
            if (temSprint) {
                List<DailyEntry> todayEntries = dailyEntryRepository
                        .findBySprintAndEntryDate(sprintAtiva, LocalDate.now());
                Map<Long, DailyEntry> entryMap = todayEntries.stream()
                        .collect(Collectors.toMap(e -> e.getUser().getId(), e -> e, (a, b) -> a));
                long totalDays = ChronoUnit.DAYS.between(sprintAtiva.getStartDate(), sprintAtiva.getEndDate()) + 1;
                long elapsed = Math.max(0, ChronoUnit.DAYS.between(sprintAtiva.getStartDate(), LocalDate.now()));
                long diasRestantes = Math.max(0, ChronoUnit.DAYS.between(LocalDate.now(), sprintAtiva.getEndDate()));
                int progressoPct = totalDays > 0 ? (int) Math.min(100, Math.round(elapsed * 100.0 / totalDays)) : 0;
                long impedimentosHoje = todayEntries.stream()
                        .filter(e -> e.getImpediments() != null && !e.getImpediments().isBlank())
                        .count();
                int totalMembers = team.getMembers().size();
                int registrosHoje = todayEntries.size();
                int presencaPct = totalMembers > 0
                        ? (int) Math.round(registrosHoje * 100.0 / totalMembers)
                        : 0;
                int totalSprintEntries = dailyEntryRepository.findBySprint(sprintAtiva).size();
                model.addAttribute("entryMap", entryMap);
                model.addAttribute("totalDays", totalDays);
                model.addAttribute("elapsed", elapsed);
                model.addAttribute("diasRestantes", diasRestantes);
                model.addAttribute("progressoPct", progressoPct);
                model.addAttribute("registrosHoje", registrosHoje);
                model.addAttribute("impedimentosHoje", impedimentosHoje);
                model.addAttribute("presencaPct", presencaPct);
                model.addAttribute("totalSprintEntries", totalSprintEntries);
            }

            model.addAttribute("team", team);
            model.addAttribute("sprintAtiva", sprintAtiva);
            model.addAttribute("addMemberDTO", new AddMemberDTO());
            model.addAttribute("dailyEntryDTO", entryDTO);
            model.addAttribute("jaRegistrou", jaRegistrou);
            model.addAttribute("temSprint", temSprint);

            return "team/team-detail";
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/leader/dashboard";
        }
    }

    @PostMapping("/team/{teamId}/add-member")
    public String addMember(@PathVariable Long teamId,
                           @Valid @ModelAttribute("addMemberDTO") AddMemberDTO dto,
                           BindingResult result,
                           Authentication auth,
                           Model model,
                           RedirectAttributes redirectAttributes) {
        
        if (result.hasErrors()) {
            try {
                Team team = teamService.getTeamWithMembers(teamId);
                model.addAttribute("team", team);
                model.addAttribute("dailyEntryDTO", new DailyEntryDTO());
                model.addAttribute("jaRegistrou", false);
                model.addAttribute("temSprint", false);
                model.addAttribute("sprintAtiva", null);
                return "team/team-detail";
            } catch (Exception e) {
                redirectAttributes.addFlashAttribute("error", "Erro ao carregar página");
                return "redirect:/leader/team/" + teamId;
            }
        }
        
        try {
            User leader = getAuthenticatedUser(auth);
            Team team = teamService.addMember(teamId, dto, leader);
            redirectAttributes.addFlashAttribute("success", "Membro adicionado com sucesso!");
            return "redirect:/leader/team/" + teamId;
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/leader/team/" + teamId;
        }
    }

    @PostMapping("/team/{teamId}/regenerate-code")
    public String regenerateCode(@PathVariable Long teamId,
                                 Authentication auth,
                                 RedirectAttributes redirectAttributes) {
        try {
            User leader = getAuthenticatedUser(auth);
            teamService.regenerateCode(teamId, leader);
            redirectAttributes.addFlashAttribute("success", "Novo código de convite gerado!");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/leader/team/" + teamId;
    }

    @PostMapping("/team/{teamId}/github-repo")
    public String setGithubRepo(@PathVariable Long teamId,
                                @RequestParam String githubRepo,
                                Authentication auth,
                                RedirectAttributes redirectAttributes) {
        try {
            User leader = getAuthenticatedUser(auth);
            teamService.setGithubRepo(teamId, leader, githubRepo);
            redirectAttributes.addFlashAttribute("success", "Repositório GitHub da equipe atualizado!");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/leader/team/" + teamId;
    }

    @PostMapping("/team/{teamId}/entry")
    public String saveDailyEntry(@PathVariable Long teamId,
                                 @Valid @ModelAttribute("dailyEntryDTO") DailyEntryDTO dto,
                                 BindingResult result,
                                 Authentication auth,
                                 RedirectAttributes redirectAttributes) {
        
        if (result.hasErrors()) {
            redirectAttributes.addFlashAttribute("error", "Preencha todos os campos obrigatórios");
            return "redirect:/leader/team/" + teamId;
        }
        
        try {
            User leader = getAuthenticatedUser(auth);
            dailyEntryService.salvarOuEditar(dto, leader);
            redirectAttributes.addFlashAttribute("success", "Registro diário salvo com sucesso!");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        
        return "redirect:/leader/team/" + teamId;
    }

    @GetMapping("/team/{teamId}/sprint/create")
    public String showCreateSprintForm(@PathVariable Long teamId,
                                       Model model,
                                       Authentication auth,
                                       RedirectAttributes redirectAttributes) {
        try {
            User leader = getAuthenticatedUser(auth);
            Team team = teamService.getTeamWithMembers(teamId);
            
            if (!team.getLeader().getId().equals(leader.getId())) {
                redirectAttributes.addFlashAttribute("error", "Você não tem permissão para criar sprints");
                return "redirect:/leader/team/" + teamId;
            }
            
            model.addAttribute("team", team);
            model.addAttribute("sprintCreateDTO", new SprintCreateDTO());
            return "team/sprint-create";
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/leader/team/" + teamId;
        }
    }

    @PostMapping("/team/{teamId}/sprint/create")
    public String createSprint(@PathVariable Long teamId,
                               @Valid @ModelAttribute("sprintCreateDTO") SprintCreateDTO dto,
                               BindingResult result,
                               Authentication auth,
                               Model model,
                               RedirectAttributes redirectAttributes) {
        
        if (result.hasErrors()) {
            try {
                Team team = teamService.getTeamWithMembers(teamId);
                model.addAttribute("team", team);
                return "team/sprint-create";
            } catch (Exception e) {
                redirectAttributes.addFlashAttribute("error", "Erro ao carregar formulário");
                return "redirect:/leader/team/" + teamId;
            }
        }
        
        try {
            User leader = getAuthenticatedUser(auth);
            Team team = teamService.getTeamWithMembers(teamId);
            
            if (!team.getLeader().getId().equals(leader.getId())) {
                redirectAttributes.addFlashAttribute("error", "Você não tem permissão para criar sprints");
                return "redirect:/leader/team/" + teamId;
            }
            
            Sprint sprint = sprintService.createSprint(dto, teamId);
            redirectAttributes.addFlashAttribute("success", "Sprint '" + sprint.getName() + "' criada com sucesso!");
            return "redirect:/leader/team/" + teamId;
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/leader/team/" + teamId;
        }
    }

    @GetMapping("/team/{teamId}/history")
    public String showHistory(@PathVariable Long teamId,
                              @RequestParam(required = false) String startDate,
                              @RequestParam(required = false) String endDate,
                              Authentication auth,
                              Model model,
                              RedirectAttributes redirectAttributes) {
        try {
            User leader = getAuthenticatedUser(auth);
            Team team = teamService.getTeamWithMembers(teamId);

            if (!team.getLeader().getId().equals(leader.getId())) {
                redirectAttributes.addFlashAttribute("error", "Você não tem permissão para ver esta equipe");
                return "redirect:/leader/dashboard";
            }

            LocalDate start = null;
            LocalDate end = null;
            if (startDate != null && !startDate.isEmpty()) {
                start = LocalDate.parse(startDate);
            }
            if (endDate != null && !endDate.isEmpty()) {
                end = LocalDate.parse(endDate);
            }

            List<DailyEntry> history = historyService.getTeamHistory(team, start, end);

            Sprint sprint = sprintRepository.findByTeamAndStatus(team, Sprint.Status.ATIVA).orElse(null);

            List<Sprint> allSprints = sprintRepository.findByTeam(team);
            allSprints.sort(Comparator.comparing(Sprint::getStartDate).reversed());

            Map<Long, List<DailyEntry>> entriesBySprintId = new LinkedHashMap<>();
            Map<Long, List<DailyEntry>> todayEntriesBySprintId = new LinkedHashMap<>();
            for (Sprint s : allSprints) {
                entriesBySprintId.put(s.getId(), new ArrayList<>());
                if (s.getStatus() == Sprint.Status.ATIVA) {
                    todayEntriesBySprintId.put(s.getId(),
                            dailyEntryRepository.findBySprintAndEntryDateOrderByCreatedAtDesc(s, LocalDate.now()));
                }
            }
            for (DailyEntry entry : history) {
                Sprint s = entry.getSprint();
                if (s != null && entriesBySprintId.containsKey(s.getId())) {
                    entriesBySprintId.get(s.getId()).add(entry);
                }
            }

            model.addAttribute("team", team);
            model.addAttribute("history", history);
            model.addAttribute("sprint", sprint);
            model.addAttribute("allSprints", allSprints);
            model.addAttribute("entriesBySprintId", entriesBySprintId);
            model.addAttribute("todayEntriesBySprintId", todayEntriesBySprintId);
            model.addAttribute("totalEntries", history.size());
            model.addAttribute("startDate", start);
            model.addAttribute("endDate", end);

            return "team/team-history";
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/leader/team/" + teamId;
        }
    }

    @GetMapping("/team/{teamId}/sprint/report")
    public String viewSprintReport(@PathVariable Long teamId,
                                   Authentication auth,
                                   Model model,
                                   RedirectAttributes redirectAttributes) {
        try {
            User leader = getAuthenticatedUser(auth);
            Team team = teamService.getTeamWithMembers(teamId);

            if (!team.getLeader().getId().equals(leader.getId())) {
                redirectAttributes.addFlashAttribute("error", "Você não tem permissão para ver este relatório");
                return "redirect:/leader/team/" + teamId;
            }

            Sprint sprint = sprintService.getActiveSprint(teamId);
            if (sprint == null) {
                sprint = sprintRepository.findByTeam(team).stream()
                        .max(Comparator.comparing(Sprint::getStartDate))
                        .orElse(null);
            }

            List<DailyEntry> entries = sprint != null
                    ? dailyEntryRepository.findBySprint(sprint)
                    : List.of();

            Map<Long, List<DailyEntry>> entriesByMember = entries.stream()
                    .sorted(Comparator.comparing(DailyEntry::getEntryDate))
                    .collect(Collectors.groupingBy(e -> e.getUser().getId()));

            long totalDays = sprint != null
                    ? ChronoUnit.DAYS.between(sprint.getStartDate(), sprint.getEndDate()) + 1
                    : 0;
            long diasUteis = sprint != null
                    ? diasUteisNoPeriodo(sprint.getStartDate(), sprint.getEndDate())
                    : 0;

            model.addAttribute("team", team);
            model.addAttribute("sprint", sprint);
            model.addAttribute("entries", entries);
            model.addAttribute("entriesByMember", entriesByMember);
            model.addAttribute("totalDays", totalDays);
            model.addAttribute("diasUteis", diasUteis);
            model.addAttribute("backUrl", "/leader/team/" + teamId);
            model.addAttribute("canExportPdf", sprint != null);
            if (sprint != null) {
                model.addAttribute("sprintReport",
                        sprintReportService.findBySprint(sprint).orElse(null));
            }

            return "report";
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/leader/team/" + teamId;
        }
    }

    @GetMapping("/team/{teamId}/sprint/{sprintId}/report")
    public String viewSprintReportById(@PathVariable Long teamId,
                                       @PathVariable Long sprintId,
                                       Authentication auth,
                                       Model model,
                                       RedirectAttributes redirectAttributes) {
        try {
            User leader = getAuthenticatedUser(auth);
            Team team = teamService.getTeamWithMembers(teamId);

            if (!team.getLeader().getId().equals(leader.getId())) {
                redirectAttributes.addFlashAttribute("error", "Você não tem permissão para ver este relatório");
                return "redirect:/leader/team/" + teamId;
            }

            Sprint sprint = sprintRepository.findById(sprintId)
                    .orElseThrow(() -> new RuntimeException("Sprint não encontrada"));

            if (!sprint.getTeam().getId().equals(teamId)) {
                redirectAttributes.addFlashAttribute("error", "Você não tem permissão para ver este relatório");
                return "redirect:/leader/team/" + teamId;
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
            model.addAttribute("backUrl", "/leader/team/" + teamId + "/history");
            model.addAttribute("canExportPdf", true);
            model.addAttribute("sprintReport",
                    sprintReportService.findBySprint(sprint).orElse(null));

            return "report";
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/leader/team/" + teamId;
        }
    }

    @GetMapping("/team/{teamId}/sprint/{sprintId}/report/pdf")
    public ResponseEntity<byte[]> exportSprintReportPdf(@PathVariable Long teamId,
                                                         @PathVariable Long sprintId,
                                                         Authentication auth) {
        User leader = getAuthenticatedUser(auth);
        Team team;
        try {
            team = teamService.getTeamWithMembers(teamId);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }

        if (!team.getLeader().getId().equals(leader.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        Sprint sprint = sprintRepository.findById(sprintId).orElse(null);
        if (sprint == null || !sprint.getTeam().getId().equals(teamId)) {
            return ResponseEntity.notFound().build();
        }

        List<DailyEntry> entries = dailyEntryRepository.findBySprint(sprint).stream()
                .sorted(Comparator.comparing(DailyEntry::getEntryDate))
                .toList();

        Map<Long, List<DailyEntry>> entriesByMember = entries.stream()
                .collect(Collectors.groupingBy(e -> e.getUser().getId()));

        long totalDays = ChronoUnit.DAYS.between(sprint.getStartDate(), sprint.getEndDate()) + 1;

        Map<String, Object> vars = new HashMap<>();
        vars.put("team", team);
        vars.put("sprint", sprint);
        vars.put("entries", entries);
        vars.put("entriesByMember", entriesByMember);
        vars.put("totalDays", totalDays);

        byte[] pdf = pdfExportService.renderReportPdf(vars);

        String filename = "relatorio-" + PdfExportService.sanitizeFileNamePart(team.getName())
                + "-sprint" + sprint.getId() + ".pdf";

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(pdf);
    }

    @PostMapping("/team/{teamId}/sprint/close")
    public String closeSprint(@PathVariable Long teamId,
                              Authentication auth,
                              RedirectAttributes redirectAttributes) {
        try {
            User leader = getAuthenticatedUser(auth);
            Team team = teamService.getTeamWithMembers(teamId);
            if (!team.getLeader().getId().equals(leader.getId())) {
                redirectAttributes.addFlashAttribute("error", "Você não tem permissão para encerrar esta sprint");
                return "redirect:/leader/team/" + teamId;
            }
            Sprint sprint = sprintService.getActiveSprint(teamId);
            if (sprint != null) {
                sprint.setStatus(Sprint.Status.ENCERRADA);
                sprintRepository.save(sprint);
                sprintReportService.generateAndSaveAsync(sprint.getId());
                redirectAttributes.addFlashAttribute("success", "Sprint '" + sprint.getName() + "' encerrada! O relatório está sendo gerado e estará disponível em instantes.");
            } else {
                redirectAttributes.addFlashAttribute("error", "Nenhuma sprint ativa encontrada.");
            }
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/leader/team/" + teamId;
    }

    @PostMapping("/team/{teamId}/sprint/{sprintId}/report/generate")
    public String regenerateSprintReport(@PathVariable Long teamId,
                                         @PathVariable Long sprintId,
                                         Authentication auth,
                                         RedirectAttributes redirectAttributes) {
        try {
            User leader = getAuthenticatedUser(auth);
            Team team = teamService.getTeamWithMembers(teamId);
            if (!team.getLeader().getId().equals(leader.getId())) {
                redirectAttributes.addFlashAttribute("error", "Você não tem permissão para gerar este relatório");
                return "redirect:/leader/team/" + teamId;
            }

            Sprint sprint = sprintRepository.findById(sprintId)
                    .orElseThrow(() -> new RuntimeException("Sprint não encontrada"));

            if (!sprint.getTeam().getId().equals(teamId)) {
                redirectAttributes.addFlashAttribute("error", "Você não tem permissão para gerar este relatório");
                return "redirect:/leader/team/" + teamId;
            }

            sprintReportService.generateAndSave(sprint, team);
            redirectAttributes.addFlashAttribute("success", "Relatório gerado com sucesso!");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", "Não foi possível gerar o relatório: " + e.getMessage());
        }
        return "redirect:/leader/team/" + teamId + "/sprint/" + sprintId + "/report";
    }

    private User getAuthenticatedUser(Authentication auth) {
        String email = auth.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
    }

    private long diasUteisNoPeriodo(LocalDate inicio, LocalDate fim) {
        return inicio.datesUntil(fim.plusDays(1))
                .filter(d -> d.getDayOfWeek() != DayOfWeek.SATURDAY && d.getDayOfWeek() != DayOfWeek.SUNDAY)
                .count();
    }
}