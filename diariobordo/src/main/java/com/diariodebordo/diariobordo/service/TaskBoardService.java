package com.diariodebordo.diariobordo.service;

import com.diariodebordo.diariobordo.dto.MyWorkItemDTO;
import com.diariodebordo.diariobordo.model.Epic;
import com.diariodebordo.diariobordo.model.Task;
import com.diariodebordo.diariobordo.model.Team;
import com.diariodebordo.diariobordo.model.User;
import com.diariodebordo.diariobordo.model.UserStory;
import com.diariodebordo.diariobordo.model.WorkStatus;
import com.diariodebordo.diariobordo.repository.EpicRepository;
import com.diariodebordo.diariobordo.repository.TaskRepository;
import com.diariodebordo.diariobordo.repository.TeamRepository;
import com.diariodebordo.diariobordo.repository.UserStoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TaskBoardService {

    private final EpicRepository epicRepository;
    private final UserStoryRepository userStoryRepository;
    private final TaskRepository taskRepository;
    private final TeamRepository teamRepository;

    public Team getUserTeam(User user) {
        List<Team> teams = teamRepository.findByMemberId(user.getId());
        if (teams.isEmpty()) {
            throw new RuntimeException("Você não pertence a nenhuma equipe.");
        }
        return teams.get(0);
    }

    public Epic createEpic(String title, String description, User author) {
        Team team = getUserTeam(author);
        if (title == null || title.isBlank()) {
            throw new RuntimeException("Informe um título para a epic.");
        }

        Epic epic = new Epic();
        epic.setTitle(title.trim());
        epic.setDescription(description);
        epic.setTeam(team);
        epic.setCreatedBy(author);
        return epicRepository.save(epic);
    }

    public UserStory createUserStory(Long epicId, String title, String description, Long assigneeId, User author) {
        Epic epic = epicRepository.findById(epicId)
                .orElseThrow(() -> new RuntimeException("Epic não encontrada."));
        Team team = requireSameTeam(epic.getTeam(), author);
        if (title == null || title.isBlank()) {
            throw new RuntimeException("Informe um título para a user story.");
        }

        UserStory story = new UserStory();
        story.setTitle(title.trim());
        story.setDescription(description);
        story.setEpic(epic);
        story.setAssignee(resolveAssignee(assigneeId, team));
        story.setCreatedBy(author);
        return userStoryRepository.save(story);
    }

    public Task createTask(Long storyId, String title, String description, Long assigneeId, User author) {
        UserStory story = userStoryRepository.findById(storyId)
                .orElseThrow(() -> new RuntimeException("User story não encontrada."));
        Team team = requireSameTeam(story.getEpic().getTeam(), author);
        if (title == null || title.isBlank()) {
            throw new RuntimeException("Informe um título para a task.");
        }

        Task task = new Task();
        task.setTitle(title.trim());
        task.setDescription(description);
        task.setUserStory(story);
        task.setAssignee(resolveAssignee(assigneeId, team));
        task.setCreatedBy(author);
        return taskRepository.save(task);
    }

    public UserStory updateStoryStatus(Long storyId, WorkStatus status, User author) {
        UserStory story = userStoryRepository.findById(storyId)
                .orElseThrow(() -> new RuntimeException("User story não encontrada."));
        requireSameTeam(story.getEpic().getTeam(), author);
        story.setStatus(status);
        return userStoryRepository.save(story);
    }

    public Task updateTaskStatus(Long taskId, WorkStatus status, User author) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task não encontrada."));
        requireSameTeam(task.getUserStory().getEpic().getTeam(), author);
        task.setStatus(status);
        return taskRepository.save(task);
    }

    public UserStory reassignStory(Long storyId, Long assigneeId, User author) {
        UserStory story = userStoryRepository.findById(storyId)
                .orElseThrow(() -> new RuntimeException("User story não encontrada."));
        Team team = requireSameTeam(story.getEpic().getTeam(), author);
        story.setAssignee(resolveAssignee(assigneeId, team));
        return userStoryRepository.save(story);
    }

    public Task reassignTask(Long taskId, Long assigneeId, User author) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task não encontrada."));
        Team team = requireSameTeam(task.getUserStory().getEpic().getTeam(), author);
        task.setAssignee(resolveAssignee(assigneeId, team));
        return taskRepository.save(task);
    }

    public List<Epic> getBoardData(User user) {
        Team team = getUserTeam(user);
        return epicRepository.findByTeamOrderByCreatedAtDesc(team);
    }

    public Map<Long, List<UserStory>> getStoriesByEpic(List<Epic> epics) {
        return userStoryRepository.findByEpicIn(epics).stream()
                .collect(Collectors.groupingBy(s -> s.getEpic().getId()));
    }

    public Map<Long, List<Task>> getTasksByStory(List<UserStory> stories) {
        return taskRepository.findByUserStoryIn(stories).stream()
                .collect(Collectors.groupingBy(t -> t.getUserStory().getId()));
    }

    public List<MyWorkItemDTO> getMyAssignments(User user) {
        List<MyWorkItemDTO> items = new ArrayList<>();

        for (UserStory story : userStoryRepository.findByAssignee(user)) {
            items.add(new MyWorkItemDTO(MyWorkItemDTO.Type.STORY, story.getId(), story.getTitle(),
                    story.getEpic().getTitle(), null, story.getStatus()));
        }
        for (Task task : taskRepository.findByAssignee(user)) {
            items.add(new MyWorkItemDTO(MyWorkItemDTO.Type.TASK, task.getId(), task.getTitle(),
                    task.getUserStory().getEpic().getTitle(), task.getUserStory().getTitle(), task.getStatus()));
        }

        items.sort((a, b) -> a.getStatus() == b.getStatus()
                ? a.getTitle().compareToIgnoreCase(b.getTitle())
                : a.getStatus().compareTo(b.getStatus()));
        return items;
    }

    private Team requireSameTeam(Team team, User author) {
        Team authorTeam = getUserTeam(author);
        if (!authorTeam.getId().equals(team.getId())) {
            throw new RuntimeException("Você não tem permissão para alterar itens desta equipe.");
        }
        return authorTeam;
    }

    private User resolveAssignee(Long assigneeId, Team team) {
        if (assigneeId == null) {
            return null;
        }
        return team.getMembers().stream()
                .filter(m -> m.getId().equals(assigneeId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("O responsável selecionado não pertence a esta equipe."));
    }
}
