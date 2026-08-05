package io.github.danzizhangdev.stallora.sample.client;

import io.github.danzizhangdev.stallora.common.api.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class KeyInfoController {

    private final KeyInfoService service;

    public KeyInfoController(KeyInfoService service) {
        this.service = service;
    }

    @GetMapping("/sample/key-info")
    public ApiResponse<Map<String, Object>> keyInfo() {
        return ApiResponse.ok(service.fetch());
    }
}
