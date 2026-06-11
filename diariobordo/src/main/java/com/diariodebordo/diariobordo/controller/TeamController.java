package com.diariodebordo.diariobordo.controller;

import com.diariodebordo.diariobordo.dto.AddMemberDTO;
import com.diariodebordo.diariobordo.dto.TeamCreateDTO;
import com.diariodebordo.diariobordo.model.Team;
import com.diariodebordo.diariobordo.model.User;
import com.diariodebordo.diariobordo.repository.UserRepository;
import com.diariodebordo.diariobordo.service.TeamService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/leader")
public class TeamController {

    private final TeamService teamService;
    private final UserRepository userRepository;

    public TeamController(TeamService teamService, UserRepository userRepository) {
        this.teamService = teamService;
        this.userRepository = userRepository;
    }

    // ⚠️ IMPORTANTE: Esta rota deve vir ANTES da rota com {teamId}
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
        
        // Verificar se o usuário é o líder
        if (!team.getLeader().getId().equals(leader.getId())) {
            redirectAttributes.addFlashAttribute("error", "Você não tem permissão para ver esta equipe");
            return "redirect:/leader/dashboard";
        }
        
        model.addAttribute("team", team);
        model.addAttribute("addMemberDTO", new AddMemberDTO());
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
            Team team = teamService.getTeamWithMembers(teamId);
            model.addAttribute("team", team);
            return "team/team-detail";
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
    
    private User getAuthenticatedUser(Authentication auth) {
        String email = auth.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
    }
}