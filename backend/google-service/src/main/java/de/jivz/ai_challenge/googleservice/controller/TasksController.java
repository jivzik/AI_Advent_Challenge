package de.jivz.ai_challenge.googleservice.controller;

import de.jivz.ai_challenge.googleservice.dto.TaskRequest;
import de.jivz.ai_challenge.googleservice.dto.TaskResponse;
import com.google.api.services.tasks.model.TaskList;
import de.jivz.ai_challenge.googleservice.service.GoogleTasksService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
@Slf4j
public class TasksController {

    private final GoogleTasksService googleTasksService;

    /**
     * Получить все списки задач
     * GET /api/tasks/lists
     */
    @GetMapping("/lists")
    public ResponseEntity<List<TaskList>> getTaskLists() {
        try {
            List<TaskList> taskLists = googleTasksService.getAllTaskLists();
            return ResponseEntity.ok(taskLists);
        } catch (IOException e) {
            log.error("Ошибка при получении списков задач", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Получить задачи из конкретного списка
     * GET /api/tasks/lists/{taskListId}
     */
    @GetMapping("/lists/{taskListId}")
    public ResponseEntity<List<TaskResponse>> getTasks(@PathVariable String taskListId) {
        try {
            List<TaskResponse> tasks = googleTasksService.getTasks(taskListId);
            return ResponseEntity.ok(tasks);
        } catch (IOException e) {
            log.error("Ошибка при получении задач из списка: {}", taskListId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Получить задачи из дефолтного списка
     * GET /api/tasks
     */
    @GetMapping
    public ResponseEntity<List<TaskResponse>> getDefaultTasks() {
        try {
            List<TaskResponse> tasks = googleTasksService.getDefaultTasks();
            return ResponseEntity.ok(tasks);
        } catch (IOException e) {
            log.error("Ошибка при получении задач из дефолтного списка", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Создать задачу в конкретном списке
     * POST /api/tasks/lists/{taskListId}
     */
    @PostMapping("/lists/{taskListId}")
    public ResponseEntity<TaskResponse> createTask(
            @PathVariable String taskListId,
            @RequestBody TaskRequest taskRequest) {
        try {
            TaskResponse task = googleTasksService.createTask(taskListId, taskRequest);
            return ResponseEntity.status(HttpStatus.CREATED).body(task);
        } catch (IOException e) {
            log.error("Ошибка при создании задачи в списке: {}", taskListId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Создать задачу в дефолтном списке
     * POST /api/tasks
     */
    @PostMapping
    public ResponseEntity<TaskResponse> createDefaultTask(@RequestBody TaskRequest taskRequest) {
        try {
            TaskResponse task = googleTasksService.createDefaultTask(taskRequest);
            return ResponseEntity.status(HttpStatus.CREATED).body(task);
        } catch (IOException e) {
            log.error("Ошибка при создании задачи в дефолтном списке", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Обновить задачу
     * PUT /api/tasks/lists/{taskListId}/tasks/{taskId}
     */
    @PutMapping("/lists/{taskListId}/tasks/{taskId}")
    public ResponseEntity<TaskResponse> updateTask(
            @PathVariable String taskListId,
            @PathVariable String taskId,
            @RequestBody TaskRequest taskRequest) {
        try {
            TaskResponse task = googleTasksService.updateTask(taskListId, taskId, taskRequest);
            return ResponseEntity.ok(task);
        } catch (IOException e) {
            log.error("Ошибка при обновлении задачи {} в списке: {}", taskId, taskListId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Удалить задачу
     * DELETE /api/tasks/lists/{taskListId}/tasks/{taskId}
     */
    @DeleteMapping("/lists/{taskListId}/tasks/{taskId}")
    public ResponseEntity<Void> deleteTask(
            @PathVariable String taskListId,
            @PathVariable String taskId) {
        try {
            googleTasksService.deleteTask(taskListId, taskId);
            return ResponseEntity.noContent().build();
        } catch (IOException e) {
            log.error("Ошибка при удалении задачи {} из списка: {}", taskId, taskListId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Отметить задачу как выполненную
     * PATCH /api/tasks/lists/{taskListId}/tasks/{taskId}/complete
     */
    @PatchMapping("/lists/{taskListId}/tasks/{taskId}/complete")
    public ResponseEntity<TaskResponse> completeTask(
            @PathVariable String taskListId,
            @PathVariable String taskId) {
        try {
            TaskResponse task = googleTasksService.completeTask(taskListId, taskId);
            return ResponseEntity.ok(task);
        } catch (IOException e) {
            log.error("Ошибка при отметке задачи {} как выполненной в списке: {}", taskId, taskListId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}