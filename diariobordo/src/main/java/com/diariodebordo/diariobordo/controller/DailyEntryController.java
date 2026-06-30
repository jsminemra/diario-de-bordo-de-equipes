package com.diariodebordo.diariobordo.controller;

import com.diariodebordo.diariobordo.dto.DailyEntryDTO;
import com.diariodebordo.diariobordo.model.DailyEntry;
import com.diariodebordo.diariobordo.model.Sprint;
import com.diariodebordo.diariobordo.model.User;
import com.diariodebordo.diariobordo.repository.UserRepository;
import com.diariodebordo.diariobordo.service.DailyEntryService;
import com.diariodebordo.diariobordo.service.HistoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/member")
@RequiredArgsConstructor
public class DailyEntryController {

    private final DailyEntryService dailyEntryService;
    private final UserRepository userRepository;
    private final HistoryService historyService;

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
            List<User> membrosAusentes = dailyEntryService.getMembrosAusentesHoje(user);

            model.addAttribute("registros", registros);
            model.addAttribute("jaRegistrou", jaRegistrou);
            model.addAttribute("entryHoje", entryHoje.orElse(null));
            model.addAttribute("membrosAusentes", membrosAusentes);
            model.addAttribute("usuario", user);
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

        LocalDate start = null;
        LocalDate end = null;

        if (startDate != null && !startDate.isEmpty()) {
            start = LocalDate.parse(startDate);
        } else if (sprintAtiva != null) {
            start = sprintAtiva.getStartDate();
        } else {
            start = LocalDate.now().minusDays(30);
        }

        if (endDate != null && !endDate.isEmpty()) {
            end = LocalDate.parse(endDate);
        } else if (sprintAtiva != null) {
            end = sprintAtiva.getEndDate();
        } else {
            end = LocalDate.now();
        }

        List<DailyEntry> history = historyService.getUserHistory(user, start, end);

        model.addAttribute("history", history);
        model.addAttribute("sprintAtiva", sprintAtiva);
        model.addAttribute("startDate", start);
        model.addAttribute("endDate", end);
        model.addAttribute("usuario", user);

        return "member/history";
    }
}