package com.redbus.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "chat_sessions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatSession {

    @Id
    @Column(length = 64)
    private String id;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "started_at", insertable = false, updatable = false)
    private LocalDateTime startedAt;

    @Column(name = "last_active_at", insertable = false, updatable = false)
    private LocalDateTime lastActiveAt;

    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<ChatMessage> messages = new ArrayList<>();
}
