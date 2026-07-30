package com.diariodebordo.diariobordo.controller;

import com.diariodebordo.diariobordo.config.SecurityConfig;
import com.diariodebordo.diariobordo.model.Team;
import com.diariodebordo.diariobordo.model.User;
import com.diariodebordo.diariobordo.repository.TeamRepository;
import com.diariodebordo.diariobordo.repository.UserRepository;
import com.diariodebordo.diariobordo.service.CustomUserDetailsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private TeamRepository teamRepository;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    void getLoginDeveRetornarStatus200EViewLogin() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("login"));
    }

    @Test
    @WithMockUser(username = "prof@email.com", roles = "PROFESSOR")
    void getDashboardComRoleProfessorDeveRedirecionarParaProfessorPanel() throws Exception {
        User user = new User();
        user.setEmail("prof@email.com");
        user.setRole(User.Role.PROFESSOR);

        when(userRepository.findByEmail("prof@email.com")).thenReturn(Optional.of(user));

        mockMvc.perform(get("/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/professor/panel"));
    }

    @Test
    @WithMockUser(username = "lider@email.com", roles = "LEADER")
    void getDashboardComRoleLeaderDeveRedirecionarParaLeaderTeam() throws Exception {
        User user = new User();
        user.setEmail("lider@email.com");
        user.setRole(User.Role.LEADER);

        when(userRepository.findByEmail("lider@email.com")).thenReturn(Optional.of(user));

        mockMvc.perform(get("/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/leader/team"));
    }

    @Test
    @WithMockUser(username = "membro@email.com", roles = "MEMBER")
    void getDashboardComRoleMemberDeveRedirecionarParaMemberFeed() throws Exception {
        User user = new User();
        user.setId(1L);
        user.setEmail("membro@email.com");
        user.setRole(User.Role.MEMBER);

        List<User> members = new ArrayList<>();
        members.add(user);
        Team team = new Team();
        team.setId(1L);
        team.setMembers(members);

        when(userRepository.findByEmail("membro@email.com")).thenReturn(Optional.of(user));
        when(teamRepository.findByMemberId(1L)).thenReturn(List.of(team));

        mockMvc.perform(get("/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/member/feed"));
    }
}
