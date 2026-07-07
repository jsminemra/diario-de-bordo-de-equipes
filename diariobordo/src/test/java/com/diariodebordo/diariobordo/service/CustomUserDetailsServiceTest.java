package com.diariodebordo.diariobordo.service;

import com.diariodebordo.diariobordo.model.User;
import com.diariodebordo.diariobordo.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService service;

    @Test
    void deveRetornarUserDetailsQuandoUsuarioExiste() {
        User user = new User();
        user.setEmail("membro@email.com");
        user.setPassword("senha123");
        user.setRole(User.Role.MEMBER);

        when(userRepository.findByEmail("membro@email.com")).thenReturn(Optional.of(user));

        UserDetails details = service.loadUserByUsername("membro@email.com");

        assertThat(details.getUsername()).isEqualTo("membro@email.com");
        assertThat(details.getPassword()).isEqualTo("senha123");
        assertThat(details.getAuthorities()).hasSize(1);
        assertThat(details.getAuthorities().iterator().next().getAuthority()).isEqualTo("ROLE_MEMBER");
    }

    @Test
    void deveLancarUsernameNotFoundExceptionQuandoUsuarioNaoExiste() {
        when(userRepository.findByEmail("naoexiste@email.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.loadUserByUsername("naoexiste@email.com"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("naoexiste@email.com");
    }
}
