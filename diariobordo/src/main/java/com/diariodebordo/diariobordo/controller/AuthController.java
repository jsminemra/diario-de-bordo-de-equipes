package com.diariodebordo.diariobordo.controller;

import com.diariodebordo.diariobordo.model.Team;
import com.diariodebordo.diariobordo.model.User;
import com.diariodebordo.diariobordo.repository.TeamRepository;
import com.diariodebordo.diariobordo.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class AuthController {

    private final UserRepository userRepository;
    private final TeamRepository teamRepository;

    public AuthController(UserRepository userRepository, TeamRepository teamRepository) {
        this.userRepository = userRepository;
        this.teamRepository = teamRepository;
    }

    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    @GetMapping("/dashboard")
    public String dashboard(Authentication auth) {
        String email = auth.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        boolean hasTeam = teamRepository.findAll().stream()
                .anyMatch(t -> t.getMembers().stream()
                        .anyMatch(m -> m.getId().equals(user.getId())));

        switch (user.getRole()) {
            case PROFESSOR:
                return "redirect:/professor/panel";
            case LEADER:
                return "redirect:/leader/team";
            default: // MEMBER
                if (!hasTeam) {
                    return "redirect:/member/dashboard";
                }
                return "redirect:/member/feed";
        }
    }
}