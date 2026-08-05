package io.github.danzizhangdev.stallora.auth;

import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyType;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AuthApplicationTests {

    @Autowired
    TestRestTemplate rest;

    record TokenReq(String username, String password) {
    }

    @Test
    void validLogin_returnsVerifiableJwtWithRoles() throws Exception {
        var res = rest.postForEntity("/auth/token", new TokenReq("alice", "password-alice"), Map.class);
        assertEquals(HttpStatus.OK, res.getStatusCode());
        String token = (String) ((Map<?, ?>) res.getBody().get("data")).get("token");

        String jwksJson = rest.getForObject("/.well-known/jwks.json", String.class);
        RSAKey rsa = (RSAKey) JWKSet.parse(jwksJson).getKeys().get(0);
        SignedJWT jwt = SignedJWT.parse(token);
        assertTrue(jwt.verify(new RSASSAVerifier(rsa.toRSAPublicKey())));
        assertEquals("alice", jwt.getJWTClaimsSet().getSubject());
        assertEquals("stallora-auth", jwt.getJWTClaimsSet().getIssuer());
        assertTrue(jwt.getJWTClaimsSet().getStringListClaim("roles").contains("admin"));
    }

    @Test
    void wrongPassword_returns401WithAuthCode() {
        var res = rest.postForEntity("/auth/token", new TokenReq("alice", "wrong"), Map.class);
        assertEquals(HttpStatus.UNAUTHORIZED, res.getStatusCode());
        assertEquals("AUTH-0401", res.getBody().get("code"));
    }

    @Test
    void jwks_exposesRsaKeyWithKid() throws Exception {
        String jwksJson = rest.getForObject("/.well-known/jwks.json", String.class);
        JWK jwk = JWKSet.parse(jwksJson).getKeys().get(0);
        assertEquals(KeyType.RSA, jwk.getKeyType());
        assertNotNull(jwk.getKeyID());
    }

    @Test
    void blankUsername_returns400ValidationCode() {
        var res = rest.postForEntity("/auth/token", new TokenReq("", "x"), Map.class);
        assertEquals(HttpStatus.BAD_REQUEST, res.getStatusCode());
        assertEquals("CMN-0400", res.getBody().get("code"));
    }
}
