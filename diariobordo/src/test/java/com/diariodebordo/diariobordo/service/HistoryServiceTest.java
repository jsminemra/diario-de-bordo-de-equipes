package com.diariodebordo.diariobordo.service;

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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HistoryServiceTest {

    @Mock
    private DailyEntryRepository dailyEntryRepository;

    @Mock
    private SprintRepository sprintRepository;

    @Mock
    private TeamRepository teamRepository;

    @InjectMocks
    private HistoryService historyService;

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

    private Sprint criarSprint(Long id, Team team, Sprint.Status status) {
        Sprint s = new Sprint();
        s.setId(id);
        s.setName("Sprint " + id);
        s.setTeam(team);
        s.setStartDate(LocalDate.now().minusWeeks(1));
        s.setEndDate(LocalDate.now().plusWeeks(1));
        s.setStatus(status);
        return s;
    }

    private DailyEntry criarEntrada(Long id, User user, Sprint sprint, LocalDate data) {
        DailyEntry e = new DailyEntry();
        e.setId(id);
        e.setUser(user);
        e.setSprint(sprint);
        e.setWhatWasDone("Implementei a feature " + id);
        e.setWhatWillBeDone("Vou revisar o PR");
        e.setImpediments("Nenhum");
        e.setEntryDate(data);
        return e;
    }

    // ─── getUserHistory ──────────────────────────────────────────────────────

    @Test
    void deveRetornarRegistrosDoUsuarioOrdenadosPorDataDecrescente() {
        User user = criarUsuario(1L, "user@test.com");
        Team team = criarEquipe(1L, List.of(user));
        Sprint sprint = criarSprint(1L, team, Sprint.Status.ATIVA);

        LocalDate hoje = LocalDate.now();
        DailyEntry e1 = criarEntrada(1L, user, sprint, hoje.minusDays(5));
        DailyEntry e2 = criarEntrada(2L, user, sprint, hoje.minusDays(1));
        DailyEntry e3 = criarEntrada(3L, user, sprint, hoje.minusDays(3));

        when(teamRepository.findByMemberId(user.getId())).thenReturn(List.of(team));
        when(sprintRepository.findByTeam(team)).thenReturn(List.of(sprint));
        when(dailyEntryRepository.findByUserAndSprint(user, sprint))
                .thenReturn(List.of(e1, e2, e3));

        List<DailyEntry> resultado = historyService.getUserHistory(user, null, null);

        assertThat(resultado).hasSize(3);
        assertThat(resultado.get(0).getEntryDate()).isEqualTo(hoje.minusDays(1));
        assertThat(resultado.get(1).getEntryDate()).isEqualTo(hoje.minusDays(3));
        assertThat(resultado.get(2).getEntryDate()).isEqualTo(hoje.minusDays(5));
    }

    @Test
    void deveRetornarListaVaziaQuandoUsuarioSemEquipe() {
        User user = criarUsuario(1L, "sem-equipe@test.com");

        when(teamRepository.findByMemberId(user.getId())).thenReturn(List.of());

        List<DailyEntry> resultado = historyService.getUserHistory(user, null, null);

        assertThat(resultado).isEmpty();
    }

    @Test
    void deveAplicarFiltroDeDataCorretamente() {
        User user = criarUsuario(1L, "user@test.com");
        Team team = criarEquipe(1L, List.of(user));
        Sprint sprint = criarSprint(1L, team, Sprint.Status.ATIVA);

        LocalDate hoje = LocalDate.now();
        LocalDate inicio = hoje.minusDays(10);
        LocalDate fim = hoje.minusDays(5);

        DailyEntry dentroRange = criarEntrada(1L, user, sprint, hoje.minusDays(7));
        DailyEntry antesRange  = criarEntrada(2L, user, sprint, hoje.minusDays(15));
        DailyEntry depoisRange = criarEntrada(3L, user, sprint, hoje.minusDays(2));

        when(teamRepository.findByMemberId(user.getId())).thenReturn(List.of(team));
        when(sprintRepository.findByTeam(team)).thenReturn(List.of(sprint));
        when(dailyEntryRepository.findByUserAndSprint(user, sprint))
                .thenReturn(List.of(dentroRange, antesRange, depoisRange));

        List<DailyEntry> resultado = historyService.getUserHistory(user, inicio, fim);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getEntryDate()).isEqualTo(hoje.minusDays(7));
    }

    @Test
    void naoDeveRetornarRegistrosDeOutrosUsuarios() {
        User user1 = criarUsuario(1L, "user1@test.com");
        User user2 = criarUsuario(2L, "user2@test.com");
        Team team = criarEquipe(1L, List.of(user1, user2));
        Sprint sprint = criarSprint(1L, team, Sprint.Status.ATIVA);

        LocalDate hoje = LocalDate.now();
        // Repositório retorna apenas entradas de user1 quando consultado com user1
        DailyEntry entradaUser1 = criarEntrada(1L, user1, sprint, hoje);

        when(teamRepository.findByMemberId(user1.getId())).thenReturn(List.of(team));
        when(sprintRepository.findByTeam(team)).thenReturn(List.of(sprint));
        when(dailyEntryRepository.findByUserAndSprint(user1, sprint))
                .thenReturn(List.of(entradaUser1));

        List<DailyEntry> resultado = historyService.getUserHistory(user1, null, null);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getUser().getId()).isEqualTo(user1.getId());
    }

    @Test
    void deveUsarJanelaDe30DiasComoDefaultQuandoFiltroNulo() {
        User user = criarUsuario(1L, "user@test.com");
        Team team = criarEquipe(1L, List.of(user));
        Sprint sprint = criarSprint(1L, team, Sprint.Status.ATIVA);

        LocalDate hoje = LocalDate.now();
        DailyEntry recente   = criarEntrada(1L, user, sprint, hoje.minusDays(10));
        DailyEntry antigo    = criarEntrada(2L, user, sprint, hoje.minusDays(35));

        when(teamRepository.findByMemberId(user.getId())).thenReturn(List.of(team));
        when(sprintRepository.findByTeam(team)).thenReturn(List.of(sprint));
        when(dailyEntryRepository.findByUserAndSprint(user, sprint))
                .thenReturn(List.of(recente, antigo));

        List<DailyEntry> resultado = historyService.getUserHistory(user, null, null);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getEntryDate()).isEqualTo(hoje.minusDays(10));
    }

    // ─── getSprintAtiva ──────────────────────────────────────────────────────

    @Test
    void getSprintAtiva_deveRetornarSprintAtivaDaEquipeDoUsuario() {
        User user = criarUsuario(1L, "user@test.com");
        Team team = criarEquipe(1L, List.of(user));
        Sprint sprintAtiva = criarSprint(1L, team, Sprint.Status.ATIVA);

        when(teamRepository.findByMemberId(user.getId())).thenReturn(List.of(team));
        when(sprintRepository.findByTeamAndStatus(team, Sprint.Status.ATIVA))
                .thenReturn(Optional.of(sprintAtiva));

        Sprint resultado = historyService.getSprintAtiva(user);

        assertThat(resultado).isNotNull();
        assertThat(resultado.getStatus()).isEqualTo(Sprint.Status.ATIVA);
        assertThat(resultado.getName()).isEqualTo("Sprint 1");
    }

    @Test
    void getSprintAtiva_deveRetornarNullQuandoNaoHaSprintAtiva() {
        User user = criarUsuario(1L, "user@test.com");
        Team team = criarEquipe(1L, List.of(user));

        when(teamRepository.findByMemberId(user.getId())).thenReturn(List.of(team));
        when(sprintRepository.findByTeamAndStatus(team, Sprint.Status.ATIVA))
                .thenReturn(Optional.empty());

        Sprint resultado = historyService.getSprintAtiva(user);

        assertThat(resultado).isNull();
    }

    @Test
    void getSprintAtiva_deveRetornarNullQuandoUsuarioSemEquipe() {
        User user = criarUsuario(1L, "sem-equipe@test.com");

        when(teamRepository.findByMemberId(user.getId())).thenReturn(List.of());

        Sprint resultado = historyService.getSprintAtiva(user);

        assertThat(resultado).isNull();
    }
}
