package com.diariodebordo.diariobordo.controller;

import com.diariodebordo.diariobordo.dto.JoinTeamDTO;
import com.diariodebordo.diariobordo.model.User;
import com.diariodebordo.diariobordo.repository.TeamRepository;
import com.diariodebordo.diariobordo.repository.UserRepository;
import com.diariodebordo.diariobordo.service.TeamService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/member")
public class MemberController {

    private final UserRepository userRepository;
    private final TeamRepository teamRepository;
    private final TeamService teamService;

    public MemberController(UserRepository userRepository, TeamRepository teamRepository, TeamService teamService) {
        this.userRepository = userRepository;
        this.teamRepository = teamRepository;
        this.teamService = teamService;
    }

    @GetMapping("/dashboard")
    public String dashboard(Authentication auth, Model model) {
        String email = auth.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        boolean hasTeam = !teamRepository.findByMemberId(user.getId()).isEmpty();

        if (!hasTeam) {
            model.addAttribute("user", user);
            model.addAttribute("joinTeamDTO", new JoinTeamDTO());
            return "member/no-team";
        }
        return "redirect:/member/feed";
    }

    @PostMapping("/join")
    public String joinTeam(@Valid @ModelAttribute("joinTeamDTO") JoinTeamDTO dto,
                           BindingResult result,
                           Authentication auth,
                           RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            redirectAttributes.addFlashAttribute("error", "Informe o código de convite");
            return "redirect:/member/dashboard";
        }

        try {
            User user = userRepository.findByEmail(auth.getName())
                    .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
            var team = teamService.joinTeamByCode(dto.getCode(), user);
            redirectAttributes.addFlashAttribute("success", "Você entrou na equipe '" + team.getName() + "'!");
            return "redirect:/member/feed";
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/member/dashboard";
        }
    }
}