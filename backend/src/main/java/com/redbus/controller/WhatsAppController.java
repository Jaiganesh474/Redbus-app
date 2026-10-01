package com.redbus.controller;

import com.redbus.service.WhatsAppService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/whatsapp")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class WhatsAppController {

    private final WhatsAppService whatsAppService;

    @GetMapping("/test")
    public ResponseEntity<Map<String, Object>> testWhatsApp(
            @RequestParam(defaultValue = "8939129572") String phone,
            @RequestParam(defaultValue = "template") String type,
            @RequestParam(required = false) String text
    ) {
        Map<String, Object> response = new HashMap<>();
        response.put("recipient", phone);
        response.put("type", type);

        boolean success;
        if ("template".equalsIgnoreCase(type)) {
            success = whatsAppService.sendTemplateMessage(phone, "hello_world", "en_US");
            response.put("template", "hello_world");
        } else {
            String msg = (text != null && !text.isBlank()) ? text : "🎉 redBus Test: WhatsApp Integration is fully functional!";
            success = whatsAppService.sendTextMessage(phone, msg);
            response.put("message", msg);
        }

        response.put("success", success);
        response.put("status", success ? "DELIVERED_TO_META_GATEWAY" : "FAILED");
        return ResponseEntity.ok(response);
    }
}
