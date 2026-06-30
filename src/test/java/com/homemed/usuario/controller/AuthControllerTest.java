package com.homemed.usuario.controller;

import com.homemed.usuario.dto.LoginRequest;
import com.homemed.usuario.dto.LoginResponse;
import com.homemed.usuario.model.Usuario;
import com.homemed.usuario.security.JwtUtil;
import com.homemed.usuario.service.UsuarioService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private UsuarioService usuarioService;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private AuthController controller;

    @Test
    @DisplayName("login() debe devolver 200 y un token cuando las credenciales son válidas")
    void login_devuelveTokenConCredencialesValidas() {
        Usuario usuario = new Usuario(1L, "Juan Perez", "juan@test.com", "hash", "PACIENTE");
        when(usuarioService.autenticar("juan@test.com", "secreta123")).thenReturn(usuario);
        when(jwtUtil.generarToken(1L, "juan@test.com", "PACIENTE")).thenReturn("token-falso");

        ResponseEntity<LoginResponse> respuesta = controller.login(new LoginRequest("juan@test.com", "secreta123"));

        assertEquals(HttpStatus.OK, respuesta.getStatusCode());
        assertEquals("token-falso", respuesta.getBody().getToken());
        verify(usuarioService, times(1)).autenticar("juan@test.com", "secreta123");
    }

    @Test
    @DisplayName("login() debe devolver 401 cuando las credenciales son inválidas")
    void login_devuelve401ConCredencialesInvalidas() {
        when(usuarioService.autenticar("juan@test.com", "incorrecta")).thenReturn(null);

        ResponseEntity<LoginResponse> respuesta = controller.login(new LoginRequest("juan@test.com", "incorrecta"));

        assertEquals(HttpStatus.UNAUTHORIZED, respuesta.getStatusCode());
        verify(jwtUtil, never()).generarToken(any(), any(), any());
    }
}
