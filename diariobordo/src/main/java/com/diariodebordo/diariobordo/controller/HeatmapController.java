package com.diariodebordo.diariobordo.controller;

import com.diariodebordo.diariobordo.dto.HeatmapDataDTO;
import com.diariodebordo.diariobordo.model.DailyEntry;
import com.diariodebordo.diariobordo.model.User;
import com.diariodebordo.diariobordo.repository.UserRepository;
import com.diariodebordo.diariobordo.service.HeatmapService;
import com.diariodebordo.diariobordo.service.HistoryService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Controller
public class HeatmapController {

    private final HeatmapService heatmapService;
    private final UserRepository userRepository;
    private final HistoryService historyService;

    public HeatmapController(HeatmapService heatmapService, UserRepository userRepository, HistoryService historyService) {
        this.heatmapService = heatmapService;
        this.userRepository = userRepository;
        this.historyService = historyService;
    }

    @GetMapping("/member/heatmap")
    public String memberHeatmap(Authentication auth, Model model) {
        return prepareHeatmap(auth, model, "member");
    }

    @GetMapping("/leader/heatmap")
    public String leaderHeatmap(Authentication auth, Model model) {
        return prepareHeatmap(auth, model, "leader");
    }

    @GetMapping("/professor/heatmap")
    public String professorHeatmap(Authentication auth, Model model) {
        return "redirect:/professor/panel";
    }

    private String prepareHeatmap(Authentication auth, Model model, String role) {
        User user = userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        List<HeatmapDataDTO> heatmapData = heatmapService.getHeatmapData(user, 180);

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
        double percentual = (double) diasComRegistro / heatmapData.size() * 100;
        long totalRegistros = heatmapData.stream().mapToLong(HeatmapDataDTO::getCount).sum();

        List<DailyEntry> ultimosRegistros = historyService
                .getUserHistory(user, LocalDate.now().minusDays(60), LocalDate.now())
                .stream().limit(7).collect(Collectors.toList());

        model.addAttribute("heatmapData", heatmapData);
        model.addAttribute("weeks", weeks);
        model.addAttribute("user", user);
        model.addAttribute("days", 180);
        model.addAttribute("role", role);
        model.addAttribute("diasComRegistro", diasComRegistro);
        model.addAttribute("percentual", String.format("%.0f", percentual));
        model.addAttribute("totalRegistros", totalRegistros);
        model.addAttribute("ultimosRegistros", ultimosRegistros);

        return role + "/heatmap";
    }
}