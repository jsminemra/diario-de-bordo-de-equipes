package com.diariodebordo.diariobordo.dto;

import com.diariodebordo.diariobordo.model.WorkStatus;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class MyWorkItemDTO {

    public enum Type { STORY, TASK }

    private Type type;
    private Long id;
    private String title;
    private String epicTitle;
    private String storyTitle;
    private WorkStatus status;
}
