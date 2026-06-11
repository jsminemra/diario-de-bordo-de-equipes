package com.diariodebordo.diariobordo.service;

import com.diariodebordo.diariobordo.dto.AddMemberDTO;
import com.diariodebordo.diariobordo.dto.TeamCreateDTO;
import com.diariodebordo.diariobordo.model.Team;
import com.diariodebordo.diariobordo.model.User;
import com.diariodebordo.diariobordo.repository.TeamRepository;
import com.diariodebordo.diariobordo.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class TeamService {

    private final TeamRepository teamRepository;
    private final UserRepository userRepository;

    public TeamService(TeamRepository teamRepository, UserRepository userRepository) {
        this.teamRepository = teamRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public Team createTeam(TeamCreateDTO dto, User leader) {
        // Verificar se o líder já tem uma equipe
        List<Team> existingTeams = teamRepository.findByLeader(leader);
        if (!existingTeams.isEmpty()) {
            throw new RuntimeException("Você já é líder de uma equipe. Apenas uma equipe por líder.");
        }

        Team team = new Team();
        team.setName(dto.getName());
        team.setLeader(leader);
        team.setMembers(new ArrayList<>());
        
        // Adicionar o líder como membro também
        team.getMembers().add(leader);
        
        return teamRepository.save(team);
    }

    @Transactional
    public Team addMember(Long teamId, AddMemberDTO dto, User leader) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new RuntimeException("Equipe não encontrada"));
        
        // Verificar se o usuário atual é o líder da equipe
        if (!team.getLeader().getId().equals(leader.getId())) {
            throw new RuntimeException("Apenas o líder pode adicionar membros");
        }
        
        // Buscar o usuário pelo e-mail
        User member = userRepository.findByEmail(dto.getEmail())
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado com o e-mail: " + dto.getEmail()));
        
        // Verificar se o usuário já pertence a alguma equipe
        if (isUserInAnyTeam(member)) {
            throw new RuntimeException("Usuário " + member.getName() + " já pertence a uma equipe");
        }
        
        // Verificar se já é membro desta equipe
        if (team.getMembers().stream().anyMatch(m -> m.getId().equals(member.getId()))) {
            throw new RuntimeException("Usuário já é membro desta equipe");
        }
        
        team.getMembers().add(member);
        return teamRepository.save(team);
    }
    
    private boolean isUserInAnyTeam(User user) {
        // Verificar em todas as equipes se o usuário é membro
        return teamRepository.findAll().stream()
                .anyMatch(team -> team.getMembers().stream()
                        .anyMatch(member -> member.getId().equals(user.getId())));
    }
    
    public Team getTeamWithMembers(Long teamId) {
        return teamRepository.findById(teamId)
                .orElseThrow(() -> new RuntimeException("Equipe não encontrada"));
    }
    
    public List<Team> getTeamsByLeader(User leader) {
        return teamRepository.findByLeader(leader);
    }
}