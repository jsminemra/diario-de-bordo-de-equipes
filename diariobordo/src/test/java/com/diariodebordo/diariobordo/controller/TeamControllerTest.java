package com.diariodebordo.diariobordo.controller;

import com.diariodebordo.diariobordo.config.SecurityConfig;
import com.diariodebordo.diariobordo.dto.TeamCreateDTO;
import com.diariodebordo.diariobordo.model.Team;
import com.diariodebordo.diariobordo.model.User;
import com.diariodebordo.diariobordo.repository.DailyEntryRepository;
import com.diariodebordo.diariobordo.repository.SprintRepository;
import com.diariodebordo.diariobordo.repository.UserRepository;
import com.diariodebordo.diariobordo.service.CustomUserDetailsService;
import com.diariodebordo.diariobordo.service.DailyEntryService;
import com.diariodebordo.diariobordo.service.HistoryService;
import com.diariodebordo.diariobordo.service.PdfExportService;
import com.diariodebordo.diariobordo.service.SprintReportService;
import com.diariodebordo.diariobordo.service.SprintService;
import com.diariodebordo.diariobordo.service.TeamService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(TeamController.class)
@Import(SecurityConfig.class)
@WithMockUser(roles = "LEADER")
class TeamControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TeamService teamService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private DailyEntryService dailyEntryService;

    @MockitoBean
    private SprintRepository sprintRepository;

    @MockitoBean
    private SprintService sprintService;

    @MockitoBean
    private HistoryService historyService;

    @MockitoBean
    private DailyEntryRepository dailyEntryRepository;

    @MockitoBean
    private SprintReportService sprintReportService;

    @MockitoBean
    private PdfExportService pdfExportService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    void getCreateDeveRetornarStatus200EViewCreateTeam() throws Exception {
        mockMvc.perform(get("/leader/team/create"))
                .andExpect(status().isOk())
                .andExpect(view().name("team/create-team"));
    }

    @Test
    @WithMockUser(username = "lider@email.com", roles = "LEADER")
    void postCreateComDadosValidosDeveRedirecionar() throws Exception {
        User leader = new User();
        leader.setId(1L);
        leader.setEmail("lider@email.com");

        Team createdTeam = new Team();
        createdTeam.setId(1L);
        createdTeam.setName("Equipe Alpha");

        when(userRepository.findByEmail("lider@email.com")).thenReturn(Optional.of(leader));
        when(teamService.createTeam(any(TeamCreateDTO.class), any(User.class))).thenReturn(createdTeam);

        mockMvc.perform(post("/leader/team/create")
                        .with(csrf())
                        .param("name", "Equipe Alpha"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/leader/team/1"));
    }

    @Test
    void postCreateComNomeVazioDeveRetornarFormularioComErros() throws Exception {
        mockMvc.perform(post("/leader/team/create")
                        .with(csrf())
                        .param("name", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("team/create-team"))
                .andExpect(model().attributeHasErrors("teamCreateDTO"));
    }
}
