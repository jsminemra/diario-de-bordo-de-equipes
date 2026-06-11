package com.diariodebordo.diariobordo.config;

import com.diariodebordo.diariobordo.model.User;
import com.diariodebordo.diariobordo.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) throws Exception {
        // Criar usuário MEMBER se não existir
        if (userRepository.findByEmail("membro@email.com").isEmpty()) {
            User member = new User();
            member.setEmail("membro@email.com");
            member.setPassword(passwordEncoder.encode("123456"));
            member.setName("João Membro");
            member.setRole(User.Role.MEMBER);
            userRepository.save(member);
            System.out.println("Usuário MEMBER criado!");
        }

        // Criar usuário LEADER se não existir
        if (userRepository.findByEmail("lider@email.com").isEmpty()) {
            User leader = new User();
            leader.setEmail("lider@email.com");
            leader.setPassword(passwordEncoder.encode("123456"));
            leader.setName("Maria Líder");
            leader.setRole(User.Role.LEADER);
            userRepository.save(leader);
            System.out.println("Usuário LEADER criado!");
        }

        // Criar usuário PROFESSOR se não existir
        if (userRepository.findByEmail("professora@email.com").isEmpty()) {
            User professor = new User();
            professor.setEmail("professora@email.com");
            professor.setPassword(passwordEncoder.encode("123456"));
            professor.setName("Dra. Professora");
            professor.setRole(User.Role.PROFESSOR);
            userRepository.save(professor);
            System.out.println("Usuário PROFESSOR criado!");
        }
    }
}