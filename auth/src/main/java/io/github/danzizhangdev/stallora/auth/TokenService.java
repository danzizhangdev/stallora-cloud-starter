package io.github.danzizhangdev.stallora.auth;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import io.github.danzizhangdev.stallora.common.error.BusinessException;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

/** Issues RS256 JWTs signed with the key held by {@link KeyManager}. */
@Service
public class TokenService {

    private final KeyManager keys;
    private final AuthProperties props;

    public TokenService(KeyManager keys, AuthProperties props) {
        this.keys = keys;
        this.props = props;
    }

    public record IssuedToken(String token, String tokenType, long expiresInSeconds) {
    }

    public IssuedToken issue(String username, String password) {
        AuthProperties.DemoUser user = props.users().stream()
                .filter(u -> u.username().equals(username))
                // constant-time comparison: the timing of a failed login should not leak
                // how much of the password was right
                .filter(u -> MessageDigest.isEqual(u.password().getBytes(StandardCharsets.UTF_8),
                                                   password.getBytes(StandardCharsets.UTF_8)))
                .findFirst()
                .orElseThrow(() -> new BusinessException(AuthErrorCodes.INVALID_CREDENTIALS));
        try {
            Instant now = Instant.now();
            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                    .subject(user.username())
                    .issuer("stallora-auth")
                    .claim("roles", user.roles())
                    .jwtID(UUID.randomUUID().toString())
                    .issueTime(Date.from(now))
                    .expirationTime(Date.from(now.plus(props.tokenTtl())))
                    .build();
            SignedJWT jwt = new SignedJWT(
                    new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(keys.keyId()).build(), claims);
            jwt.sign(keys.signer());
            return new IssuedToken(jwt.serialize(), "Bearer", props.tokenTtl().toSeconds());
        } catch (JOSEException e) {
            throw new IllegalStateException("Failed to sign token", e);
        }
    }
}
