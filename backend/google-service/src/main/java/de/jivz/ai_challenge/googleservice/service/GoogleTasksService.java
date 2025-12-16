package de.jivz.ai_challenge.googleservice.service;

import de.jivz.ai_challenge.googleservice.dto.TaskRequest;
import de.jivz.ai_challenge.googleservice.dto.TaskResponse;
import com.google.api.services.tasks.Tasks;
import com.google.api.services.tasks.model.Task;
import com.google.api.services.tasks.model.TaskList;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class GoogleTasksService {

    private final Tasks tasksService;
    private static final String DEFAULT_TASK_LIST = "@default";

    /**
     * Получить все списки задач
     */
    public List<TaskList> getAllTaskLists() throws IOException {
        log.info("Получение всех списков задач");
        var result = tasksService.tasklists().list()
                .setMaxResults(10L)
                .execute();

        List<TaskList> taskLists = result.getItems();
        if (taskLists == null || taskLists.isEmpty()) {
            log.info("Списки задач не найдены");
            return new ArrayList<>();
        }

        log.info("Найдено {} списков задач", taskLists.size());
        return taskLists;
    }

    /**
     * Получить задачи из списка
     */
    public List<TaskResponse> getTasks(String taskListId) throws IOException {
        log.info("Получение задач из списка: {}", taskListId);

        var result = tasksService.tasks()
                .list(taskListId)
                .setMaxResults(100L)
                .execute();

        List<Task> tasks = result.getItems();
        if (tasks == null || tasks.isEmpty()) {
            log.info("Задачи не найдены в списке: {}", taskListId);
            return new ArrayList<>();
        }

        log.info("Найдено {} задач в списке: {}", tasks.size(), taskListId);
        return tasks.stream()
                .map(this::convertToTaskResponse)
                .collect(Collectors.toList());
    }

    /**
     * Получить задачи из дефолтного списка
     */
    public List<TaskResponse> getDefaultTasks() throws IOException {
        return getTasks(DEFAULT_TASK_LIST);
    }

    /**
     * Создать новую задачу
     */
    public TaskResponse createTask(String taskListId, TaskRequest request) throws IOException {
        log.info("Создание новой задачи в списке: {}", taskListId);

        Task task = new Task();
        task.setTitle(request.getTitle());
        task.setNotes(request.getNotes());

        if (request.getDue() != null) {
            task.setDue(request.getDue());
        }

        Task result = tasksService.tasks().insert(taskListId, task).execute();
        log.info("Задача создана с ID: {}", result.getId());

        return convertToTaskResponse(result);
    }

    /**
     * Создать задачу в дефолтном списке
     */
    public TaskResponse createDefaultTask(TaskRequest request) throws IOException {
        return createTask(DEFAULT_TASK_LIST, request);
    }

    /**
     * Обновить задачу
     */
    public TaskResponse updateTask(String taskListId, String taskId, TaskRequest request) throws IOException {
        log.info("Обновление задачи {} в списке: {}", taskId, taskListId);

        Task task = tasksService.tasks().get(taskListId, taskId).execute();

        if (request.getTitle() != null) {
            task.setTitle(request.getTitle());
        }
        if (request.getNotes() != null) {
            task.setNotes(request.getNotes());
        }
        if (request.getDue() != null) {
            task.setDue(request.getDue());
        }

        Task result = tasksService.tasks().update(taskListId, taskId, task).execute();
        log.info("Задача обновлена: {}", result.getId());

        return convertToTaskResponse(result);
    }

    /**
     * Удалить задачу
     */
    public void deleteTask(String taskListId, String taskId) throws IOException {
        log.info("Удаление задачи {} из списка: {}", taskId, taskListId);
        tasksService.tasks().delete(taskListId, taskId).execute();
        log.info("Задача удалена успешно");
    }

    /**
     * Отметить задачу как выполненную
     */
    public TaskResponse completeTask(String taskListId, String taskId) throws IOException {
        log.info("Отметка задачи {} как выполненной в списке: {}", taskId, taskListId);

        Task task = tasksService.tasks().get(taskListId, taskId).execute();
        task.setStatus("completed");

        Task result = tasksService.tasks().update(taskListId, taskId, task).execute();
        log.info("Задача отмечена как выполненная: {}", result.getId());

        return convertToTaskResponse(result);
    }

    /**
     * Конвертировать Task в TaskResponse
     */
    private TaskResponse convertToTaskResponse(Task task) {
        return TaskResponse.builder()
                .id(task.getId())
                .title(task.getTitle())
                .notes(task.getNotes())
                .status(task.getStatus())
                .due(task.getDue())
                .updated(task.getUpdated())
                .build();
    }
}