package com.hufsphere.linkboard.domain;

import lombok.*;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Workspace {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private String description;

    private String defaultLanguage;

    private LocalDateTime updatedAt;

    @PreUpdate
    @PrePersist
    public void updateTime() {
        this.updatedAt = LocalDateTime.now();
    }
}