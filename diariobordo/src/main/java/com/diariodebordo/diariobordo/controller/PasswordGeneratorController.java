package com.diariodebordo.diariobordo.controller;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PasswordGeneratorController {

    @GetMapping("/generate-password")
    public String generatePassword(@RequestParam String password) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(10);
        String hash = encoder.encode(password);
        return "Senha: " + password + "<br>Hash BCrypt: " + hash;
    }
}