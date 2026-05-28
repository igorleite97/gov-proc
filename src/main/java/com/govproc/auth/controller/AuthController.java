package com.govproc.auth.controller;

import com.govproc.auth.dto.AuthResponse;
import com.govproc.auth.dto.LoginRequest;
import com.govproc.auth.dto.RegisterRequest;
import com.govproc.auth.service.AuthService;
import com.govproc.shared.web.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@Tag(name = "Auth", description = "Autenticação e registro de usuários")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Registrar novo usuário",
               description = "Cria um usuário com perfil ANALYST e retorna o JWT de acesso")
    public ApiResponse<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.ok(authService.register(request), "Usuário registrado com sucesso");
    }

    @PostMapping("/login")
    @Operation(summary = "Autenticar usuário",
               description = "Valida email e senha; retorna JWT válido por 24h")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok(authService.login(request));
    }
}
