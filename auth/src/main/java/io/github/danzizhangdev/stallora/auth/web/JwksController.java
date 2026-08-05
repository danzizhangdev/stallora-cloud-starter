package io.github.danzizhangdev.stallora.auth.web;

import io.github.danzizhangdev.stallora.auth.KeyManager;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Publishes the public half of the signing key so every other service can validate
 * tokens locally, without calling back here on each request.
 */
@RestController
public class JwksController {

    private final KeyManager keys;

    public JwksController(KeyManager keys) {
        this.keys = keys;
    }

    @GetMapping(value = "/.well-known/jwks.json", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> jwks() {
        return keys.publicJwkSet().toJSONObject();
    }
}
