package com.redbus.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "chat_messages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private ChatSession session;

    @Column(nullable = false, length = 20)
    private String role; // 'user', 'assistant', 'system'

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "tool_calls", columnDefinition = "TEXT")
    private String toolCalls;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;
}
