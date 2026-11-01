package com.procureflow.identity.infrastructure.security;

import com.procureflow.identity.application.port.AccessToken;
import com.procureflow.identity.application.port.AccessTokenIssuer;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.List;

@Component
public class JwtAccessTokenIssuer implements AccessTokenIssuer {

    private final JwtEncoder encoder;
    private final JwtProperties props;

    JwtAccessTokenIssuer(JwtEncoder encoder , JwtProperties props){
        this.encoder = encoder;
        this.props = props;
    }
    @Override
    public AccessToken issue(String subject, String email, Collection<String> authorities){
        Instant now = Instant.now();
        Instant expiresAt = now.plus(props.accessTtlMinutes(), ChronoUnit.MINUTES);

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(props.issuer())
                .subject(subject)
                .issuedAt(now)
                .expiresAt(expiresAt)
                .claim("email",email)
                .claim("authorities", List.copyOf(authorities))
                .build();

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = encoder.encode(JwtEncoderParameters.from(header,claims)).getTokenValue();
        return new AccessToken(token,expiresAt);
    }
}
