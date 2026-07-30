package com.diariodebordo.diariobordo.controller;

import com.diariodebordo.diariobordo.dto.DailyEntryDTO;
import com.diariodebordo.diariobordo.model.DailyEntry;
import com.diariodebordo.diariobordo.model.Sprint;
import com.diariodebordo.diariobordo.model.Team;
import com.diariodebordo.diariobordo.model.User;
import com.diariodebordo.diariobordo.repository.DailyEntryRepository;
import com.diariodebordo.diariobordo.repository.SprintRepository;
import com.diariodebordo.diariobordo.repository.UserRepository;
import com.diariodebordo.diariobordo.service.DailyEntryService;
import com.diariodebordo.diariobordo.service.HistoryService;
import com.diariodebordo.diariobordo.service.SprintReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/member")
@RequiredArgsConstructor
public class DailyEntryController {

    private final DailyEntryService dailyEntryService;
    private final UserRepository userRepository;
    private final HistoryService historyService;
    private final SprintRepository sprintRepository;
    private final DailyEntryRepository dailyEntryRepository;
    private final SprintReportService sprintReportService;

    @GetMapping("/entry/create")
    public String exibirFormulario(Model model, Authentication auth) {
        User user = userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        Optional<DailyEntry> registroHoje = dailyEntryService.buscarRegistroDeHoje(user);

        DailyEntryDTO dto = new DailyEntryDTO();
        registroHoje.ifPresent(entry -> {
            dto.setWhatWasDone(entry.getWhatWasDone() != null ? entry.getWhatWasDone() : "");
            dto.setWhatWillBeDone(entry.getWhatWillBeDone() != null ? entry.getWhatWillBeDone() : "");
            dto.setImpediments(entry.getImpediments() != null ? entry.getImpediments() : "");
        });

        model.addAttribute("dailyEntryDTO", dto);
        model.addAttribute("jaRegistrou", registroHoje.isPresent());
        return "member/entry-form";
    }

    @PostMapping("/entry/create")
    public String salvar(@Valid @ModelAttribute DailyEntryDTO dto,
                         BindingResult result,
                         Model model,
                         Authentication auth) {
        if (result.hasErrors()) {
            model.addAttribute("jaRegistrou", false);
            return "member/entry-form";
        }
        try {
            User user = userRepository.findByEmail(auth.getName())
                    .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
            dailyEntryService.salvarOuEditar(dto, user);
            return "redirect:/member/feed";
        } catch (RuntimeException e) {
            model.addAttribute("erro", "Erro ao salvar registro: " + e.getMessage());
            model.addAttribute("jaRegistrou", false);
            return "member/entry-form";
        }
    }

    @GetMapping("/feed")
    public String exibirFeed(Model model, Authentication auth) {
        try {
            User user = userRepository.findByEmail(auth.getName())
                    .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

            List<DailyEntry> registros = dailyEntryService.buscarFeedDoDia(user);
            Optional<DailyEntry> entryHoje = dailyEntryService.buscarRegistroDeHoje(user);
            boolean jaRegistrou = entryHoje.isPresent();
            List<User> membrosAusentes = dailyEntryService.getMembrosAusentesHoje(user, registros);

            Sprint sprintAtiva = historyService.getSprintAtiva(user);
            long diasRestantes = 0;
            if (sprintAtiva != null) {
                diasRestantes = Math.max(0, ChronoUnit.DAYS.between(LocalDate.now(), sprintAtiva.getEndDate()));
            }

            model.addAttribute("registros", registros);
            model.addAttribute("jaRegistrou", jaRegistrou);
            model.addAttribute("entryHoje", entryHoje.orElse(null));
            model.addAttribute("membrosAusentes", membrosAusentes);
            model.addAttribute("usuario", user);
            model.addAttribute("sprintAtiva", sprintAtiva);
            model.addAttribute("diasRestantes", diasRestantes);
            model.addAttribute("temSprint", true);
            model.addAttribute("mensagem", null);

            return "member/feed";

        } catch (RuntimeException e) {
            User user = userRepository.findByEmail(auth.getName()).orElse(null);

            model.addAttribute("temSprint", false);
            model.addAttribute("mensagem", "Nenhuma sprint ativa no momento. Aguarde o líder criar uma sprint para começar os registros.");
            model.addAttribute("usuario", user);
            model.addAttribute("registros", List.of());
            model.addAttribute("jaRegistrou", false);
            model.addAttribute("entryHoje", null);
            model.addAttribute("membrosAusentes", List.of());

            return "member/feed";
        }
    }

