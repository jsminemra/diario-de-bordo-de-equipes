package com.diariodebordo.diariobordo.controller;

import com.diariodebordo.diariobordo.dto.CadastroDTO;
import com.diariodebordo.diariobordo.model.User;
import com.diariodebordo.diariobordo.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/cadastro")
    public String exibirCadastro(Model model) {
        model.addAttribute("cadastroDTO", new CadastroDTO());
        model.addAttribute("roles", User.Role.values());
        return "cadastro";
    }

    @PostMapping("/cadastro")
    // Validações de formulário — confirmação de senha feita manualmente pois Bean Validation
    // não compara campos cruzados nativamente
    public String processar(@Valid @ModelAttribute CadastroDTO dto,
                            BindingResult result,
                            Model model) {
        if (result.hasErrors()) {
            model.addAttribute("roles", User.Role.values());
            return "cadastro";
        }

        if (!dto.getPassword().equals(dto.getConfirmPassword())) {
            model.addAttribute("erro", "As senhas não coincidem");
            model.addAttribute("roles", User.Role.values());
            return "cadastro";
        }

        try {
            User user = new User();
            user.setName(dto.getName());
            user.setEmail(dto.getEmail());
            user.setPassword(dto.getPassword());
            user.setRole(dto.getRole());
            userService.cadastrar(user);
            return "redirect:/login";
        } catch (IllegalArgumentException e) {
            model.addAttribute("erro", e.getMessage());
            model.addAttribute("roles", User.Role.values());
            return "cadastro";
        } catch (Exception e) {
            model.addAttribute("erro", "Erro inesperado. Tente novamente.");
            model.addAttribute("roles", User.Role.values());
            return "cadastro";
        }
    }
}
