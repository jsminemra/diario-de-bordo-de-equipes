package com.diariodebordo.diariobordo.repository;

import com.diariodebordo.diariobordo.model.Task;
import com.diariodebordo.diariobordo.model.User;
import com.diariodebordo.diariobordo.model.UserStory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long> {

    List<Task> findByUserStoryOrderByCreatedAtAsc(UserStory userStory);

    List<Task> findByAssignee(User assignee);

    List<Task> findByUserStoryIn(List<UserStory> userStories);
}
