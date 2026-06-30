package com.diariodebordo.diariobordo.service;

import com.diariodebordo.diariobordo.dto.HeatmapDataDTO;
import com.diariodebordo.diariobordo.model.DailyEntry;
import com.diariodebordo.diariobordo.model.Sprint;
import com.diariodebordo.diariobordo.model.Team;
import com.diariodebordo.diariobordo.model.User;
import com.diariodebordo.diariobordo.repository.DailyEntryRepository;
import com.diariodebordo.diariobordo.repository.SprintRepository;
import com.diariodebordo.diariobordo.repository.TeamRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class HeatmapServiceTest {

    @Mock
    private DailyEntryRepository dailyEntryRepository;

    @Mock
    private SprintRepository sprintRepository;

    @Mock
    private TeamRepository teamRepository;

    @InjectMocks
    private HeatmapService heatmapService;

    // ─── helpers ────────────────────────────────────────────────────────────

    private User criarUsuario(Long id, String email) {
        User u = new User();
        u.setId(id);
        u.setEmail(email);
        u.setName("Usuário " + id);
        u.setRole(User.Role.MEMBER);
        return u;
    }

    private Team criarEquipe(Long id, List<User> membros) {
        Team t = new Team();
        t.setId(id);
        t.setName("Equipe " + id);
        t.setMembers(new ArrayList<>(membros));
        return t;
    }

    private Sprint criarSprint(Long id, Team team) {
        Sprint s = new Sprint();
        s.setId(id);
        s.setName("Sprint " + id);
        s.setTeam(team);
        s.setStartDate(LocalDate.now().minusWeeks(1));
        s.setEndDate(LocalDate.now().plusWeeks(1));
        s.setStatus(Sprint.Status.ATIVA);
        return s;
    }

    private DailyEntry criarEntrada(Long id, User user, Sprint sprint, LocalDate data, String descricao) {
        DailyEntry e = new DailyEntry();
        e.setId(id);
        e.setUser(user);
        e.setSprint(sprint);
        e.setWhatWasDone(descricao);
        e.setWhatWillBeDone("Vou revisar o PR");
        e.setImpediments("Nenhum");
        e.setEntryDate(data);
        return e;
    }

    // ─── getHeatmapData ──────────────────────────────────────────────────────

    @Test
    void deveRetornarUmElementoPorDiaNoIntervalo() {
        User user = criarUsuario(1L, "user@test.com");
        Team team = criarEquipe(1L, List.of(user));
        Sprint sprint = criarSprint(1L, team);

        when(teamRepository.findAll()).thenReturn(List.of(team));
        when(sprintRepository.findByTeam(team)).thenReturn(List.of(sprint));
        when(dailyEntryRepository.findByUserAndSprint(user, sprint)).thenReturn(List.of());

        List<HeatmapDataDTO> resultado = heatmapService.getHeatmapData(user, 7);

        // startDate = hoje - 7, loop inclusive até hoje → 8 dias
        assertThat(resultado).hasSize(8);
    }

    @Test
    void deveMarcarDiasComRegistroComCount1() {
        User user = criarUsuario(1L, "user@test.com");
        Team team = criarEquipe(1L, List.of(user));
        Sprint sprint = criarSprint(1L, team);

        LocalDate hoje = LocalDate.now();
        LocalDate dataRegistro = hoje.minusDays(2);
        DailyEntry entry = criarEntrada(1L, user, sprint, dataRegistro, "Implementei a feature");

        when(teamRepository.findAll()).thenReturn(List.of(team));
        when(sprintRepository.findByTeam(team)).thenReturn(List.of(sprint));
        when(dailyEntryRepository.findByUserAndSprint(user, sprint)).thenReturn(List.of(entry));

        List<HeatmapDataDTO> resultado = heatmapService.getHeatmapData(user, 7);

        HeatmapDataDTO diaComRegistro = resultado.stream()
                .filter(d -> d.getDate().equals(dataRegistro))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Data de registro não encontrada no heatmap"));

        assertThat(diaComRegistro.getCount()).isEqualTo(1);
        assertThat(diaComRegistro.getSummary()).contains("Implementei");

        long diasSemRegistro = resultado.stream().filter(d -> d.getCount() == 0).count();
        assertThat(diasSemRegistro).isEqualTo(7);
    }

    @Test
    void deveRetornarHeatmapVazioQuandoUsuarioNaoPertenceANenhumaEquipe() {
        User user = criarUsuario(1L, "sem-equipe@test.com");

        when(teamRepository.findAll()).thenReturn(List.of());

        List<HeatmapDataDTO> resultado = heatmapService.getHeatmapData(user, 7);

        assertThat(resultado).hasSize(8);
        assertThat(resultado).allMatch(d -> d.getCount() == 0);
    }

    @Test
    void deveRetornarHeatmapVazioQuandoEquipeSemSprints() {
        User user = criarUsuario(1L, "user@test.com");
        Team team = criarEquipe(1L, List.of(user));

        when(teamRepository.findAll()).thenReturn(List.of(team));
        when(sprintRepository.findByTeam(team)).thenReturn(List.of());

        List<HeatmapDataDTO> resultado = heatmapService.getHeatmapData(user, 7);

        assertThat(resultado).hasSize(8);
        assertThat(resultado).allMatch(d -> d.getCount() == 0);
    }

    @Test
    void deveTruncarSummaryQuandoDescricaoUltrapassaTrintaCaracteres() {
        User user = criarUsuario(1L, "user@test.com");
        Team team = criarEquipe(1L, List.of(user));
        Sprint sprint = criarSprint(1L, team);

        LocalDate hoje = LocalDate.now();
        String descricaoLonga = "Esta é uma descrição muito longa que definitivamente ultrapassa 30 caracteres";
        DailyEntry entry = criarEntrada(1L, user, sprint, hoje, descricaoLonga);

        when(teamRepository.findAll()).thenReturn(List.of(team));
        when(sprintRepository.findByTeam(team)).thenReturn(List.of(sprint));
        when(dailyEntryRepository.findByUserAndSprint(user, sprint)).thenReturn(List.of(entry));

        List<HeatmapDataDTO> resultado = heatmapService.getHeatmapData(user, 7);

        HeatmapDataDTO hoje_dto = resultado.stream()
                .filter(d -> d.getDate().equals(hoje))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Hoje não encontrado no heatmap"));

        assertThat(hoje_dto.getSummary()).endsWith("...");
        // 30 chars + "..." = 33
        assertThat(hoje_dto.getSummary().length()).isLessThanOrEqualTo(33);
    }

    @Test
    void naoDeveExibirRegistrosDeOutroUsuario() {
        User user1 = criarUsuario(1L, "user1@test.com");
        User user2 = criarUsuario(2L, "user2@test.com");
        Team team = criarEquipe(1L, List.of(user1, user2));
        Sprint sprint = criarSprint(1L, team);

        // user2 tem registro hoje, mas o heatmap é de user1 que não tem
        when(teamRepository.findAll()).thenReturn(List.of(team));
        when(sprintRepository.findByTeam(team)).thenReturn(List.of(sprint));
        when(dailyEntryRepository.findByUserAndSprint(user1, sprint)).thenReturn(List.of());

        List<HeatmapDataDTO> resultado = heatmapService.getHeatmapData(user1, 7);

        assertThat(resultado).allMatch(d -> d.getCount() == 0);
        // Garante que os registros de user2 nunca foram consultados
        verify(dailyEntryRepository, never()).findByUserAndSprint(eq(user2), any());
    }

    @Test
    void todosOsDiasDevemTerType_none_QuandoSemRegistros() {
        User user = criarUsuario(1L, "user@test.com");
        Team team = criarEquipe(1L, List.of(user));
        Sprint sprint = criarSprint(1L, team);

        when(teamRepository.findAll()).thenReturn(List.of(team));
        when(sprintRepository.findByTeam(team)).thenReturn(List.of(sprint));
        when(dailyEntryRepository.findByUserAndSprint(user, sprint)).thenReturn(List.of());

        List<HeatmapDataDTO> resultado = heatmapService.getHeatmapData(user, 7);

        assertThat(resultado).allMatch(d -> "none".equals(d.getType()));
    }

    @Test
    void diaComRegistroDeveTerminarComType_entry() {
        User user = criarUsuario(1L, "user@test.com");
        Team team = criarEquipe(1L, List.of(user));
        Sprint sprint = criarSprint(1L, team);

        LocalDate ontem = LocalDate.now().minusDays(1);
        DailyEntry entry = criarEntrada(1L, user, sprint, ontem, "Feature implementada");

        when(teamRepository.findAll()).thenReturn(List.of(team));
        when(sprintRepository.findByTeam(team)).thenReturn(List.of(sprint));
        when(dailyEntryRepository.findByUserAndSprint(user, sprint)).thenReturn(List.of(entry));

        List<HeatmapDataDTO> resultado = heatmapService.getHeatmapData(user, 7);

        HeatmapDataDTO ontem_dto = resultado.stream()
                .filter(d -> d.getDate().equals(ontem))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Ontem não encontrado no heatmap"));

        assertThat(ontem_dto.getType()).isEqualTo("entry");
    }
}
