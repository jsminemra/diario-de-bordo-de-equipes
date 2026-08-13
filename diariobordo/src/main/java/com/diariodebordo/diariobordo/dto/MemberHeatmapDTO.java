package com.diariodebordo.diariobordo.dto;

import com.diariodebordo.diariobordo.model.User;
import lombok.Data;

import java.util.List;

/**
 * Par (membro, heatmap) usado nas visões "gerais" — um heatmap
 * compacto por integrante da equipe, tanto de registro diário quanto
 * de commits do GitHub.
 */
@Data
public class MemberHeatmapDTO {
    private User member;
    private List<HeatmapDataDTO> heatmapData;
    private long totalAtividade;
    private String semDadosMotivo; // null quando há dados normalmente
}
