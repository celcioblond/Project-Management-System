package com.celcioblond.pms.controller;

import com.celcioblond.pms.dto.ChatRequest;
import com.celcioblond.pms.security.UserPrincipal;
import com.celcioblond.pms.service.ChatBotService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ChatBotController {

    private final ChatBotService chatBotService;

    @PostMapping("/chat/ask")
    public ResponseEntity<String> askBot(@Valid @RequestBody ChatRequest request,
                                         @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(chatBotService.getBotResponse(request.message(), currentUser));
    }
}
