package com.homemed.usuario.controller;

import com.homemed.usuario.dto.LoginRequest;
import com.homemed.usuario.dto.LoginResponse;
import com.homemed.usuario.model.Usuario;
import com.homemed.usuario.security.JwtUtil;
import com.homemed.usuario.service.UsuarioService;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * CAPA DE CONTROLADOR: autenticación JWT.
 * Genera un token cuando las credenciales son válidas.
 * Incluye MDC logging (Loki) y Span OTel (Tempo) igual que el resto del proyecto.
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private JwtUtil jwtUtil;

    private final Tracer tracer = GlobalOpenTelemetry.getTracer("usuario-controller", "1.0.0");

    /**
     * POST /auth/login
     * Devuelve un JWT si las credenciales son correctas, 401 si no.
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        String traceId = UUID.randomUUID().toString().replace("-", "").toLowerCase();
        MDC.put("traceId", traceId);
        MDC.put("http_path", "/auth/login");
        MDC.put("action", "LOGIN");

        Span span = tracer.spanBuilder("HTTP POST /auth/login").startSpan();
        span.setAttribute("http.method", "POST");
        span.setAttribute("usuario.email", request.getEmail());
        span.setAttribute("custom.trace_id", traceId);

        try {
            Usuario usuario = usuarioService.autenticar(request.getEmail(), request.getPassword());

            if (usuario == null) {
                log.warn("AUTH_EVENT - Credenciales inválidas para email={}.", request.getEmail());
                return ResponseEntity.status(401).build();
            }

            String token = jwtUtil.generarToken(usuario.getId(), usuario.getEmail(), usuario.getRol());
            log.info("AUTH_EVENT - Login exitoso para usuarioId={}, rol={}.", usuario.getId(), usuario.getRol());

            return ResponseEntity.ok(new LoginResponse(token, usuario.getId(), usuario.getRol()));
        } finally {
            span.end();
            MDC.clear();
        }
    }
}
