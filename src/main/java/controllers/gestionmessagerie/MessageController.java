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
import tn.esprit.pijava.springboot.dto.MessageRequestDto;
import tn.esprit.pijava.springboot.dto.MessageResponseDto;
import models.gestionmessagerie.Message;
import tn.esprit.pijava.springboot.service.MessageService;

@RestController
@RequestMapping("/messages")
public class MessageController {

    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    @GetMapping
    public ResponseEntity<List<MessageResponseDto>> findAll() {
        List<MessageResponseDto> messages = messageService.findAll().stream()
                .map(this::toResponseDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(messages);
    }

    @GetMapping("/{id}")
    public ResponseEntity<MessageResponseDto> findById(@PathVariable Long id) {
        Message message = messageService.findById(id);
        return ResponseEntity.ok(toResponseDto(message));
    }

    @PostMapping
    public ResponseEntity<MessageResponseDto> create(@Valid @RequestBody MessageRequestDto requestDto) {
        Message saved = messageService.save(requestDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponseDto(saved));
    }

    @PutMapping("/{id}")
    public ResponseEntity<MessageResponseDto> update(@PathVariable Long id, @Valid @RequestBody MessageRequestDto requestDto) {
        Message updated = messageService.update(id, requestDto);
        return ResponseEntity.ok(toResponseDto(updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        messageService.deleteById(id);
        return ResponseEntity.ok().build();
    }

    private MessageResponseDto toResponseDto(Message message) {
        MessageResponseDto dto = new MessageResponseDto();
        dto.setId(message.getId());
        dto.setContenu(message.getContenu());
        dto.setDateEnvoi(message.getDateEnvoi());
        dto.setStatut(message.getStatut());
        dto.setExpediteurId(message.getExpediteur() != null ? message.getExpediteur().getId() : null);
        dto.setDestinataireId(message.getDestinataire() != null ? message.getDestinataire().getId() : null);
        return dto;
    }
}
