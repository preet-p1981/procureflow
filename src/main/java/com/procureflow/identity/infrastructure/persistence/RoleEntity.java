package com.procureflow.identity.infrastructure.persistence;

import jakarta.persistence.*;
import lombok.Getter;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "role")
public class RoleEntity {

    @Getter
    @Id
    @Column(name = "code" , length = 40)
    private String code;

    @Getter
    @Column(name = "description" , nullable = false)
    private String description;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "role_permission" , joinColumns = @JoinColumn(name = "role_code"))
    @Column(name = "permission_code" , length=60)
    private Set<String> permissionCodes = new HashSet<>();

    protected RoleEntity(){
        //for jpa
    }

    public Set<String> getPermissionCodes(){
        return permissionCodes;
    }

}
