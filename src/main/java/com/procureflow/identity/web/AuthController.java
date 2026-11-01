package com.procureflow.identity.web;

import com.procureflow.identity.application.LoginHandler;
import com.procureflow.identity.application.LoginResult;
import com.procureflow.identity.application.RefreshHandler;
import com.procureflow.identity.application.RegisterUserHandler;
import com.procureflow.identity.application.command.LoginCommand;
import com.procureflow.identity.application.command.RegisterUserCommand;
import com.procureflow.identity.web.request.LoginRequest;
import com.procureflow.identity.web.request.RefreshRequest;
import com.procureflow.identity.web.request.RegisterRquest;
import com.procureflow.identity.web.response.RegisterResponse;
import com.procureflow.identity.web.response.TokenResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final RegisterUserHandler register;
    private final LoginHandler login;
    private final RefreshHandler refresh;

    public AuthController(RegisterUserHandler register, LoginHandler login, RefreshHandler refresh) {
        this.register = register;
        this.login = login;
        this.refresh = refresh;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public RegisterResponse register(@Valid @RequestBody RegisterRquest req) {
        var user = register.handle(new RegisterUserCommand(req.email(), req.password(), req.fullName()));
        return new RegisterResponse(user.id().value().toString(),
                user.email().value(), user.status().name());
    }

    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest req) {
        return toResponse(login.handle(new LoginCommand(req.email(), req.password())));
    }

    @PostMapping("/refresh")
    public TokenResponse refresh(@Valid @RequestBody RefreshRequest req) {
        return toResponse(refresh.handle(req.refreshToken()));
    }

    private static TokenResponse toResponse(LoginResult r) {
        return new TokenResponse(r.accessToken().value(), r.refreshToken(), "Bearer",
                r.accessToken().expiresAt());
    }
}