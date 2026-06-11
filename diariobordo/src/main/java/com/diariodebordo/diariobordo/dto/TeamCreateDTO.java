package com.diariodebordo.diariobordo.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TeamCreateDTO {
    
    @NotBlank(message = "O nome da equipe é obrigatório")
    private String name;
    
    private String description;
}