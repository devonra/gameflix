package com.gameflix.userservice.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "users",
       uniqueConstraints = @UniqueConstraint(name = "uk_users_email", columnNames = "email"))
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String email;

    @Column(nullable = false, length = 50)
    private String username;

    // Sized for the hash, not the password: "{bcrypt}" + 60 chars = 68
    @Column(nullable = false, length = 255)
    private String password;

    @Column(nullable = false, length = 20)
    private String role;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected User() { }

    public User(String email, String username, String passwordHash, String role) {
        this.email = email;
        this.username = username;
        this.password = passwordHash;
        this.role = role;
    }

    @PrePersist
    void onCreate() { this.createdAt = Instant.now(); }

    public Long getId() { return id; }
    public String getEmail() { return email; }
    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public String getRole() { return role; }
    public Instant getCreatedAt() { return createdAt; }
}
