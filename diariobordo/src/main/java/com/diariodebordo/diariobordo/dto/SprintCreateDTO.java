package com.diariodebordo.diariobordo.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Data
public class SprintCreateDTO {

    @NotBlank(message = "O nome da sprint é obrigatório")
    private String name;

    @NotNull(message = "A data de início é obrigatória")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @FutureOrPresent(message = "A data de início deve ser hoje ou no futuro")
    private LocalDate startDate;

    @NotNull(message = "A data de fim é obrigatória")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @Future(message = "A data de fim deve ser no futuro")
    private LocalDate endDate;
}