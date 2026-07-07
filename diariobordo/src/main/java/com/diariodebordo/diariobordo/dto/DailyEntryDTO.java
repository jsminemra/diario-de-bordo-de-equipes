package com.diariodebordo.diariobordo.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DailyEntryDTO {

    @NotBlank(message = "O campo 'O que fiz hoje' é obrigatório")
    private String whatWasDone;

    @NotBlank(message = "O campo 'O que farei amanhã' é obrigatório")
    private String whatWillBeDone;

    private String impediments;
}