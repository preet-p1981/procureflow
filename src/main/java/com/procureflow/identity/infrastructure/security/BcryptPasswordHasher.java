package com.procureflow.identity.infrastructure.security;

import com.procureflow.identity.domain.port.PasswordHasher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class BcryptPasswordHasher implements PasswordHasher {

    private final PasswordEncoder encoder;

    BcryptPasswordHasher(PasswordEncoder encoder){
        this.encoder = encoder;
    }

    @Override
    public String hash(String rawPassword){
        return encoder.encode(rawPassword);
    }

    @Override
    public boolean matches(String rawPassword,String hash){
        return encoder.matches(rawPassword,hash);
    }
}
