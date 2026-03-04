package com.example.Project_Management.controller;


import com.example.Project_Management.service.ChatBotService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequestMapping("/api")
public class ChatBotController {

    @Autowired
    private ChatBotService chatBotService;

    @GetMapping("/chat/ask")
    public ResponseEntity<String> askBot(@RequestParam String userQuery, @RequestParam String username) throws IOException {

        String response = chatBotService.getBotResponse(userQuery, username);

        return ResponseEntity.ok(response);
    }
}
