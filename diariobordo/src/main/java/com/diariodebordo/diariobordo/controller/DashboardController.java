package com.diariodebordo.diariobordo.controller;

import com.diariodebordo.diariobordo.model.Team;
import com.diariodebordo.diariobordo.model.User;
import com.diariodebordo.diariobordo.repository.UserRepository;
import com.diariodebordo.diariobordo.service.TeamService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class DashboardController {

    private final TeamService teamService;
    private final UserRepository userRepository;

    public DashboardController(TeamService teamService, UserRepository userRepository) {
        this.teamService = teamService;
        this.userRepository = userRepository;
    }
    
    @GetMapping("/leader/team")
    public String leaderTeam(Authentication auth) {
        String email = auth.getName();
        User leader = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
        
        List<Team> teams = teamService.getTeamsByLeader(leader);
        
        if (!teams.isEmpty()) {
            return "redirect:/leader/team/" + teams.get(0).getId();
        }
        
        return "redirect:/leader/team/create";
    }
}