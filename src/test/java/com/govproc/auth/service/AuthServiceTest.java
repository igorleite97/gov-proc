package com.govproc.auth.service;

import com.govproc.auth.domain.Role;
import com.govproc.auth.domain.User;
import com.govproc.auth.dto.AuthResponse;
import com.govproc.auth.dto.LoginRequest;
import com.govproc.auth.dto.RegisterRequest;
import com.govproc.auth.repository.UserRepository;
import com.govproc.shared.exception.BusinessException;
import com.govproc.shared.exception.UnauthorizedException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    UserRepository userRepository;

    @Mock
    PasswordEncoder passwordEncoder;

    @Mock
    JwtService jwtService;

    @InjectMocks
    AuthService authService;

    @Test
    void register_deveRetornarToken_quandoDadosValidos() {
        var request = new RegisterRequest("Igor", "igor@govproc.com", "senha123");
        when(userRepository.existsByEmail("igor@govproc.com")).thenReturn(false);
        when(passwordEncoder.encode("senha123")).thenReturn("hash_bcrypt");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(jwtService.generateToken(any())).thenReturn("jwt.valido.aqui");

        AuthResponse response = authService.register(request);

        assertThat(response.token()).isEqualTo("jwt.valido.aqui");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void register_deveSalvarComRoleANALYST_independenteDoRequest() {
        var request = new RegisterRequest("Igor", "igor@govproc.com", "senha123");
        when(userRepository.existsByEmail(any())).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("hash");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(jwtService.generateToken(any())).thenReturn("token");

        authService.register(request);

        verify(userRepository).save(argThat(u -> u.getRole() == Role.ANALYST));
    }

    @Test
    void register_deveLancarBusinessException_quandoEmailJaCadastrado() {
        var request = new RegisterRequest("Igor", "igor@govproc.com", "senha123");
        when(userRepository.existsByEmail("igor@govproc.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("igor@govproc.com");
    }

    @Test
    void login_deveRetornarToken_quandoCredenciaisCorretas() {
        User user = new User("Igor", "igor@govproc.com", "hash", Role.ANALYST);
        var request = new LoginRequest("igor@govproc.com", "senha123");
        when(userRepository.findByEmail("igor@govproc.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("senha123", "hash")).thenReturn(true);
        when(jwtService.generateToken(user)).thenReturn("jwt.token");

        AuthResponse response = authService.login(request);

        assertThat(response.token()).isEqualTo("jwt.token");
    }

    @Test
    void login_deveLancarUnauthorized_quandoUsuarioNaoEncontrado() {
        var request = new LoginRequest("naoexiste@govproc.com", "qualquer");
        when(userRepository.findByEmail(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void login_deveLancarUnauthorized_quandoSenhaErrada() {
        User user = new User("Igor", "igor@govproc.com", "hash", Role.ANALYST);
        var request = new LoginRequest("igor@govproc.com", "errada");
        when(userRepository.findByEmail(any())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("errada", "hash")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(UnauthorizedException.class);
    }
}
