package com.procureflow.identity.infrastructure.persistence;

import com.procureflow.identity.domain.UserStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "app_user")
public class UserEntity {

    @Getter
    @Id
    @Column(name = "id")
    private UUID id;

    @Setter
    @Getter
    @Column(name = "email" , nullable = false)
    private String email;

    @Setter
    @Getter
    @Column(name = "password_hash",nullable = false)
    private String passwordHash;

    @Setter
    @Getter
    @Column(name = "full_name",nullable = false)
    private String fullName;

    @Setter
    @Getter
    @Enumerated(EnumType.STRING)
    @Column(name = "status",nullable = false)
    private UserStatus status;

    @Setter
    @Getter
    @Column(name = "failed_attempts",nullable = false)
    private int failedAttempts;

    @Setter
    @Getter
    @Column(name = "locked_until")
    private Instant lockedUntil;

    @Column(name = "created_at",nullable = false,updatable = false)
    private Instant createdAt;

    //@Version
    @Column(name = "updated_at",nullable = false)
    private Instant updatedAt;

    @Getter
    @Version
    @Column(name = "version",nullable = false)
    private Long version;

    @Setter
    @Getter
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "user_role",joinColumns = @JoinColumn(name = "user_id"),inverseJoinColumns = @JoinColumn(name = "role_code"))
    private Set<RoleEntity> roles = new HashSet<>();

    protected UserEntity(){}

    public UserEntity(UUID id){
        this.id = id;
    }

    @PrePersist
    void onCreate(){
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate(){
        updatedAt = Instant.now();
    }

}
