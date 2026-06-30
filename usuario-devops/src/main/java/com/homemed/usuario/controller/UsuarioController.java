package com.homemed.usuario.controller;

import com.homemed.usuario.dto.UsuarioDTO;
import com.homemed.usuario.model.Usuario;
import com.homemed.usuario.service.UsuarioService;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

/**
 * CAPA DE CONTROLADOR: expone los endpoints REST del recurso "usuarios".
 * Cada método inyecta un traceId en el MDC (para Grafana Loki) y abre
 * un Span raíz en OpenTelemetry (para Grafana Tempo).
 * Las respuestas GET incluyen enlaces HATEOAS.
 */
@RestController
@RequestMapping("/usuarios")
public class UsuarioController {

    private static final Logger log = LoggerFactory.getLogger(UsuarioController.class);

    @Autowired
    private UsuarioService service;

    private final Tracer tracer = GlobalOpenTelemetry.getTracer("usuario-controller", "1.0.0");

    /**
     * GET /usuarios
     * Lista todos los usuarios con enlaces HATEOAS.
     */
    @GetMapping
    public ResponseEntity<CollectionModel<EntityModel<UsuarioDTO>>> listar() {
        long startTime = System.currentTimeMillis();
        String traceId = UUID.randomUUID().toString().replace("-", "").toLowerCase();
        MDC.put("traceId", traceId);
        MDC.put("http_path", "/usuarios");
        MDC.put("action", "LISTAR_USUARIOS");

        Span span = tracer.spanBuilder("HTTP GET /usuarios").startSpan();
        span.setAttribute("http.method", "GET");
        span.setAttribute("custom.trace_id", traceId);

        try {
            List<UsuarioDTO> items = service.obtenerTodos();
            long duration = System.currentTimeMillis() - startTime;

            MDC.put("total_usuarios", String.valueOf(items.size()));
            MDC.put("duration_ms", String.valueOf(duration));

            if (items.isEmpty()) {
                log.warn("USUARIO_EVENT - Consulta de usuarios sin resultados en la base de datos.");
            } else {
                log.info("USUARIO_EVENT - Consulta exitosa. {} usuario(s) enviados al cliente.", items.size());
            }

            List<EntityModel<UsuarioDTO>> recursos = items.stream().map(this::toModel).toList();
            CollectionModel<EntityModel<UsuarioDTO>> resultado = CollectionModel.of(
                    recursos,
                    linkTo(methodOn(UsuarioController.class).listar()).withSelfRel());

            return ResponseEntity.ok(resultado);
        } finally {
            span.end();
            MDC.clear();
        }
    }

    /**
     * GET /usuarios/{id}
     * Obtiene un usuario por id con enlaces HATEOAS.
     */
    @GetMapping("/{id}")
    public ResponseEntity<EntityModel<UsuarioDTO>> obtener(@PathVariable Long id) {
        String traceId = UUID.randomUUID().toString().replace("-", "").toLowerCase();
        MDC.put("traceId", traceId);
        MDC.put("http_path", "/usuarios/" + id);
        MDC.put("action", "OBTENER_USUARIO");

        Span span = tracer.spanBuilder("HTTP GET /usuarios/{id}").startSpan();
        span.setAttribute("http.method", "GET");
        span.setAttribute("usuario.id", id);
        span.setAttribute("custom.trace_id", traceId);

        try {
            UsuarioDTO usuario = service.obtenerPorId(id);

            if (usuario == null) {
                log.warn("USUARIO_EVENT - Usuario con id={} no encontrado.", id);
                return ResponseEntity.notFound().build();
            }

            log.info("USUARIO_EVENT - Usuario id={} encontrado y enviado al cliente.", id);
            return ResponseEntity.ok(toModel(usuario));
        } finally {
            span.end();
            MDC.clear();
        }
    }

    /**
     * POST /usuarios
     * Crea un nuevo usuario (contraseña hasheada en la capa de servicio).
     */
    @PostMapping
    public ResponseEntity<UsuarioDTO> guardar(@RequestBody Usuario usuario) {
        String traceId = UUID.randomUUID().toString().replace("-", "").toLowerCase();
        MDC.put("traceId", traceId);
        MDC.put("http_path", "/usuarios");
        MDC.put("action", "CREAR_USUARIO");

        Span span = tracer.spanBuilder("HTTP POST /usuarios").startSpan();
        span.setAttribute("http.method", "POST");
        span.setAttribute("usuario.email", usuario.getEmail());
        span.setAttribute("custom.trace_id", traceId);

        try {
            UsuarioDTO creado = service.guardar(usuario);
            log.info("USUARIO_EVENT - Usuario creado con id={} y email={}.", creado.getId(), creado.getEmail());
            return ResponseEntity.status(201).body(creado);
        } finally {
            span.end();
            MDC.clear();
        }
    }

    /**
     * DELETE /usuarios/{id}
     * Elimina un usuario por id.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        String traceId = UUID.randomUUID().toString().replace("-", "").toLowerCase();
        MDC.put("traceId", traceId);
        MDC.put("http_path", "/usuarios/" + id);
        MDC.put("action", "ELIMINAR_USUARIO");

        Span span = tracer.spanBuilder("HTTP DELETE /usuarios/{id}").startSpan();
        span.setAttribute("http.method", "DELETE");
        span.setAttribute("usuario.id", id);
        span.setAttribute("custom.trace_id", traceId);

        try {
            boolean eliminado = service.eliminar(id);

            if (!eliminado) {
                log.warn("USUARIO_EVENT - Intento de eliminar usuario id={} que no existe.", id);
                return ResponseEntity.notFound().build();
            }

            log.info("USUARIO_EVENT - Usuario id={} eliminado correctamente.", id);
            return ResponseEntity.noContent().build();
        } finally {
            span.end();
            MDC.clear();
        }
    }

    /**
     * Convierte un DTO en EntityModel con enlaces HATEOAS:
     * - self: enlace al propio recurso
     * - usuarios: enlace a la colección completa
     */
    private EntityModel<UsuarioDTO> toModel(UsuarioDTO dto) {
        return EntityModel.of(dto,
                linkTo(methodOn(UsuarioController.class).obtener(dto.getId())).withSelfRel(),
                linkTo(methodOn(UsuarioController.class).listar()).withRel("usuarios"));
    }
}
