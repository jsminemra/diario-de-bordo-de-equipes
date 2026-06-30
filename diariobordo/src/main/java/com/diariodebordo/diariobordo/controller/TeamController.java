package com.diariodebordo.diariobordo.controller;

import com.diariodebordo.diariobordo.dto.AddMemberDTO;
import com.diariodebordo.diariobordo.dto.DailyEntryDTO;
import com.diariodebordo.diariobordo.dto.SprintCreateDTO;
import com.diariodebordo.diariobordo.dto.TeamCreateDTO;
import com.diariodebordo.diariobordo.model.DailyEntry;
import com.diariodebordo.diariobordo.model.Sprint;
import com.diariodebordo.diariobordo.model.Team;
import com.diariodebordo.diariobordo.model.User;
import com.diariodebordo.diariobordo.repository.SprintRepository;
import com.diariodebordo.diariobordo.repository.UserRepository;
import com.diariodebordo.diariobordo.service.DailyEntryService;
import com.diariodebordo.diariobordo.service.HistoryService;
import com.diariodebordo.diariobordo.service.SprintService;
import com.diariodebordo.diariobordo.service.TeamService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/leader")
public class TeamController {

    private final TeamService teamService;
    private final UserRepository userRepository;
    private final DailyEntryService dailyEntryService;
    private final SprintRepository sprintRepository;
    private final SprintService sprintService;
    private final HistoryService historyService;

    public TeamController(TeamService teamService, 
                          UserRepository userRepository,
                          DailyEntryService dailyEntryService, 
                          SprintRepository sprintRepository,
                          SprintService sprintService,
                          HistoryService historyService) {
        this.teamService = teamService;
        this.userRepository = userRepository;
        this.dailyEntryService = dailyEntryService;
        this.sprintRepository = sprintRepository;
        this.sprintService = sprintService;
        this.historyService = historyService;
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

            model.addAttribute("team", team);
            model.addAttribute("history", history);
            model.addAttribute("sprint", sprint);
            model.addAttribute("allSprints", allSprints);
            model.addAttribute("startDate", start);
            model.addAttribute("endDate", end);

            return "team/team-history";
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/leader/team/" + teamId;
        }
    }

    private User getAuthenticatedUser(Authentication auth) {
        String email = auth.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
    }
}