package com.diariodebordo.diariobordo.repository;

import com.diariodebordo.diariobordo.model.Team;
import com.diariodebordo.diariobordo.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class TeamRepositoryTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private TeamRepository teamRepository;

    private User leader;

    @BeforeEach
    void setUp() {
        leader = new User();
        leader.setEmail("lider@email.com");
        leader.setPassword("$2a$10$0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopq");
        leader.setName("Líder da Equipe");
        leader.setRole(User.Role.LEADER);
        em.persistAndFlush(leader);
    }

    @Test
    void findByLeader_deveRetornarEquipeDoLider() {
        Team team = new Team();
        team.setName("Equipe Alpha");
        team.setLeader(leader);
        em.persistAndFlush(team);

        List<Team> result = teamRepository.findByLeader(leader);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Equipe Alpha");
        assertThat(result.get(0).getLeader().getEmail()).isEqualTo("lider@email.com");
    }

    @Test
    void findByLeader_deveRetornarListaVaziaQuandoLiderSemEquipes() {
        List<Team> result = teamRepository.findByLeader(leader);

        assertThat(result).isEmpty();
    }
}
