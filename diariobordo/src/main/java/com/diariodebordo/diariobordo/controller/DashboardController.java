package com.diariodebordo.diariobordo.controller;

import com.diariodebordo.diariobordo.model.Team;
import com.diariodebordo.diariobordo.model.User;
import com.diariodebordo.diariobordo.repository.UserRepository;
import com.diariodebordo.diariobordo.service.TeamService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;
import java.util.Optional;

@Controller
public class DashboardController {

    private final TeamService teamService;
    private final UserRepository userRepository;

    public DashboardController(TeamService teamService, UserRepository userRepository) {
        this.teamService = teamService;
        this.userRepository = userRepository;
    }
    
    @GetMapping("/member/feed")
    public String memberFeed() {
        return "member/feed";
    }
    
    @GetMapping("/member/team")
    public String memberTeam(Authentication auth, Model model) {
        User user = userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        Optional<Team> team = teamService.getTeamByMember(user);
        if (team.isEmpty()) {
            model.addAttribute("user", user);
            model.addAttribute("userName", user.getName());
            return "member/team-profile";
        }
        return "redirect:/member/team/" + team.get().getId();
    }

    @GetMapping("/member/team/{teamId}")
    public String memberTeamProfile(@PathVariable Long teamId, Authentication auth, Model model) {
        User user = userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        Team team = teamService.getTeamWithMembers(teamId);

        boolean isMember = team.getMembers().stream()
                .anyMatch(m -> m.getId().equals(user.getId()));
        if (!isMember) {
            throw new AccessDeniedException("Você não faz parte desta equipe");
        }

        model.addAttribute("team", team);
        model.addAttribute("user", user);
        model.addAttribute("userName", user.getName());
        return "member/team-profile";
    }

    @GetMapping("/leader/team")
    public String leaderTeam(Authentication auth) {
        String email = auth.getName();
        User leader = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
        
        List<Team> teams = teamService.getTeamsByLeader(leader);
        
        // Se já tem equipe, vai para a página da equipe
        if (!teams.isEmpty()) {
            return "redirect:/leader/team/" + teams.get(0).getId();
        }
        
        // Se não tem equipe, vai para página de criação
        return "redirect:/leader/team/create";
    }
    
    /*@GetMapping("/professor/panel")
     public String professorPanel() {
        return "redirect:/professor/panel";
     }
        */
}