package com.diariodebordo.diariobordo.controller;

import com.diariodebordo.diariobordo.model.Team;
import com.diariodebordo.diariobordo.model.User;
import com.diariodebordo.diariobordo.repository.TeamRepository;
import com.diariodebordo.diariobordo.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/professor")
public class ProfessorController {

    private final TeamRepository teamRepository;
    private final UserRepository userRepository;

    public ProfessorController(TeamRepository teamRepository, UserRepository userRepository) {
        this.teamRepository = teamRepository;
        this.userRepository = userRepository;
    }

    @GetMapping("/panel")
    public String panel(Model model) {
        // Buscar todas as equipes
        List<Team> allTeams = teamRepository.findAll();
        model.addAttribute("teams", allTeams);
        model.addAttribute("totalTeams", allTeams.size());
        
        // Buscar todos os usuários (para estatísticas)
        List<User> allUsers = userRepository.findAll();
        model.addAttribute("totalUsers", allUsers.size());
        
        return "professor/panel";
    }
    
    @GetMapping("/team/{teamId}")
    public String viewTeam(@PathVariable Long teamId, Model model) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new RuntimeException("Equipe não encontrada"));
        model.addAttribute("team", team);
        return "professor/team-detail";
    }
}