package com.diariodebordo.diariobordo.controller;

import com.diariodebordo.diariobordo.dto.RegisterDTO;
import com.diariodebordo.diariobordo.model.User;
import com.diariodebordo.diariobordo.service.RegisterService;
import jakarta.validation.Valid;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class RegisterController {

    private final RegisterService registerService;
    private final AuthenticationManager authenticationManager;

    public RegisterController(RegisterService registerService, AuthenticationManager authenticationManager) {
        this.registerService = registerService;
        this.authenticationManager = authenticationManager;
    }

    @GetMapping("/register")
    public String showRegisterForm(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            return "redirect:/dashboard";
        }
        
        model.addAttribute("registerDTO", new RegisterDTO());
        return "register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("registerDTO") RegisterDTO dto,
                           BindingResult result,
                           Model model,
                           RedirectAttributes redirectAttributes) {
        
        if (result.hasErrors()) {
            return "register";
        }

        try {
            User newUser = registerService.register(dto);
            
            UsernamePasswordAuthenticationToken authToken = 
                    new UsernamePasswordAuthenticationToken(dto.getEmail(), dto.getPassword());
            Authentication authentication = authenticationManager.authenticate(authToken);
            SecurityContextHolder.getContext().setAuthentication(authentication);
            
            redirectAttributes.addFlashAttribute("success", "Cadastro realizado com sucesso! Bem-vindo(a), " + newUser.getName() + "!");
            return "redirect:/dashboard";
            
        } catch (RuntimeException e) {
            model.addAttribute("error", e.getMessage());
            return "register";
        } catch (Exception e) {
            model.addAttribute("error", "Erro ao realizar cadastro. Tente novamente.");
            return "register";
        }
    }
}