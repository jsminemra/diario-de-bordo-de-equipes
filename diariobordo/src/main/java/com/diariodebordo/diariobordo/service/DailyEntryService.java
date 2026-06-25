package com.diariodebordo.diariobordo.service;

import com.diariodebordo.diariobordo.dto.DailyEntryDTO;
import com.diariodebordo.diariobordo.model.DailyEntry;
import com.diariodebordo.diariobordo.model.Sprint;
import com.diariodebordo.diariobordo.model.User;
import com.diariodebordo.diariobordo.repository.DailyEntryRepository;
import com.diariodebordo.diariobordo.repository.SprintRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DailyEntryService {

    private final DailyEntryRepository dailyEntryRepository;
    private final SprintRepository sprintRepository;

    // US-05: salva ou edita o registro do dia
    public DailyEntry salvarOuEditar(DailyEntryDTO dto, User user) {
        Sprint sprintAtiva = sprintRepository.findFirstByOrderByIdDesc()
                .orElseThrow(() -> new RuntimeException("Nenhuma sprint ativa encontrada"));

        Optional<DailyEntry> existente = dailyEntryRepository
                .findByUserAndEntryDate(user, LocalDate.now());

        DailyEntry entry = existente.orElse(new DailyEntry());
        entry.setUser(user);
        entry.setSprint(sprintAtiva);
        entry.setWhatWasDone(dto.getWhatWasDone());
        entry.setWhatWillBeDone(dto.getWhatWillBeDone());
        entry.setImpediments(dto.getImpediments());
        entry.setEntryDate(LocalDate.now());

        return dailyEntryRepository.save(entry);
    }

    // US-06: busca todos os registros do dia da equipe do usuário
    public List<DailyEntry> buscarFeedDoDia(User user) {
        Sprint sprintAtiva = sprintRepository.findFirstByOrderByIdDesc()
                .orElseThrow(() -> new RuntimeException("Nenhuma sprint ativa encontrada"));

        return dailyEntryRepository
                .findBySprintAndEntryDateOrderByCreatedAtDesc(sprintAtiva, LocalDate.now());
    }

    // Busca o registro do usuário no dia atual (para pré-preencher o formulário)
    public Optional<DailyEntry> buscarRegistroDeHoje(User user) {
        return dailyEntryRepository.findByUserAndEntryDate(user, LocalDate.now());
    }
}