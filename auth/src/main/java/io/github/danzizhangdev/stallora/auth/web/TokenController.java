package io.github.danzizhangdev.stallora.auth.web;

import io.github.danzizhangdev.stallora.auth.TokenService;
import io.github.danzizhangdev.stallora.common.api.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TokenController {

    private final TokenService tokenService;

    public TokenController(TokenService tokenService) {
        this.tokenService = tokenService;
    }

    public record TokenRequest(@NotBlank String username, @NotBlank String password) {
    }

    @PostMapping("/auth/token")
    public ApiResponse<TokenService.IssuedToken> token(@Valid @RequestBody TokenRequest req) {
        return ApiResponse.ok(tokenService.issue(req.username(), req.password()));
    }
}