    @GetMapping("/history")
    public String exibirHistorico(@RequestParam(required = false) String startDate,
                                  @RequestParam(required = false) String endDate,
                                  Model model,
                                  Authentication auth) {
        User user = userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        Sprint sprintAtiva = historyService.getSprintAtiva(user);

        // All entries across all sprints for grouping
        List<DailyEntry> allHistory = historyService.getUserHistory(
                user, LocalDate.now().minusYears(2), LocalDate.now());

        // Group by sprint preserving order (most recent sprint first via entry dates)
        Map<Long, List<DailyEntry>> entriesBySprintId = new LinkedHashMap<>();
        Map<Long, Sprint> sprintsById = new LinkedHashMap<>();
        for (DailyEntry entry : allHistory) {
            Sprint s = entry.getSprint();
            if (s != null) {
                entriesBySprintId.computeIfAbsent(s.getId(), k -> new ArrayList<>()).add(entry);
                sprintsById.putIfAbsent(s.getId(), s);
            }
        }
        List<Sprint> allSprints = new ArrayList<>(sprintsById.values());

        model.addAttribute("history", allHistory);
        model.addAttribute("allSprints", allSprints);
        model.addAttribute("entriesBySprintId", entriesBySprintId);
        model.addAttribute("totalEntries", allHistory.size());
        model.addAttribute("sprintAtiva", sprintAtiva);
        model.addAttribute("usuario", user);

        return "member/history";
    }

    @GetMapping("/sprint/{sprintId}/report")
    public String visualizarRelatorio(@PathVariable Long sprintId,
                                      Authentication auth,
                                      Model model,
                                      RedirectAttributes redirectAttributes) {
        User user = userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        Sprint sprint = sprintRepository.findById(sprintId).orElse(null);
        if (sprint == null) {
            redirectAttributes.addFlashAttribute("error", "Sprint não encontrada");
            return "redirect:/member/history";
        }

        Team team = sprint.getTeam();
        boolean pertenceAEquipe = team.getMembers().stream()
                .anyMatch(m -> m.getId().equals(user.getId()));
        if (!pertenceAEquipe) {
            redirectAttributes.addFlashAttribute("error", "Você não tem permissão para ver este relatório");
            return "redirect:/member/history";
        }

        List<DailyEntry> entries = dailyEntryRepository.findBySprint(sprint).stream()
                .sorted(Comparator.comparing(DailyEntry::getEntryDate))
                .toList();

        Map<Long, List<DailyEntry>> entriesByMember = entries.stream()
                .collect(Collectors.groupingBy(e -> e.getUser().getId()));

        long totalDays = ChronoUnit.DAYS.between(sprint.getStartDate(), sprint.getEndDate()) + 1;
        long diasUteis = diasUteisNoPeriodo(sprint.getStartDate(), sprint.getEndDate());

        model.addAttribute("team", team);
        model.addAttribute("sprint", sprint);
        model.addAttribute("entries", entries);
        model.addAttribute("entriesByMember", entriesByMember);
        model.addAttribute("totalDays", totalDays);
        model.addAttribute("diasUteis", diasUteis);
        model.addAttribute("backUrl", "/member/history");
        model.addAttribute("canExportPdf", false);
        model.addAttribute("sprintReport", sprintReportService.findBySprint(sprint).orElse(null));

        return "report";
    }

    private long diasUteisNoPeriodo(LocalDate inicio, LocalDate fim) {
        return inicio.datesUntil(fim.plusDays(1))
                .filter(d -> d.getDayOfWeek() != DayOfWeek.SATURDAY && d.getDayOfWeek() != DayOfWeek.SUNDAY)
                .count();
    }
}