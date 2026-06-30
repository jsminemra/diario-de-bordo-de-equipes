package com.diariodebordo.diariobordo.controller;

import com.diariodebordo.diariobordo.model.Team;
import com.diariodebordo.diariobordo.model.User;
import com.diariodebordo.diariobordo.repository.TeamRepository;
import com.diariodebordo.diariobordo.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/member")
public class MemberController {

    private final UserRepository userRepository;
    private final TeamRepository teamRepository;

    public MemberController(UserRepository userRepository, TeamRepository teamRepository) {
        this.userRepository = userRepository;
        this.teamRepository = teamRepository;
    }

    @GetMapping("/dashboard")
    public String dashboard(Authentication auth, Model model) {
        String email = auth.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        boolean hasTeam = teamRepository.findAll().stream()
                .anyMatch(t -> t.getMembers().stream()
                        .anyMatch(m -> m.getId().equals(user.getId())));

        if (!hasTeam) {
            model.addAttribute("user", user);
            return "member/no-team";
        }
        return "redirect:/member/feed";
    }
}