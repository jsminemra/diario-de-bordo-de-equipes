package com.diariodebordo.diariobordo.repository;

import com.diariodebordo.diariobordo.model.DailyEntry;
import com.diariodebordo.diariobordo.model.Sprint;
import com.diariodebordo.diariobordo.model.Team;
import com.diariodebordo.diariobordo.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class DailyEntryRepositoryTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private DailyEntryRepository dailyEntryRepository;

    private User member;
    private Sprint sprint;

    @BeforeEach
    void setUp() {
        User leader = new User();
        leader.setEmail("lider@email.com");
        leader.setPassword("senha123");
        leader.setName("Líder");
        leader.setRole(User.Role.LEADER);
        em.persistAndFlush(leader);

        member = new User();
        member.setEmail("membro@email.com");
        member.setPassword("senha123");
        member.setName("Membro");
        member.setRole(User.Role.MEMBER);
        em.persistAndFlush(member);

        Team team = new Team();
        team.setName("Equipe Gamma");
        team.setLeader(leader);
        em.persistAndFlush(team);

        sprint = new Sprint();
        sprint.setName("Sprint 1");
        sprint.setTeam(team);
        sprint.setStartDate(LocalDate.now());
        sprint.setEndDate(LocalDate.now().plusWeeks(2));
        em.persistAndFlush(sprint);
    }

    private DailyEntry criarEntrada(User user, Sprint sprint) {
        DailyEntry entry = new DailyEntry();
        entry.setUser(user);
        entry.setSprint(sprint);
        entry.setWhatWasDone("Implementei a feature X");
        entry.setWhatWillBeDone("Vou revisar o PR");
        entry.setImpediments("Nenhum");
        return entry;
    }

    @Test
    void findBySprint_deveRetornarEntradasDaSprint() {
        em.persistAndFlush(criarEntrada(member, sprint));

        List<DailyEntry> result = dailyEntryRepository.findBySprint(sprint);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getWhatWasDone()).isEqualTo("Implementei a feature X");
    }

    @Test
    void findBySprintAndEntryDate_deveRetornarEntradasDaData() {
        DailyEntry entry = criarEntrada(member, sprint);
        em.persistAndFlush(entry);

        LocalDate hoje = LocalDate.now();
        List<DailyEntry> result = dailyEntryRepository.findBySprintAndEntryDate(sprint, hoje);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getEntryDate()).isEqualTo(hoje);
    }

    @Test
    void findByUserAndSprint_deveRetornarEntradasDoUsuarioNaSprint() {
        em.persistAndFlush(criarEntrada(member, sprint));

        List<DailyEntry> result = dailyEntryRepository.findByUserAndSprint(member, sprint);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getUser().getEmail()).isEqualTo("membro@email.com");
        assertThat(result.get(0).getSprint().getName()).isEqualTo("Sprint 1");
    }
}
