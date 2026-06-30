package com.homemed.usuario.service;

import com.homemed.usuario.dto.UsuarioDTO;
import com.homemed.usuario.model.Usuario;
import com.homemed.usuario.repository.UsuarioRepository;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * CAPA DE SERVICIO: lógica de negocio del microservicio usuario.
 * Cada operación abre un Span hijo de OpenTelemetry para que las trazas
 * aparezcan anidadas en Grafana Tempo.
 */
@Service
public class UsuarioService {

    @Autowired
    private UsuarioRepository repository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private final Tracer tracer = GlobalOpenTelemetry.getTracer("usuario-service", "1.0.0");

    /**
     * Lista todos los usuarios y devuelve su representación DTO.
     */
    public List<UsuarioDTO> obtenerTodos() {
        Span span = tracer.spanBuilder("UsuarioService - obtenerTodos").startSpan();
        span.setAttribute("component", "service");

        try (Scope scope = span.makeCurrent()) {
            span.addEvent("Consultando todos los usuarios en la base de datos");
            return repository.findAll()
                    .stream()
                    .map(this::toDTO)
                    .toList();
        } catch (Exception e) {
            span.recordException(e);
            throw e;
        } finally {
            span.end();
        }
    }

    /**
     * Persiste un nuevo usuario con la contraseña hasheada en BCrypt.
     */
    public UsuarioDTO guardar(Usuario usuario) {
        Span span = tracer.spanBuilder("UsuarioService - guardar").startSpan();
        span.setAttribute("component", "service");
        span.setAttribute("usuario.email", usuario.getEmail());

        try (Scope scope = span.makeCurrent()) {
            span.addEvent("Aplicando hash BCrypt a la contraseña");
            // La contraseña se almacena siempre con hash BCrypt, nunca en texto plano.
            usuario.setPassword(passwordEncoder.encode(usuario.getPassword()));

            span.addEvent("Persistiendo usuario en la base de datos");
            return toDTO(repository.save(usuario));
        } catch (Exception e) {
            span.recordException(e);
            throw e;
        } finally {
            span.end();
        }
    }

    /**
     * Busca un usuario por su ID.
     */
    public UsuarioDTO obtenerPorId(Long id) {
        Span span = tracer.spanBuilder("UsuarioService - obtenerPorId").startSpan();
        span.setAttribute("component", "service");
        span.setAttribute("usuario.id", id);

        try (Scope scope = span.makeCurrent()) {
            span.addEvent("Consultando usuario por ID");
            Usuario usuario = repository.findById(id).orElse(null);
            return usuario == null ? null : toDTO(usuario);
        } catch (Exception e) {
            span.recordException(e);
            throw e;
        } finally {
            span.end();
        }
    }

    /**
     * Elimina un usuario por su ID.
     * @return true si fue eliminado, false si no existía.
     */
    public boolean eliminar(Long id) {
        Span span = tracer.spanBuilder("UsuarioService - eliminar").startSpan();
        span.setAttribute("component", "service");
        span.setAttribute("usuario.id", id);

        try (Scope scope = span.makeCurrent()) {
            if (!repository.existsById(id)) {
                span.addEvent("Usuario no encontrado, operación cancelada");
                return false;
            }
            span.addEvent("Eliminando usuario de la base de datos");
            repository.deleteById(id);
            return true;
        } catch (Exception e) {
            span.recordException(e);
            throw e;
        } finally {
            span.end();
        }
    }

    /**
     * Verifica las credenciales para el login.
     * @return la entidad Usuario si son válidas, null si no.
     */
    public Usuario autenticar(String email, String password) {
        Span span = tracer.spanBuilder("UsuarioService - autenticar").startSpan();
        span.setAttribute("component", "service");
        span.setAttribute("usuario.email", email);

        try (Scope scope = span.makeCurrent()) {
            span.addEvent("Buscando usuario por email");
            Usuario usuario = repository.findByEmail(email).orElse(null);
            if (usuario == null) {
                span.addEvent("Usuario no encontrado");
                return null;
            }
            span.addEvent("Verificando contraseña con BCrypt");
            return passwordEncoder.matches(password, usuario.getPassword()) ? usuario : null;
        } catch (Exception e) {
            span.recordException(e);
            throw e;
        } finally {
            span.end();
        }
    }

    private UsuarioDTO toDTO(Usuario usuario) {
        return new UsuarioDTO(
                usuario.getId(),
                usuario.getNombre(),
                usuario.getEmail(),
                usuario.getRol()
        );
    }
}
