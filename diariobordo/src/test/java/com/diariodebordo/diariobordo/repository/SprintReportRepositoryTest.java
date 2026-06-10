package com.diariodebordo.diariobordo.repository;

import com.diariodebordo.diariobordo.model.Sprint;
import com.diariodebordo.diariobordo.model.SprintReport;
import com.diariodebordo.diariobordo.model.Team;
import com.diariodebordo.diariobordo.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class SprintReportRepositoryTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private SprintReportRepository sprintReportRepository;

    private Sprint sprint;

    @BeforeEach
    void setUp() {
        User leader = new User();
        leader.setEmail("lider@email.com");
        leader.setPassword("senha123");
        leader.setName("Líder");
        leader.setRole(User.Role.LEADER);
        em.persistAndFlush(leader);

        Team team = new Team();
        team.setName("Equipe Delta");
        team.setLeader(leader);
        em.persistAndFlush(team);

        sprint = new Sprint();
        sprint.setName("Sprint Encerrada");
        sprint.setTeam(team);
        sprint.setStartDate(LocalDate.now().minusWeeks(2));
        sprint.setEndDate(LocalDate.now().minusDays(1));
        em.persistAndFlush(sprint);
    }

    @Test
    void findBySprint_deveRetornarRelatorioCorreto() {
        SprintReport report = new SprintReport();
        report.setSprint(sprint);
        report.setSummary("Resumo da sprint");
        report.setTotalEntries(10);
        report.setTotalMembers(5);
        report.setMembersWithEntries(4);
        em.persistAndFlush(report);

        Optional<SprintReport> result = sprintReportRepository.findBySprint(sprint);

        assertThat(result).isPresent();
        assertThat(result.get().getSummary()).isEqualTo("Resumo da sprint");
        assertThat(result.get().getTotalEntries()).isEqualTo(10);
        assertThat(result.get().getMembersWithEntries()).isEqualTo(4);
    }

    @Test
    void findBySprint_deveRetornarVazioParaSprintSemRelatorio() {
        Optional<SprintReport> result = sprintReportRepository.findBySprint(sprint);

        assertThat(result).isEmpty();
    }
}
