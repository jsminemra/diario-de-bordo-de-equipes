package com.diariodebordo.diariobordo.service;

import com.diariodebordo.diariobordo.dto.AddMemberDTO;
import com.diariodebordo.diariobordo.dto.TeamCreateDTO;
import com.diariodebordo.diariobordo.model.Team;
import com.diariodebordo.diariobordo.model.User;
import com.diariodebordo.diariobordo.repository.TeamRepository;
import com.diariodebordo.diariobordo.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

@Service
public class TeamService {

    private static final String CODE_CHARSET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"; // sem 0/O e 1/I, evita ambiguidade
    private static final int CODE_LENGTH = 6;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final TeamRepository teamRepository;
    private final UserRepository userRepository;

    public TeamService(TeamRepository teamRepository, UserRepository userRepository) {
        this.teamRepository = teamRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public Team createTeam(TeamCreateDTO dto, User leader) {
        List<Team> existingTeams = teamRepository.findByLeader(leader);
        if (!existingTeams.isEmpty()) {
            throw new RuntimeException("Você já é líder de uma equipe. Apenas uma equipe por líder.");
        }

        Team team = new Team();
        team.setName(dto.getName());
        team.setLeader(leader);
        team.setMembers(new ArrayList<>());
        team.setCode(generateUniqueCode());

        team.getMembers().add(leader);

        return teamRepository.save(team);
    }

    @Transactional
    public Team joinTeamByCode(String rawCode, User user) {
        if (rawCode == null || rawCode.isBlank()) {
            throw new RuntimeException("Informe o código de convite da equipe");
        }
        String code = rawCode.trim().toUpperCase();

        Team team = teamRepository.findByCode(code)
                .orElseThrow(() -> new RuntimeException("Código inválido. Confira com o líder da equipe."));

        if (isUserInAnyTeam(user)) {
            throw new RuntimeException("Você já pertence a uma equipe");
        }

        team.getMembers().add(user);
        return teamRepository.save(team);
    }

    @Transactional
    public Team regenerateCode(Long teamId, User leader) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new RuntimeException("Equipe não encontrada"));

        if (!team.getLeader().getId().equals(leader.getId())) {
            throw new RuntimeException("Apenas o líder pode gerar um novo código");
        }

        team.setCode(generateUniqueCode());
        return teamRepository.save(team);
    }

    private String generateUniqueCode() {
        String code;
        do {
            StringBuilder sb = new StringBuilder(CODE_LENGTH);
            for (int i = 0; i < CODE_LENGTH; i++) {
                sb.append(CODE_CHARSET.charAt(RANDOM.nextInt(CODE_CHARSET.length())));
            }
            code = sb.toString();
        } while (teamRepository.existsByCode(code));
        return code;
    }

    @Transactional
    public Team addMember(Long teamId, AddMemberDTO dto, User leader) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new RuntimeException("Equipe não encontrada"));
        
        if (!team.getLeader().getId().equals(leader.getId())) {
            throw new RuntimeException("Apenas o líder pode adicionar membros");
        }
        
        User member = userRepository.findByEmail(dto.getEmail())
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado com o e-mail: " + dto.getEmail()));

        if (isUserInAnyTeam(member)) {
            throw new RuntimeException("Usuário " + member.getName() + " já pertence a uma equipe");
        }
        
        if (team.getMembers().stream().anyMatch(m -> m.getId().equals(member.getId()))) {
            throw new RuntimeException("Usuário já é membro desta equipe");
        }
        
        team.getMembers().add(member);
        return teamRepository.save(team);
    }
    
    private boolean isUserInAnyTeam(User user) {
        return teamRepository.findAll().stream()
                .anyMatch(team -> team.getMembers().stream()
                        .anyMatch(member -> member.getId().equals(user.getId())));
    }
    
    @Transactional
    public Team getTeamWithMembers(Long teamId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new RuntimeException("Equipe não encontrada"));
        if (team.getCode() == null) {
            team.setCode(generateUniqueCode());
            team = teamRepository.save(team);
        }
        return team;
    }
    
    public List<Team> getTeamsByLeader(User leader) {
        return teamRepository.findByLeader(leader);
    }
}