package com.diariodebordo.diariobordo.service;

import com.diariodebordo.diariobordo.model.User;
import com.diariodebordo.diariobordo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder; // Bean configurado pelo Diogo (Spring Security)

    public User cadastrar(User user) {
        if (userRepository.findByEmail(user.getEmail()).isPresent()) {
            throw new IllegalArgumentException("E-mail já cadastrado");
        }
        // TO-DO: remover comentário após Diogo configurar o Spring Security (US-01b)
        // user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setPassword(user.getPassword()); // temporário — senha sem encode
        return userRepository.save(user);
    }

    public User buscarPorEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
    }
}
