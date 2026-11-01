package com.procureflow.identity.web;

import com.procureflow.identity.application.ChangeUserRoleHandler;
import com.procureflow.identity.application.UserRoles;
import com.procureflow.identity.application.command.ChangeUserRolesCommand;
import com.procureflow.identity.web.request.ChangeRolesRequest;
import com.procureflow.identity.web.response.MeResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final ChangeUserRoleHandler changeRoles;

    public UserController(ChangeUserRoleHandler changeRoles) {
        this.changeRoles = changeRoles;
    }

    @GetMapping("/me")
    public MeResponse me(@AuthenticationPrincipal Jwt jwt, Authentication auth) {
        List<String> authorities = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority).sorted().toList();
        return new MeResponse(jwt.getSubject(), jwt.getClaimAsString("email"), authorities);
    }

    @PatchMapping("/{id}/roles")
    public UserRoles changeRoles(@PathVariable UUID id, @Valid @RequestBody ChangeRolesRequest req) {
        return changeRoles.handle(new ChangeUserRolesCommand(id, req.assign(), req.remove()));
    }
}