package com.diariodebordo.diariobordo.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class HeatmapDataDTO {
    private LocalDate date;
    private int count;
    private String summary;
    private String type; // "entry" ou "commit" ou "none"
}