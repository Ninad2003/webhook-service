package com.project.webhook_service.controller;

import com.project.webhook_service.dto.EventDetailResponse;
import com.project.webhook_service.dto.EventIngestionRequest;
import com.project.webhook_service.dto.EventIngestionResponse;
import com.project.webhook_service.dto.EventResponse;
import com.project.webhook_service.service.EventIngestionService;
import com.project.webhook_service.service.EventQueryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/events")
@CrossOrigin("*")
@RequiredArgsConstructor
public class EventController {

    private final EventIngestionService eventIngestionService;
    private final EventQueryService eventQueryService;

    @PostMapping
    public ResponseEntity<EventIngestionResponse> ingestEvent(@Valid @RequestBody EventIngestionRequest request) {
        EventIngestionResponse response = eventIngestionService.ingestEvent(request);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    @GetMapping
    public ResponseEntity<Page<EventResponse>> listEvents(
            @RequestParam(required = false) String partnerId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String eventType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<EventResponse> events = eventQueryService.listEvents(partnerId, status, eventType, pageable);
        return ResponseEntity.ok(events);
    }

    @GetMapping("/{id}")
    public ResponseEntity<EventDetailResponse> getEventDetail(@PathVariable Long id) {
        EventDetailResponse detail = eventQueryService.getEventDetail(id);
        return ResponseEntity.ok(detail);
    }
}
