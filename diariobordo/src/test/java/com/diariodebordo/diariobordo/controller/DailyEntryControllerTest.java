package com.diariodebordo.diariobordo.controller;

import com.diariodebordo.diariobordo.config.SecurityConfig;
import com.diariodebordo.diariobordo.model.DailyEntry;
import com.diariodebordo.diariobordo.model.Sprint;
import com.diariodebordo.diariobordo.model.User;
import com.diariodebordo.diariobordo.repository.UserRepository;
import com.diariodebordo.diariobordo.service.CustomUserDetailsService;
import com.diariodebordo.diariobordo.service.DailyEntryService;
import com.diariodebordo.diariobordo.service.HistoryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(DailyEntryController.class)
@Import(SecurityConfig.class)
class DailyEntryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DailyEntryService dailyEntryService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private HistoryService historyService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    // ─── GET /member/history ─────────────────────────────────────────────────

    @Test
    @WithMockUser(username = "membro@test.com", roles = "MEMBER")
    void getHistory_deveRetornarStatus200EViewHistory() throws Exception {
        User user = criarUsuario("membro@test.com");

        when(userRepository.findByEmail("membro@test.com")).thenReturn(Optional.of(user));
        when(historyService.getSprintAtiva(user)).thenReturn(null);
        when(historyService.getUserHistory(eq(user), any(), any())).thenReturn(List.of());

        mockMvc.perform(get("/member/history"))
                .andExpect(status().isOk())
                .andExpect(view().name("member/history"));
    }

    @Test
    @WithMockUser(username = "membro@test.com", roles = "MEMBER")
    void getHistory_devePassarHistoryEUsuarioAoModelo() throws Exception {
        User user = criarUsuario("membro@test.com");
        DailyEntry entry = criarEntrada(user);
        Sprint sprint = criarSprintAtiva();

        when(userRepository.findByEmail("membro@test.com")).thenReturn(Optional.of(user));
        when(historyService.getSprintAtiva(user)).thenReturn(sprint);
        when(historyService.getUserHistory(eq(user), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of(entry));

        mockMvc.perform(get("/member/history"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("history"))
                .andExpect(model().attributeExists("usuario"))
                .andExpect(model().attributeExists("sprintAtiva"))
                .andExpect(model().attributeExists("startDate"))
                .andExpect(model().attributeExists("endDate"));
    }

    @Test
    @WithMockUser(username = "membro@test.com", roles = "MEMBER")
    void getHistory_comFiltroDeData_deveUsarDatasInformadasComoParametros() throws Exception {
        User user = criarUsuario("membro@test.com");

        when(userRepository.findByEmail("membro@test.com")).thenReturn(Optional.of(user));
        when(historyService.getSprintAtiva(user)).thenReturn(null);
        when(historyService.getUserHistory(eq(user), any(), any())).thenReturn(List.of());

        mockMvc.perform(get("/member/history")
                        .param("startDate", "2026-06-01")
                        .param("endDate", "2026-06-15"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("startDate", LocalDate.of(2026, 6, 1)))
                .andExpect(model().attribute("endDate", LocalDate.of(2026, 6, 15)));
    }

    @Test
    void getHistory_semAutenticacao_deveRedirecionar() throws Exception {
        mockMvc.perform(get("/member/history"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @WithMockUser(username = "membro@test.com", roles = "MEMBER")
    void getHistory_semSprintAtiva_deveUsarJanelaDe30DiasComoDefault() throws Exception {
        User user = criarUsuario("membro@test.com");

        when(userRepository.findByEmail("membro@test.com")).thenReturn(Optional.of(user));
        when(historyService.getSprintAtiva(user)).thenReturn(null);
        when(historyService.getUserHistory(eq(user), any(), any())).thenReturn(List.of());

        mockMvc.perform(get("/member/history"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("startDate", LocalDate.now().minusDays(30)))
                .andExpect(model().attribute("endDate", LocalDate.now()));
    }

    @Test
    @WithMockUser(username = "membro@test.com", roles = "MEMBER")
    void getHistory_comSprintAtiva_deveUsarDatasDeSprintComoDefault() throws Exception {
        User user = criarUsuario("membro@test.com");
        Sprint sprint = criarSprintAtiva();

        when(userRepository.findByEmail("membro@test.com")).thenReturn(Optional.of(user));
        when(historyService.getSprintAtiva(user)).thenReturn(sprint);
        when(historyService.getUserHistory(eq(user), any(), any())).thenReturn(List.of());

        mockMvc.perform(get("/member/history"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("startDate", sprint.getStartDate()))
                .andExpect(model().attribute("endDate", sprint.getEndDate()));
    }

    // ─── GET /member/feed ────────────────────────────────────────────────────

    @Test
    @WithMockUser(username = "membro@test.com", roles = "MEMBER")
    void getFeed_devePassarMembrosAusentesAoModelo() throws Exception {
        User user = criarUsuario("membro@test.com");

        when(userRepository.findByEmail("membro@test.com")).thenReturn(Optional.of(user));
        when(dailyEntryService.buscarFeedDoDia(user)).thenReturn(List.of());
        when(dailyEntryService.buscarRegistroDeHoje(user)).thenReturn(Optional.empty());
        when(dailyEntryService.getMembrosAusentesHoje(user)).thenReturn(List.of());

        mockMvc.perform(get("/member/feed"))
                .andExpect(status().isOk())
                .andExpect(view().name("member/feed"))
                .andExpect(model().attributeExists("membrosAusentes"));
    }

    @Test
    @WithMockUser(username = "membro@test.com", roles = "MEMBER")
    void getFeed_devePassarEntryHojeAoModelo() throws Exception {
        User user = criarUsuario("membro@test.com");
        DailyEntry entryHoje = criarEntrada(user);

        when(userRepository.findByEmail("membro@test.com")).thenReturn(Optional.of(user));
        when(dailyEntryService.buscarFeedDoDia(user)).thenReturn(List.of(entryHoje));
        when(dailyEntryService.buscarRegistroDeHoje(user)).thenReturn(Optional.of(entryHoje));
        when(dailyEntryService.getMembrosAusentesHoje(user)).thenReturn(List.of());

        mockMvc.perform(get("/member/feed"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("jaRegistrou", true))
                .andExpect(model().attribute("entryHoje", entryHoje));
    }

    // ─── helpers ────────────────────────────────────────────────────────────

    private User criarUsuario(String email) {
        User u = new User();
        u.setId(1L);
        u.setEmail(email);
        u.setName("Membro Teste");
        u.setRole(User.Role.MEMBER);
        return u;
    }

    private DailyEntry criarEntrada(User user) {
        DailyEntry e = new DailyEntry();
        e.setId(1L);
        e.setUser(user);
        e.setWhatWasDone("Implementei a feature");
        e.setWhatWillBeDone("Vou revisar o PR");
        e.setImpediments("Nenhum");
        e.setEntryDate(LocalDate.now());
        return e;
    }

    private Sprint criarSprintAtiva() {
        Sprint s = new Sprint();
        s.setId(1L);
        s.setName("Sprint 1");
        s.setStartDate(LocalDate.of(2026, 6, 16));
        s.setEndDate(LocalDate.of(2026, 6, 30));
        s.setStatus(Sprint.Status.ATIVA);
        return s;
    }
}
