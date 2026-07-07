package com.diariodebordo.diariobordo.service;

import com.diariodebordo.diariobordo.dto.DailyEntryDTO;
import com.diariodebordo.diariobordo.model.DailyEntry;
import com.diariodebordo.diariobordo.model.Sprint;
import com.diariodebordo.diariobordo.model.Team;
import com.diariodebordo.diariobordo.model.User;
import com.diariodebordo.diariobordo.repository.DailyEntryRepository;
import com.diariodebordo.diariobordo.repository.SprintRepository;
import com.diariodebordo.diariobordo.repository.TeamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DailyEntryService {

    private final DailyEntryRepository dailyEntryRepository;
    private final SprintRepository sprintRepository;
    private final TeamRepository teamRepository;

    public DailyEntry salvarOuEditar(DailyEntryDTO dto, User user) {
        Team userTeam = getUserTeam(user);
        
        if (userTeam == null) {
            throw new RuntimeException("Usuário não pertence a nenhuma equipe. Aguarde o líder te adicionar.");
        }

        List<Sprint> sprints = sprintRepository.findByTeam(userTeam);
        
        if (sprints.isEmpty()) {
            Sprint novaSprint = new Sprint();
            novaSprint.setName("Sprint 1");
            novaSprint.setTeam(userTeam);
            novaSprint.setStartDate(LocalDate.now());
            novaSprint.setEndDate(LocalDate.now().plusWeeks(2));
            novaSprint.setStatus(Sprint.Status.ATIVA);
            sprintRepository.save(novaSprint);
            System.out.println("✅ Sprint automática criada para a equipe: " + userTeam.getName());
            
            sprints = sprintRepository.findByTeam(userTeam);
        }
        
        if (sprints.isEmpty()) {
            throw new RuntimeException("Nenhuma sprint disponível para sua equipe.");
        }
        
        Sprint sprintAtiva = sprints.stream()
                .max((s1, s2) -> s1.getId().compareTo(s2.getId()))
                .orElseThrow(() -> new RuntimeException("Nenhuma sprint encontrada para sua equipe"));

        Optional<DailyEntry> existente = dailyEntryRepository
                .findByUserAndEntryDate(user, LocalDate.now());

        DailyEntry entry = existente.orElse(new DailyEntry());
        entry.setUser(user);
        entry.setSprint(sprintAtiva);
        entry.setWhatWasDone(dto.getWhatWasDone());
        entry.setWhatWillBeDone(dto.getWhatWillBeDone());
        entry.setImpediments(dto.getImpediments());
        entry.setEntryDate(LocalDate.now());

        DailyEntry saved = dailyEntryRepository.save(entry);
        
        System.out.println("✅ Registro salvo: ID=" + saved.getId() + 
                           ", User=" + saved.getUser().getName() + 
                           ", Equipe=" + userTeam.getName() +
                           ", Sprint=" + saved.getSprint().getName() +
                           ", Data=" + saved.getEntryDate());
        
        return saved;
    }

    public List<DailyEntry> buscarFeedDoDia(User user) {
        Team userTeam = getUserTeam(user);
        
        if (userTeam == null) {
            return List.of();
        }
        
        List<Sprint> sprints = sprintRepository.findByTeam(userTeam);
        
        if (sprints.isEmpty()) {
            return List.of();
        }
        
        Sprint sprintAtiva = sprints.stream()
                .max((s1, s2) -> s1.getId().compareTo(s2.getId()))
                .orElse(null);

        if (sprintAtiva == null) {
            return List.of();
        }

        return dailyEntryRepository
                .findBySprintAndEntryDateOrderByCreatedAtDesc(sprintAtiva, LocalDate.now());
    }

    public Optional<DailyEntry> buscarRegistroDeHoje(User user) {
        return dailyEntryRepository.findByUserAndEntryDate(user, LocalDate.now());
    }

    public List<User> getMembrosAusentesHoje(User user) {
        Team userTeam = getUserTeam(user);
        if (userTeam == null) return List.of();

        List<DailyEntry> registrosHoje = buscarFeedDoDia(user);
        Set<Long> idsComRegistro = registrosHoje.stream()
                .map(e -> e.getUser().getId())
                .collect(Collectors.toSet());

        return userTeam.getMembers().stream()
                .filter(m -> !idsComRegistro.contains(m.getId()))
                .collect(Collectors.toList());
    }

    private Team getUserTeam(User user) {
        List<Team> teams = teamRepository.findAll().stream()
                .filter(t -> t.getMembers().stream().anyMatch(m -> m.getId().equals(user.getId())))
                .toList();
        
        if (teams.isEmpty()) {
            return null;
        }
        return teams.get(0);
    }
}