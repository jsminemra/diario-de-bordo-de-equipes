package com.diariodebordo.diariobordo.controller;

import com.diariodebordo.diariobordo.dto.DailyEntryDTO;
import com.diariodebordo.diariobordo.model.DailyEntry;
import com.diariodebordo.diariobordo.model.User;
import com.diariodebordo.diariobordo.repository.UserRepository;
import com.diariodebordo.diariobordo.service.DailyEntryService;
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

import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/member")
@RequiredArgsConstructor
public class DailyEntryController {

    private final DailyEntryService dailyEntryService;
    private final UserRepository userRepository;

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
            boolean jaRegistrou = dailyEntryService.buscarRegistroDeHoje(user).isPresent();

            model.addAttribute("registros", registros);
            model.addAttribute("jaRegistrou", jaRegistrou);
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
            
            return "member/feed";
        }
    }
}