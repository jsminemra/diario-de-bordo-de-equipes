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
import org.springframework.web.bind.annotation.*;

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

        // Se já registrou hoje, pré-preenche o formulário para edição
        DailyEntryDTO dto = new DailyEntryDTO();
        registroHoje.ifPresent(entry -> {
            dto.setWhatWasDone(entry.getWhatWasDone());
            dto.setWhatWillBeDone(entry.getWhatWillBeDone());
            dto.setImpediments(entry.getImpediments());
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
        } catch (Exception e) {
            model.addAttribute("erro", "Erro ao salvar registro. Tente novamente.");
            model.addAttribute("jaRegistrou", false);
            return "member/entry-form";
        }
    }

    @GetMapping("/feed")
    public String exibirFeed(Model model, Authentication auth) {
        User user = userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        List<DailyEntry> registros = dailyEntryService.buscarFeedDoDia(user);
        boolean jaRegistrou = dailyEntryService.buscarRegistroDeHoje(user).isPresent();

        model.addAttribute("registros", registros);
        model.addAttribute("jaRegistrou", jaRegistrou);
        model.addAttribute("usuario", user);
        return "member/feed";
    }
}