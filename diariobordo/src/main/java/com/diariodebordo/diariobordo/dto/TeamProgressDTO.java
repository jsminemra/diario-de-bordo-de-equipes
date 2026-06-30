package com.diariodebordo.diariobordo.dto;

import lombok.Data;
import java.time.LocalDate;
import java.util.List;

@Data
public class TeamProgressDTO {
    private Long teamId;
    private String teamName;
    private String leaderName;
    private int totalMembers;
    private int totalSprints;
    private int completedSprints;
    private int currentSprintNumber;
    private String currentSprintName;
    private LocalDate currentSprintStartDate;
    private LocalDate currentSprintEndDate;
    private double progressPercentage;
    private List<ReleaseProgressDTO> releases;
    
    @Data
    public static class ReleaseProgressDTO {
        private int releaseNumber;
        private String releaseName;
        private int totalSprints;
        private int completedSprints;
        private double progressPercentage;
        private List<SprintProgressDTO> sprints;
    }
    
    @Data
    public static class SprintProgressDTO {
        private int sprintNumber;
        private String sprintName;
        private boolean completed;
        private LocalDate startDate;
        private LocalDate endDate;
    }
}