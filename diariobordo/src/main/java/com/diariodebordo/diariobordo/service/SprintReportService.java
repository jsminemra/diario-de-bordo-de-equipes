package com.diariodebordo.diariobordo.service;

import com.diariodebordo.diariobordo.model.DailyEntry;
import com.diariodebordo.diariobordo.model.Sprint;
import com.diariodebordo.diariobordo.model.SprintReport;
import com.diariodebordo.diariobordo.model.Team;
import com.diariodebordo.diariobordo.repository.DailyEntryRepository;
import com.diariodebordo.diariobordo.repository.SprintReportRepository;
import com.diariodebordo.diariobordo.repository.SprintRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Service
public class SprintReportService {

    private static final Logger log = LoggerFactory.getLogger(SprintReportService.class);
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final SprintReportRepository sprintReportRepository;
    private final DailyEntryRepository dailyEntryRepository;
    private final SprintRepository sprintRepository;

    public SprintReportService(SprintReportRepository sprintReportRepository,
                               DailyEntryRepository dailyEntryRepository,
                               SprintRepository sprintRepository) {
        this.sprintReportRepository = sprintReportRepository;
        this.dailyEntryRepository = dailyEntryRepository;
        this.sprintRepository = sprintRepository;
    }

    @Transactional
    public SprintReport generateAndSave(Sprint sprint, Team team) {
        Optional<SprintReport> existing = sprintReportRepository.findBySprint(sprint);
        if (existing.isPresent()) {
            return existing.get();
        }

        List<DailyEntry> entries = dailyEntryRepository.findBySprint(sprint);

        long membersWithEntries = entries.stream()
                .map(e -> e.getUser().getId())
                .distinct()
                .count();

        int totalMembers = team.getMembers().size();
        int totalEntries = entries.size();

        String summary = String.format(
                "Sprint %s encerrada. Período: %s → %s. " +
                "%d registros de %d/%d membros.",
                sprint.getName(),
                sprint.getStartDate().format(FMT),
                sprint.getEndDate().format(FMT),
                totalEntries,
                membersWithEntries,
                totalMembers
        );

        SprintReport report = new SprintReport();
        report.setSprint(sprint);
        report.setTotalEntries(totalEntries);
        report.setTotalMembers(totalMembers);
        report.setMembersWithEntries((int) membersWithEntries);
        report.setSummary(summary);

        return sprintReportRepository.save(report);
    }

    public Optional<SprintReport> findBySprint(Sprint sprint) {
        return sprintReportRepository.findBySprint(sprint);
    }

    /**
     * Gera o relatório em segundo plano para não bloquear a requisição de
     * encerramento da sprint. Falhas ficam registradas em log; o líder pode
     * disparar uma nova tentativa síncrona via generateAndSave se o
     * relatório não aparecer.
     *
     * Recebe só o ID da sprint (não a entidade) e busca tudo de novo aqui
     * dentro: a thread do @Async não tem a sessão Hibernate da requisição
     * original, então usar entidades carregadas em outra thread (como
     * team.getMembers(), que é @ManyToMany preguiçoso) lança
     * LazyInitializationException. Buscando de novo dentro da transação
     * própria deste método, tudo fica corretamente anexado à sessão.
     */
    @Async
    @Transactional
    public void generateAndSaveAsync(Long sprintId) {
        try {
            Sprint sprint = sprintRepository.findById(sprintId)
                    .orElseThrow(() -> new RuntimeException("Sprint não encontrada"));
            generateAndSave(sprint, sprint.getTeam());
        } catch (Exception e) {
            log.error("Falha ao gerar relatório para a sprint {}: {}", sprintId, e.getMessage(), e);
        }
    }
}
