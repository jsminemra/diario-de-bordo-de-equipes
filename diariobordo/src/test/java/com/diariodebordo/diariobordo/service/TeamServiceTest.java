package com.diariodebordo.diariobordo.service;

import com.diariodebordo.diariobordo.dto.AddMemberDTO;
import com.diariodebordo.diariobordo.dto.TeamCreateDTO;
import com.diariodebordo.diariobordo.model.Team;
import com.diariodebordo.diariobordo.model.User;
import com.diariodebordo.diariobordo.repository.TeamRepository;
import com.diariodebordo.diariobordo.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TeamServiceTest {

    @Mock
    private TeamRepository teamRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private TeamService teamService;

    @Test
    void deveCriarEquipeComSucesso() {
        User leader = new User();
        leader.setId(1L);
        leader.setName("Líder");
        leader.setEmail("lider@email.com");

        TeamCreateDTO dto = new TeamCreateDTO();
        dto.setName("Equipe Alpha");

        Team savedTeam = new Team();
        savedTeam.setId(1L);
        savedTeam.setName("Equipe Alpha");
        savedTeam.setLeader(leader);
        savedTeam.setMembers(new ArrayList<>(List.of(leader)));

        when(teamRepository.findByLeader(leader)).thenReturn(Collections.emptyList());
        when(teamRepository.save(any(Team.class))).thenReturn(savedTeam);

        Team result = teamService.createTeam(dto, leader);

        assertThat(result.getName()).isEqualTo("Equipe Alpha");
        assertThat(result.getLeader()).isEqualTo(leader);
        assertThat(result.getMembers()).contains(leader);
        verify(teamRepository).save(any(Team.class));
    }

    @Test
    void deveLancarExcecaoQuandoLiderJaTemEquipe() {
        User leader = new User();
        leader.setId(1L);

        when(teamRepository.findByLeader(leader)).thenReturn(List.of(new Team()));

        TeamCreateDTO dto = new TeamCreateDTO();
        dto.setName("Nova Equipe");

        assertThatThrownBy(() -> teamService.createTeam(dto, leader))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("já é líder");
    }

    @Test
    void deveAdicionarMembroComSucesso() {
        User leader = new User();
        leader.setId(1L);

        User newMember = new User();
        newMember.setId(2L);
        newMember.setEmail("novo@email.com");
        newMember.setName("Novo Membro");

        Team team = new Team();
        team.setId(1L);
        team.setLeader(leader);
        team.setMembers(new ArrayList<>(List.of(leader)));

        AddMemberDTO dto = new AddMemberDTO();
        dto.setEmail("novo@email.com");

        // findAll retorna o team que só tem o líder — newMember ainda não está em nenhuma equipe
        when(teamRepository.findById(1L)).thenReturn(Optional.of(team));
        when(userRepository.findByEmail("novo@email.com")).thenReturn(Optional.of(newMember));
        when(teamRepository.findAll()).thenReturn(List.of(team));
        when(teamRepository.save(any(Team.class))).thenReturn(team);

        Team result = teamService.addMember(1L, dto, leader);

        assertThat(result.getMembers()).contains(newMember);
        verify(teamRepository).save(any(Team.class));
    }

    @Test
    void deveLancarExcecaoQuandoMembroNaoEncontradoPorEmail() {
        User leader = new User();
        leader.setId(1L);

        Team team = new Team();
        team.setId(1L);
        team.setLeader(leader);
        team.setMembers(new ArrayList<>());

        AddMemberDTO dto = new AddMemberDTO();
        dto.setEmail("inexistente@email.com");

        when(teamRepository.findById(1L)).thenReturn(Optional.of(team));
        when(userRepository.findByEmail("inexistente@email.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> teamService.addMember(1L, dto, leader))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("inexistente@email.com");
    }

    @Test
    void deveLancarExcecaoQuandoUsuarioJaEMembro() {
        User leader = new User();
        leader.setId(1L);

        User existingMember = new User();
        existingMember.setId(2L);
        existingMember.setEmail("membro@email.com");
        existingMember.setName("Membro Existente");

        Team team = new Team();
        team.setId(1L);
        team.setLeader(leader);
        // existingMember já está na lista de membros da equipe
        team.setMembers(new ArrayList<>(List.of(leader, existingMember)));

        AddMemberDTO dto = new AddMemberDTO();
        dto.setEmail("membro@email.com");

        when(teamRepository.findById(1L)).thenReturn(Optional.of(team));
        when(userRepository.findByEmail("membro@email.com")).thenReturn(Optional.of(existingMember));
        // findAll retorna lista vazia para que isUserInAnyTeam retorne false e o segundo
        // check ("já é membro desta equipe") seja exercitado
        when(teamRepository.findAll()).thenReturn(Collections.emptyList());

        assertThatThrownBy(() -> teamService.addMember(1L, dto, leader))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("já é membro desta equipe");
    }
}
