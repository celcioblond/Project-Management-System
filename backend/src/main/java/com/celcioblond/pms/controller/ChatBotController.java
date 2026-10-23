package com.celcioblond.pms.controller;

import com.celcioblond.pms.service.ChatBotService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ChatBotController {

    private final ChatBotService chatBotService;

    @GetMapping("/chat/ask")
    public ResponseEntity<String> askBot(@RequestParam String userQuery, @RequestParam String username) {
        return ResponseEntity.ok(chatBotService.getBotResponse(userQuery, username));
    }
}
