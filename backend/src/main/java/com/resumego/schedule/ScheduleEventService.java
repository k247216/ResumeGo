package com.resumego.schedule;

import com.resumego.common.CurrentUser;
import com.resumego.schedule.dto.CreateScheduleEventRequest;
import com.resumego.schedule.dto.ScheduleEventResponse;
import com.resumego.schedule.dto.UpdateScheduleEventRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;

@Service
public class ScheduleEventService {

    private static final Set<String> EVENT_TYPES = Set.of("interview", "exam", "followup", "other");

    private final ScheduleEventRepository repository;

    public ScheduleEventService(ScheduleEventRepository repository) {
        this.repository = repository;
    }

    public List<ScheduleEventResponse> list(LocalDateTime from, LocalDateTime to) {
        return repository.findAll(userId(), from, to).stream().map(this::toResponse).toList();
    }

    public ScheduleEventResponse get(long eventId) {
        return toResponse(requireEvent(eventId));
    }

    @Transactional
    public ScheduleEventResponse create(CreateScheduleEventRequest request) {
        ResolvedLink link = validate(request.title(), request.eventType(), request.startTime(), request.endTime(),
                request.jobDescriptionId(), request.jobProjectId());
        long id = repository.create(userId(), normalizeTitle(request.title()), request.eventType(),
                request.startTime(), request.endTime(), request.notes(), link.jobDescriptionId(),
                link.jobProjectId());
        return get(id);
    }

    @Transactional
    public ScheduleEventResponse update(long eventId, UpdateScheduleEventRequest request) {
        requireEvent(eventId);
        ResolvedLink link = validate(request.title(), request.eventType(), request.startTime(), request.endTime(),
                request.jobDescriptionId(), request.jobProjectId());
        repository.update(userId(), eventId, normalizeTitle(request.title()), request.eventType(),
                request.startTime(), request.endTime(), request.notes(), link.jobDescriptionId(),
                link.jobProjectId());
        return get(eventId);
    }

    @Transactional
    public boolean delete(long eventId) {
        return repository.softDelete(userId(), eventId) > 0;
    }

    private ResolvedLink validate(String title, String eventType, LocalDateTime startTime,
                                  LocalDateTime endTime, Long jobDescriptionId, Long jobProjectId) {
        if (title == null || title.strip().isEmpty()) {
            throw new IllegalArgumentException("日程标题不能为空");
        }
        if (eventType == null || !EVENT_TYPES.contains(eventType)) {
            throw new IllegalArgumentException("日程类型不合法");
        }
        if (startTime == null) {
            throw new IllegalArgumentException("开始时间不能为空");
        }
        if (endTime != null && endTime.isBefore(startTime)) {
            throw new IllegalArgumentException("结束时间不能早于开始时间");
        }
        if (jobProjectId != null) {
            if (!repository.ownsJobProject(userId(), jobProjectId)) {
                throw new IllegalArgumentException("所选求职计划不可用");
            }
            Long projectJobDescriptionId = repository.findJobDescriptionIdForProject(userId(), jobProjectId);
            if (jobDescriptionId != null && !Objects.equals(jobDescriptionId, projectJobDescriptionId)) {
                throw new IllegalArgumentException("岗位与求职计划不一致");
            }
            return new ResolvedLink(projectJobDescriptionId, jobProjectId);
        }
        if (jobDescriptionId != null && !repository.ownsJobDescription(userId(), jobDescriptionId)) {
            throw new IllegalArgumentException("所选目标岗位不可用");
        }
        return new ResolvedLink(jobDescriptionId, null);
    }

    private String normalizeTitle(String value) {
        String title = value.strip();
        if (title.length() > 120) {
            throw new IllegalArgumentException("日程标题不能超过 120 个字符");
        }
        return title;
    }

    private ScheduleEvent requireEvent(long eventId) {
        return repository.findById(userId(), eventId)
                .orElseThrow(() -> new NoSuchElementException("日程不存在"));
    }

    private long userId() {
        return CurrentUser.DEMO_USER_ID;
    }

    private ScheduleEventResponse toResponse(ScheduleEvent event) {
        return new ScheduleEventResponse(event.id(), event.title(), event.eventType(),
                event.startTime(), event.endTime(), event.notes(), event.jobDescriptionId(),
                event.jobProjectId(), event.createdAt(), event.updatedAt());
    }

    private record ResolvedLink(Long jobDescriptionId, Long jobProjectId) {
    }
}
