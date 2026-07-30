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

        Sprint sprintAtiva = sprintRepository.findByTeamAndStatus(userTeam, Sprint.Status.ATIVA)
                .orElseThrow(() -> new RuntimeException("Nenhuma sprint ativa para sua equipe. Aguarde o líder criar uma nova sprint."));

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

        Sprint sprintAtiva = sprintRepository.findByTeamAndStatus(userTeam, Sprint.Status.ATIVA)
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

        return getMembrosAusentesHoje(user, buscarFeedDoDia(user));
    }

    /**
     * Variante que reaproveita registros já buscados pelo chamador
     * (evita repetir a mesma consulta ao carregar o feed do dia).
     */
    public List<User> getMembrosAusentesHoje(User user, List<DailyEntry> registrosHoje) {
        Team userTeam = getUserTeam(user);
        if (userTeam == null) return List.of();

        Set<Long> idsComRegistro = registrosHoje.stream()
                .map(e -> e.getUser().getId())
                .collect(Collectors.toSet());

        return userTeam.getMembers().stream()
                .filter(m -> !idsComRegistro.contains(m.getId()))
                .collect(Collectors.toList());
    }

    private Team getUserTeam(User user) {
        List<Team> teams = teamRepository.findByMemberId(user.getId());
        if (teams.isEmpty()) {
            return null;
        }
        return teams.get(0);
    }
}