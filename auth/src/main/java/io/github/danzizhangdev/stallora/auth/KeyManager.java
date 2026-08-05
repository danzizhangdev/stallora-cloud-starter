package io.github.danzizhangdev.stallora.auth;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Holds the signing key pair.
 *
 * <p>Known limitation, deliberate for a demo: the key pair is generated at startup, so a
 * restart invalidates previously issued tokens. Resource servers re-fetch the JWKS when
 * they see an unknown {@code kid}, so they recover on their own. A real deployment loads
 * a persisted key (or a rotating pair) instead.
 */
@Component
public class KeyManager {

    private final RSAKey rsaKey;

    public KeyManager() {
        try {
            this.rsaKey = new RSAKeyGenerator(2048).keyID(UUID.randomUUID().toString()).generate();
        } catch (JOSEException e) {
            throw new IllegalStateException("Failed to generate signing key", e);
        }
    }

    public JWKSet publicJwkSet() {
        return new JWKSet(rsaKey.toPublicJWK());
    }

    public String keyId() {
        return rsaKey.getKeyID();
    }

    public JWSSigner signer() throws JOSEException {
        return new RSASSASigner(rsaKey);
    }
}
