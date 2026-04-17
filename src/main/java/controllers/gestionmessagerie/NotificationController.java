package tn.esprit.pijava.springboot.controllers.gestionmessagerie;

import java.util.List;
import java.util.stream.Collectors;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tn.esprit.pijava.springboot.dto.NotificationRequestDto;
import tn.esprit.pijava.springboot.dto.NotificationResponseDto;
import models.gestionmessagerie.Notification;
import tn.esprit.pijava.springboot.service.NotificationService;

@RestController
@RequestMapping("/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public ResponseEntity<List<NotificationResponseDto>> findAll() {
        List<NotificationResponseDto> notifications = notificationService.findAll().stream()
                .map(this::toResponseDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(notifications);
    }

    @GetMapping("/{id}")
    public ResponseEntity<NotificationResponseDto> findById(@PathVariable Long id) {
        Notification notification = notificationService.findById(id);
        return ResponseEntity.ok(toResponseDto(notification));
    }

    @PostMapping
    public ResponseEntity<NotificationResponseDto> create(@Valid @RequestBody NotificationRequestDto requestDto) {
        Notification saved = notificationService.save(requestDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponseDto(saved));
    }

    @PutMapping("/{id}")
    public ResponseEntity<NotificationResponseDto> update(@PathVariable Long id, @Valid @RequestBody NotificationRequestDto requestDto) {
        Notification updated = notificationService.update(id, requestDto);
        return ResponseEntity.ok(toResponseDto(updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        notificationService.deleteById(id);
        return ResponseEntity.ok().build();
    }

    private NotificationResponseDto toResponseDto(Notification notification) {
        NotificationResponseDto dto = new NotificationResponseDto();
        dto.setId(notification.getId());
        dto.setContenu(notification.getContenu());
        dto.setDateNotification(notification.getDateNotification());
        dto.setStatut(notification.getStatut());
        dto.setUserId(notification.getUser() != null ? notification.getUser().getId() : null);
        return dto;
    }
}
