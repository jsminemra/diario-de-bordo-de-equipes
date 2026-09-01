package com.diariodebordo.diariobordo.controller;

import com.diariodebordo.diariobordo.model.Epic;
import com.diariodebordo.diariobordo.model.Task;
import com.diariodebordo.diariobordo.model.Team;
import com.diariodebordo.diariobordo.model.User;
import com.diariodebordo.diariobordo.model.UserStory;
import com.diariodebordo.diariobordo.model.WorkStatus;
import com.diariodebordo.diariobordo.repository.UserRepository;
import com.diariodebordo.diariobordo.service.TaskBoardService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/tasks")
public class TaskBoardController {

    private static final Map<WorkStatus, String> STATUS_LABELS = Map.of(
            WorkStatus.A_FAZER, "A Fazer",
            WorkStatus.EM_ANDAMENTO, "Em Andamento",
            WorkStatus.CONCLUIDA, "Concluída"
    );

    private static final Map<WorkStatus, String> STATUS_BADGE_CLASSES = Map.of(
            WorkStatus.A_FAZER, "badge-neutral",
            WorkStatus.EM_ANDAMENTO, "badge-primary",
            WorkStatus.CONCLUIDA, "badge-success"
    );

    private final TaskBoardService taskBoardService;
    private final UserRepository userRepository;

    public TaskBoardController(TaskBoardService taskBoardService, UserRepository userRepository) {
        this.taskBoardService = taskBoardService;
        this.userRepository = userRepository;
    }

    @GetMapping
    public String board(Authentication auth, Model model, RedirectAttributes redirectAttributes) {
        try {
            User user = getAuthenticatedUser(auth);
            Team team = taskBoardService.getUserTeam(user);
            List<Epic> epics = taskBoardService.getBoardData(user);
            Map<Long, List<UserStory>> storiesByEpic = taskBoardService.getStoriesByEpic(epics);
            List<UserStory> allStories = storiesByEpic.values().stream().flatMap(List::stream).toList();
            Map<Long, List<Task>> tasksByStory = taskBoardService.getTasksByStory(allStories);

            Map<WorkStatus, Long> storyCountByStatus = allStories.stream()
                    .collect(Collectors.groupingBy(UserStory::getStatus, Collectors.counting()));
            Map<Long, Long> doneTaskCountByStoryId = tasksByStory.entrySet().stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().stream()
                            .filter(t -> t.getStatus() == WorkStatus.CONCLUIDA).count()));

            model.addAttribute("team", team);
            model.addAttribute("epics", epics);
            model.addAttribute("allStories", allStories);
            model.addAttribute("storiesByEpic", storiesByEpic);
            model.addAttribute("tasksByStory", tasksByStory);
            model.addAttribute("statuses", WorkStatus.values());
            model.addAttribute("statusLabels", STATUS_LABELS);
            model.addAttribute("storyCountByStatus", storyCountByStatus);
            model.addAttribute("doneTaskCountByStoryId", doneTaskCountByStoryId);
            return "tasks/board";
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/dashboard";
        }
    }

    @GetMapping("/backlog")
    public String backlog(Authentication auth, Model model, RedirectAttributes redirectAttributes) {
        try {
            User user = getAuthenticatedUser(auth);
            Team team = taskBoardService.getUserTeam(user);
            List<Epic> epics = taskBoardService.getBoardData(user);
            Map<Long, List<UserStory>> storiesByEpic = taskBoardService.getStoriesByEpic(epics);
            List<UserStory> allStories = storiesByEpic.values().stream().flatMap(List::stream).toList();
            Map<Long, List<Task>> tasksByStory = taskBoardService.getTasksByStory(allStories);

            model.addAttribute("team", team);
            model.addAttribute("epics", epics);
            model.addAttribute("storiesByEpic", storiesByEpic);
            model.addAttribute("tasksByStory", tasksByStory);
            model.addAttribute("statuses", WorkStatus.values());
            model.addAttribute("statusLabels", STATUS_LABELS);
            model.addAttribute("statusBadgeClasses", STATUS_BADGE_CLASSES);
            return "tasks/backlog";
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/dashboard";
        }
    }

    @PostMapping("/epics")
    public String createEpic(@RequestParam String title,
                              @RequestParam(required = false) String description,
                              Authentication auth,
                              RedirectAttributes redirectAttributes) {
        try {
            User user = getAuthenticatedUser(auth);
            taskBoardService.createEpic(title, description, user);
            redirectAttributes.addFlashAttribute("success", "Epic criada com sucesso!");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/tasks/backlog";
    }

    @PostMapping("/epics/{epicId}/stories")
    public String createStory(@PathVariable Long epicId,
                               @RequestParam String title,
                               @RequestParam(required = false) String description,
                               @RequestParam(required = false) String assigneeId,
                               Authentication auth,
                               RedirectAttributes redirectAttributes) {
        try {
            User user = getAuthenticatedUser(auth);
            taskBoardService.createUserStory(epicId, title, description, parseAssigneeId(assigneeId), user);
            redirectAttributes.addFlashAttribute("success", "User story criada com sucesso!");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/tasks/backlog";
    }

    @PostMapping("/stories/{storyId}/tasks")
    public String createTask(@PathVariable Long storyId,
                              @RequestParam String title,
                              @RequestParam(required = false) String description,
                              @RequestParam(required = false) String assigneeId,
                              Authentication auth,
                              RedirectAttributes redirectAttributes) {
        try {
            User user = getAuthenticatedUser(auth);
            taskBoardService.createTask(storyId, title, description, parseAssigneeId(assigneeId), user);
            redirectAttributes.addFlashAttribute("success", "Task criada com sucesso!");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/tasks/backlog";
    }

    @PostMapping("/stories/{storyId}/assign")
    public String assignStory(@PathVariable Long storyId,
                               @RequestParam(required = false) String assigneeId,
                               Authentication auth,
                               RedirectAttributes redirectAttributes) {
        try {
            User user = getAuthenticatedUser(auth);
            taskBoardService.reassignStory(storyId, parseAssigneeId(assigneeId), user);
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/tasks/backlog";
    }

    @PostMapping("/tasks/{taskId}/assign")
    public String assignTask(@PathVariable Long taskId,
                              @RequestParam(required = false) String assigneeId,
                              Authentication auth,
                              RedirectAttributes redirectAttributes) {
        try {
            User user = getAuthenticatedUser(auth);
            taskBoardService.reassignTask(taskId, parseAssigneeId(assigneeId), user);
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/tasks/backlog";
    }

    @PostMapping("/stories/{storyId}/status")
    @ResponseBody
    public ResponseEntity<String> updateStoryStatus(@PathVariable Long storyId,
                                                     @RequestParam WorkStatus status,
                                                     Authentication auth) {
        try {
            User user = getAuthenticatedUser(auth);
            taskBoardService.updateStoryStatus(storyId, status, user);
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/tasks/{taskId}/status")
    @ResponseBody
    public ResponseEntity<String> updateTaskStatus(@PathVariable Long taskId,
                                                    @RequestParam WorkStatus status,
                                                    Authentication auth) {
        try {
            User user = getAuthenticatedUser(auth);
            taskBoardService.updateTaskStatus(taskId, status, user);
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    private User getAuthenticatedUser(Authentication auth) {
        return userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
    }

    private Long parseAssigneeId(String raw) {
        return (raw == null || raw.isBlank()) ? null : Long.valueOf(raw);
    }
}
