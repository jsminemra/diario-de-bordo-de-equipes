package com.diariodebordo.diariobordo.controller;

import com.diariodebordo.diariobordo.dto.GitHubCommitDTO;
import com.diariodebordo.diariobordo.dto.GitHubUserDTO;
import com.diariodebordo.diariobordo.dto.HeatmapDataDTO;
import com.diariodebordo.diariobordo.model.Team;
import com.diariodebordo.diariobordo.model.User;
import com.diariodebordo.diariobordo.repository.TeamRepository;
import com.diariodebordo.diariobordo.repository.UserRepository;
import com.diariodebordo.diariobordo.service.GitHubService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.List;

@Controller
public class GitHubController {

    private final GitHubService gitHubService;
    private final UserRepository userRepository;
    private final TeamRepository teamRepository;

    public GitHubController(GitHubService gitHubService, UserRepository userRepository, TeamRepository teamRepository) {
        this.gitHubService = gitHubService;
        this.userRepository = userRepository;
        this.teamRepository = teamRepository;
    }

    @GetMapping("/github/link")
    public String showLinkForm(Authentication auth, Model model) {
        User user = userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        model.addAttribute("user", user);
        model.addAttribute("currentGithub", user.getGithubUsername());

        return "github/link";
    }

    @PostMapping("/github/link")
    public String linkGitHub(@RequestParam String githubUsername,
                             Authentication auth,
                             RedirectAttributes redirectAttributes) {
        User user = userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        if (gitHubService.validateGitHubUsername(githubUsername)) {
            user.setGithubUsername(githubUsername);
            userRepository.save(user);
            redirectAttributes.addFlashAttribute("success", "Conta do GitHub vinculada com sucesso!");
        } else {
            redirectAttributes.addFlashAttribute("error", "Usuário do GitHub não encontrado.");
        }

        return "redirect:/github/link";
    }

    @PostMapping("/github/unlink")
    public String unlinkGitHub(Authentication auth, RedirectAttributes redirectAttributes) {
        User user = userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        user.setGithubUsername(null);
        userRepository.save(user);
        redirectAttributes.addFlashAttribute("success", "Conta do GitHub desvinculada com sucesso!");

        return "redirect:/github/link";
    }

    @GetMapping("/github/heatmap")
    public String viewGitHubHeatmap(Authentication auth, Model model, RedirectAttributes redirectAttributes) {
        User user = userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        if (user.getGithubUsername() == null || user.getGithubUsername().isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Vincule sua conta do GitHub primeiro.");
            return "redirect:/github/link";
        }

        Team team = teamRepository.findByMemberId(user.getId()).stream().findFirst().orElse(null);
        if (team == null || team.getGithubRepo() == null || team.getGithubRepo().isBlank()) {
            redirectAttributes.addFlashAttribute("error",
                    "O repositório GitHub da sua equipe ainda não foi cadastrado pelo líder.");
            return "redirect:/github/link";
        }

        int days = 180;
        GitHubService.CommitFetchResult syncInfo = gitHubService.fetchCommits(user, team);
        List<HeatmapDataDTO> heatmapData = gitHubService.buildHeatmap(syncInfo.getCommits(), days);

        List<List<HeatmapDataDTO>> weeks = new ArrayList<>();
        List<HeatmapDataDTO> currentWeek = new ArrayList<>();
        for (int i = 0; i < heatmapData.size(); i++) {
            currentWeek.add(heatmapData.get(i));
            if ((i + 1) % 7 == 0 || i == heatmapData.size() - 1) {
                weeks.add(currentWeek);
                currentWeek = new ArrayList<>();
            }
        }

        long diasComRegistro = heatmapData.stream().filter(d -> d.getCount() > 0).count();
        double percentual = heatmapData.isEmpty() ? 0 : (double) diasComRegistro / heatmapData.size() * 100;
        long totalCommits = heatmapData.stream().mapToLong(HeatmapDataDTO::getCount).sum();

        model.addAttribute("heatmapData", heatmapData);
        model.addAttribute("weeks", weeks);
        model.addAttribute("user", user);
        model.addAttribute("days", days);
        model.addAttribute("diasComRegistro", diasComRegistro);
        model.addAttribute("percentual", String.format("%.0f", percentual));
        model.addAttribute("totalRegistros", totalCommits);
        model.addAttribute("githubUsername", user.getGithubUsername());
        model.addAttribute("githubRepo", team.getGithubRepo());
        model.addAttribute("lastSyncedAt", syncInfo.getLastSyncedAt());
        model.addAttribute("rateLimited", syncInfo.isRateLimited());

        return "github/heatmap";
    }

    @GetMapping("/github/commits")
    public String viewCommits(Authentication auth, Model model, RedirectAttributes redirectAttributes) {
        User user = userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        if (user.getGithubUsername() == null || user.getGithubUsername().isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Vincule sua conta do GitHub primeiro.");
            return "redirect:/github/link";
        }

        Team team = teamRepository.findByMemberId(user.getId()).stream().findFirst().orElse(null);
        if (team == null || team.getGithubRepo() == null || team.getGithubRepo().isBlank()) {
            redirectAttributes.addFlashAttribute("error",
                    "O repositório GitHub da sua equipe ainda não foi cadastrado pelo líder.");
            return "redirect:/github/link";
        }

        GitHubService.CommitFetchResult result = gitHubService.fetchCommits(user, team);
        List<GitHubCommitDTO> commits = result.getCommits();
        GitHubUserDTO userInfo = gitHubService.getUserInfo(user.getGithubUsername());

        model.addAttribute("commits", commits);
        model.addAttribute("userInfo", userInfo);
        model.addAttribute("githubUsername", user.getGithubUsername());
        model.addAttribute("githubRepo", team.getGithubRepo());
        model.addAttribute("lastSyncedAt", result.getLastSyncedAt());
        model.addAttribute("rateLimited", result.isRateLimited());

        return "github/commits";
    }
}
