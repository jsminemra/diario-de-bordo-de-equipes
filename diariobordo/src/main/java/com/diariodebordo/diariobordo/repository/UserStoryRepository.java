package com.diariodebordo.diariobordo.repository;

import com.diariodebordo.diariobordo.model.Epic;
import com.diariodebordo.diariobordo.model.User;
import com.diariodebordo.diariobordo.model.UserStory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserStoryRepository extends JpaRepository<UserStory, Long> {

    List<UserStory> findByEpicOrderByCreatedAtAsc(Epic epic);

    List<UserStory> findByAssignee(User assignee);

    List<UserStory> findByEpicIn(List<Epic> epics);
}
