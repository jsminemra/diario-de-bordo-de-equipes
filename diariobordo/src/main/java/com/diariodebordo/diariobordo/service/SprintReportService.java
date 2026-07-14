package com.diariodebordo.diariobordo.service;

import com.diariodebordo.diariobordo.model.DailyEntry;
import com.diariodebordo.diariobordo.model.Sprint;
import com.diariodebordo.diariobordo.model.SprintReport;
import com.diariodebordo.diariobordo.model.Team;
import com.diariodebordo.diariobordo.repository.DailyEntryRepository;
import com.diariodebordo.diariobordo.repository.SprintReportRepository;
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

    public SprintReportService(SprintReportRepository sprintReportRepository,
                               DailyEntryRepository dailyEntryRepository) {
        this.sprintReportRepository = sprintReportRepository;
        this.dailyEntryRepository = dailyEntryRepository;
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
     */
    @Async
    public void generateAndSaveAsync(Sprint sprint, Team team) {
        try {
            generateAndSave(sprint, team);
        } catch (Exception e) {
            log.error("Falha ao gerar relatório para a sprint {}: {}", sprint.getId(), e.getMessage(), e);
        }
    }
}
