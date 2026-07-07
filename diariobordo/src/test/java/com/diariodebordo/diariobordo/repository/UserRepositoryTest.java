package com.diariodebordo.diariobordo.repository;

import com.diariodebordo.diariobordo.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
class UserRepositoryTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private UserRepository userRepository;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setEmail("joao@email.com");
        user.setPassword("senha123");
        user.setName("João Silva");
        user.setRole(User.Role.MEMBER);
    }

    @Test
    void findByEmail_deveRetornarUsuarioCorreto() {
        em.persistAndFlush(user);

        Optional<User> result = userRepository.findByEmail("joao@email.com");

        assertThat(result).isPresent();
        assertThat(result.get().getEmail()).isEqualTo("joao@email.com");
        assertThat(result.get().getName()).isEqualTo("João Silva");
    }

    @Test
    void findByEmail_deveRetornarVazioParaEmailInexistente() {
        Optional<User> result = userRepository.findByEmail("naoexiste@email.com");

        assertThat(result).isEmpty();
    }

    @Test
    void save_deveLancarExcecaoParaEmailDuplicado() {
        em.persistAndFlush(user);

        User duplicado = new User();
        duplicado.setEmail("joao@email.com");
        duplicado.setPassword("outrasenha");
        duplicado.setName("João Duplicado");
        duplicado.setRole(User.Role.LEADER);

        assertThatThrownBy(() -> em.persistAndFlush(duplicado))
                .isInstanceOf(Exception.class);
    }
}
