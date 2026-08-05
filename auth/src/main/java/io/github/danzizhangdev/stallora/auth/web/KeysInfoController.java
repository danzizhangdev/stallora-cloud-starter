package io.github.danzizhangdev.stallora.auth.web;

import com.nimbusds.jose.jwk.JWK;
import io.github.danzizhangdev.stallora.auth.KeyManager;
import io.github.danzizhangdev.stallora.common.api.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * A small, boring endpoint whose only job is to be called by another service — it is the
 * target of the sample service's {@code @HttpExchange} client demo.
 */
@RestController
public class KeysInfoController {

    private final KeyManager keys;

    public KeysInfoController(KeyManager keys) {
        this.keys = keys;
    }

    @GetMapping("/auth/keys/info")
    public ApiResponse<Map<String, Object>> keysInfo() {
        return ApiResponse.ok(Map.of(
                "keyCount", keys.publicJwkSet().getKeys().size(),
                "kids", keys.publicJwkSet().getKeys().stream().map(JWK::getKeyID).toList()));
    }
}
