package com.bookly.api.auth;

import com.bookly.api.auth.dto.LoginRequest;
import com.bookly.api.auth.dto.RefreshRequest;
import com.bookly.api.auth.dto.RegisterRequest;
import com.bookly.api.auth.dto.TokenPair;
import com.bookly.domain.user.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public TokenPair register(@RequestBody @Valid RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public TokenPair login(@RequestBody @Valid LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/refresh")
    public TokenPair refresh(@RequestBody @Valid RefreshRequest request) {
        return authService.refresh(request.refreshToken());
    }
}
