package com.diariodebordo.diariobordo.repository;

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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class SprintRepositoryTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private SprintRepository sprintRepository;

    private Team team;

    @BeforeEach
    void setUp() {
        User leader = new User();
        leader.setEmail("lider@email.com");
        leader.setPassword("senha123");
        leader.setName("Líder");
        leader.setRole(User.Role.LEADER);
        em.persistAndFlush(leader);

        team = new Team();
        team.setName("Equipe Beta");
        team.setLeader(leader);
        em.persistAndFlush(team);
    }

    @Test
    void findByTeam_deveRetornarSprintsDaEquipe() {
        Sprint sprint = new Sprint();
        sprint.setName("Sprint 1");
        sprint.setTeam(team);
        sprint.setStartDate(LocalDate.now());
        sprint.setEndDate(LocalDate.now().plusWeeks(2));
        em.persistAndFlush(sprint);

        List<Sprint> result = sprintRepository.findByTeam(team);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Sprint 1");
        assertThat(result.get(0).getTeam().getName()).isEqualTo("Equipe Beta");
    }

    @Test
    void findByTeamAndStatus_deveRetornarSprintAtiva() {
        Sprint sprint = new Sprint();
        sprint.setName("Sprint Ativa");
        sprint.setTeam(team);
        sprint.setStartDate(LocalDate.now());
        sprint.setEndDate(LocalDate.now().plusWeeks(2));
        em.persistAndFlush(sprint);

        Optional<Sprint> result = sprintRepository.findByTeamAndStatus(team, Sprint.Status.ATIVA);

        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("Sprint Ativa");
        assertThat(result.get().getStatus()).isEqualTo(Sprint.Status.ATIVA);
    }
}
