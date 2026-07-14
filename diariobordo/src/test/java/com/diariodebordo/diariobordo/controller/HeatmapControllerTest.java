package com.diariodebordo.diariobordo.controller;

import com.diariodebordo.diariobordo.config.SecurityConfig;
import com.diariodebordo.diariobordo.dto.HeatmapDataDTO;
import com.diariodebordo.diariobordo.model.User;
import com.diariodebordo.diariobordo.repository.UserRepository;
import com.diariodebordo.diariobordo.service.CustomUserDetailsService;
import com.diariodebordo.diariobordo.service.HeatmapService;
import com.diariodebordo.diariobordo.service.HistoryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.ArrayList;
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

@WebMvcTest(HeatmapController.class)
@Import(SecurityConfig.class)
class HeatmapControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private HeatmapService heatmapService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @MockitoBean
    private HistoryService historyService;

    // ─── GET /member/heatmap ─────────────────────────────────────────────────

    @Test
    @WithMockUser(username = "membro@test.com", roles = "MEMBER")
    void getMemberHeatmap_deveRetornarStatus200EViewMemberHeatmap() throws Exception {
        User user = criarUsuario("membro@test.com", User.Role.MEMBER);

        when(userRepository.findByEmail("membro@test.com")).thenReturn(Optional.of(user));
        when(heatmapService.getHeatmapData(eq(user), eq(180))).thenReturn(criarHeatmapData(7));

        mockMvc.perform(get("/member/heatmap"))
                .andExpect(status().isOk())
                .andExpect(view().name("member/heatmap"));
    }

    @Test
    @WithMockUser(username = "membro@test.com", roles = "MEMBER")
    void getMemberHeatmap_devePassarAtributosEstatisticosAoModelo() throws Exception {
        User user = criarUsuario("membro@test.com", User.Role.MEMBER);
        List<HeatmapDataDTO> heatmapData = criarHeatmapData(7);

        when(userRepository.findByEmail("membro@test.com")).thenReturn(Optional.of(user));
        when(heatmapService.getHeatmapData(eq(user), eq(180))).thenReturn(heatmapData);

        mockMvc.perform(get("/member/heatmap"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("heatmapData"))
                .andExpect(model().attributeExists("weeks"))
                .andExpect(model().attributeExists("user"))
                .andExpect(model().attribute("days", 180))
                .andExpect(model().attributeExists("diasComRegistro"))
                .andExpect(model().attributeExists("percentual"))
                .andExpect(model().attributeExists("totalRegistros"));
    }

    @Test
    @WithMockUser(username = "membro@test.com", roles = "MEMBER")
    void getMemberHeatmap_deveUsarJanelaDe180Dias() throws Exception {
        User user = criarUsuario("membro@test.com", User.Role.MEMBER);

        when(userRepository.findByEmail("membro@test.com")).thenReturn(Optional.of(user));
        when(heatmapService.getHeatmapData(eq(user), eq(180))).thenReturn(criarHeatmapData(7));

        mockMvc.perform(get("/member/heatmap"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("days", 180));
    }

    @Test
    @WithMockUser(username = "membro@test.com", roles = "MEMBER")
    void getMemberHeatmap_devePassarRoleMemberAoModelo() throws Exception {
        User user = criarUsuario("membro@test.com", User.Role.MEMBER);

        when(userRepository.findByEmail("membro@test.com")).thenReturn(Optional.of(user));
        when(heatmapService.getHeatmapData(any(), eq(180))).thenReturn(criarHeatmapData(7));

        mockMvc.perform(get("/member/heatmap"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("role", "member"));
    }

    @Test
    void getMemberHeatmap_semAutenticacao_deveRedirecionar() throws Exception {
        mockMvc.perform(get("/member/heatmap"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    // ─── GET /leader/heatmap ─────────────────────────────────────────────────

    @Test
    @WithMockUser(username = "lider@test.com", roles = "LEADER")
    void getLeaderHeatmap_deveRetornarStatus200EViewLeaderHeatmap() throws Exception {
        User user = criarUsuario("lider@test.com", User.Role.LEADER);

        when(userRepository.findByEmail("lider@test.com")).thenReturn(Optional.of(user));
        when(heatmapService.getHeatmapData(eq(user), eq(180))).thenReturn(criarHeatmapData(7));

        mockMvc.perform(get("/leader/heatmap"))
                .andExpect(status().isOk())
                .andExpect(view().name("leader/heatmap"));
    }

    @Test
    @WithMockUser(username = "lider@test.com", roles = "LEADER")
    void getLeaderHeatmap_devePassarRoleLeaderAoModelo() throws Exception {
        User user = criarUsuario("lider@test.com", User.Role.LEADER);

        when(userRepository.findByEmail("lider@test.com")).thenReturn(Optional.of(user));
        when(heatmapService.getHeatmapData(any(), eq(180))).thenReturn(criarHeatmapData(7));

        mockMvc.perform(get("/leader/heatmap"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("role", "leader"));
    }

    @Test
    @WithMockUser(username = "membro@test.com", roles = "MEMBER")
    void getLeaderHeatmap_comRoleMember_deveRetornar403() throws Exception {
        mockMvc.perform(get("/leader/heatmap"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getLeaderHeatmap_semAutenticacao_deveRedirecionar() throws Exception {
        mockMvc.perform(get("/leader/heatmap"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    // ─── helpers ────────────────────────────────────────────────────────────

    private User criarUsuario(String email, User.Role role) {
        User u = new User();
        u.setId(1L);
        u.setEmail(email);
        u.setName("Usuário Teste");
        u.setRole(role);
        return u;
    }

    private List<HeatmapDataDTO> criarHeatmapData(int dias) {
        List<HeatmapDataDTO> data = new ArrayList<>();
        LocalDate hoje = LocalDate.now();
        for (int i = dias; i >= 0; i--) {
            HeatmapDataDTO dto = new HeatmapDataDTO();
            dto.setDate(hoje.minusDays(i));
            dto.setCount(0);
            dto.setSummary("Nenhum registro");
            dto.setType("none");
            data.add(dto);
        }
        return data;
    }
}
