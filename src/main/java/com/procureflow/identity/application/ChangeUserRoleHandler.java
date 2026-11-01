package com.procureflow.identity.application;

import com.procureflow.identity.application.command.ChangeUserRolesCommand;
import com.procureflow.identity.domain.IdentityError;
import com.procureflow.identity.domain.Role;
import com.procureflow.identity.domain.User;
import com.procureflow.identity.domain.UserId;
import com.procureflow.identity.domain.port.RoleRepository;
import com.procureflow.identity.domain.port.UserRepository;
import com.procureflow.shared.domain.CommonError;
import com.procureflow.shared.domain.DomainException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ChangeUserRoleHandler {

    private final UserRepository users;
    private final RoleRepository roles;

    public ChangeUserRoleHandler(UserRepository users,RoleRepository roles){
        this.users = users;
        this.roles = roles;
    }

    @Transactional
    @PreAuthorize("hasAuthority('ROLE_MANAGE')")
    public UserRoles handle(ChangeUserRolesCommand cmd){
        User user = users.findById(new UserId((cmd.userId())))
                .orElseThrow(()-> new DomainException(IdentityError.USER_NOT_FOUND,"user not found"));

        //resolve evry role first
        List<Role> toAssign = (cmd.assign() == null ? List.<String>of() : cmd.assign()).stream()
                .map(code->roles.findByCode(code).orElseThrow(()->
                        new DomainException(CommonError.VALIDATION_FAILED,"Unknown role : " + code))).toList();

        toAssign.forEach(user::assignRole);
        if(cmd.remove() != null){
            cmd.remove().forEach(user::removeRole);
        }

        User saved = users.save(user);
        return new UserRoles(saved.id().value().toString(),saved.email().value(),saved.roles().stream().map(Role::code).sorted().toList());
    }
}
