package com.diariodebordo.diariobordo.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class JoinTeamDTO {

    @NotBlank(message = "O código é obrigatório")
    private String code;
}
