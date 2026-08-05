package io.github.danzizhangdev.stallora.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class GatewayApplicationTests {

    @Autowired
    RouteLocator routeLocator;

    @Autowired
    WebTestClient web;

    @Test
    void routes_containAuthAndSample() {
        List<String> ids = routeLocator.getRoutes().map(Route::getId).collectList().block();
        assertTrue(ids.contains("auth-route"));
        assertTrue(ids.contains("sample-route"));
    }

    @Test
    void protectedRoute_withoutToken_is401() {
        web.get().uri("/api/sample/notes/1").exchange().expectStatus().isUnauthorized();
    }

    @Test
    void fallback_returns503WithGatewayCode() {
        web.get().uri("/fallback/sample").exchange()
           .expectStatus().isEqualTo(503)
           .expectBody().jsonPath("$.code").isEqualTo("GW-0503");
    }
}
