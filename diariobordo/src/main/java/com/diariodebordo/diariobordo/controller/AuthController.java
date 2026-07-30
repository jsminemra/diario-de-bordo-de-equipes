package com.diariodebordo.diariobordo.controller;

import com.diariodebordo.diariobordo.model.User;
import com.diariodebordo.diariobordo.repository.TeamRepository;
import com.diariodebordo.diariobordo.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

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

        boolean hasTeam = !teamRepository.findByMemberId(user.getId()).isEmpty();

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