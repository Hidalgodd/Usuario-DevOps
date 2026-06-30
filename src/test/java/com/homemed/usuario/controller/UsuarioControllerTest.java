package com.homemed.usuario.controller;

import com.homemed.usuario.dto.UsuarioDTO;
import com.homemed.usuario.model.Usuario;
import com.homemed.usuario.service.UsuarioService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UsuarioControllerTest {

    @Mock
    private UsuarioService service;

    @InjectMocks
    private UsuarioController controller;

    // ---------- GET /usuarios ----------

    @Test
    @DisplayName("listar() debe devolver 200 con la lista de elementos cuando hay datos")
    void listar_devuelveListaConDatos() {
        when(service.obtenerTodos()).thenReturn(List.of(new UsuarioDTO(1L, "Juan Perez", "juan.perez@test.com", "PACIENTE")));

        ResponseEntity<CollectionModel<EntityModel<UsuarioDTO>>> respuesta = controller.listar();

        assertEquals(HttpStatus.OK, respuesta.getStatusCode());
        assertNotNull(respuesta.getBody());
        assertEquals(1, respuesta.getBody().getContent().size());
        verify(service, times(1)).obtenerTodos();
    }

    @Test
    @DisplayName("listar() debe devolver 200 con una colección vacía cuando no hay datos")
    void listar_devuelveListaVacia() {
        when(service.obtenerTodos()).thenReturn(Collections.emptyList());

        ResponseEntity<CollectionModel<EntityModel<UsuarioDTO>>> respuesta = controller.listar();

        assertEquals(HttpStatus.OK, respuesta.getStatusCode());
        assertTrue(respuesta.getBody().getContent().isEmpty());
        verify(service, times(1)).obtenerTodos();
    }

    // ---------- POST /usuarios ----------

    @Test
    @DisplayName("guardar() debe devolver 201 junto al recurso creado")
    void guardar_creaRecursoCorrectamente() {
        Usuario entidad = new Usuario(1L, "Juan Perez", "juan.perez@test.com", "secreta123", "PACIENTE");
        UsuarioDTO dtoEsperado = new UsuarioDTO(1L, "Juan Perez", "juan.perez@test.com", "PACIENTE");
        when(service.guardar(any(Usuario.class))).thenReturn(dtoEsperado);

        ResponseEntity<UsuarioDTO> respuesta = controller.guardar(entidad);

        assertEquals(HttpStatus.CREATED, respuesta.getStatusCode());
        assertEquals(dtoEsperado.getId(), respuesta.getBody().getId());
        verify(service, times(1)).guardar(entidad);
    }

    @Test
    @DisplayName("guardar() debe propagar el resultado devuelto por el servicio")
    void guardar_devuelveElCuerpoGeneradoPorElServicio() {
        Usuario entidad = new Usuario(1L, "Juan Perez", "juan.perez@test.com", "secreta123", "PACIENTE");
        UsuarioDTO dtoEsperado = new UsuarioDTO(1L, "Juan Perez", "juan.perez@test.com", "PACIENTE");
        when(service.guardar(entidad)).thenReturn(dtoEsperado);

        ResponseEntity<UsuarioDTO> respuesta = controller.guardar(entidad);

        assertSame(dtoEsperado, respuesta.getBody());
        verify(service, times(1)).guardar(entidad);
    }

    // ---------- GET /usuarios/{id} ----------

    @Test
    @DisplayName("obtener() debe devolver 200 cuando el recurso existe")
    void obtener_devuelveRecursoCuandoExiste() {
        UsuarioDTO dtoEsperado = new UsuarioDTO(1L, "Juan Perez", "juan.perez@test.com", "PACIENTE");
        when(service.obtenerPorId(1L)).thenReturn(dtoEsperado);

        ResponseEntity<EntityModel<UsuarioDTO>> respuesta = controller.obtener(1L);

        assertEquals(HttpStatus.OK, respuesta.getStatusCode());
        assertEquals(dtoEsperado.getId(), respuesta.getBody().getContent().getId());
        verify(service, times(1)).obtenerPorId(1L);
    }

    @Test
    @DisplayName("obtener() debe devolver 404 cuando el recurso no existe")
    void obtener_devuelve404CuandoNoExiste() {
        when(service.obtenerPorId(99L)).thenReturn(null);

        ResponseEntity<EntityModel<UsuarioDTO>> respuesta = controller.obtener(99L);

        assertEquals(HttpStatus.NOT_FOUND, respuesta.getStatusCode());
        verify(service, times(1)).obtenerPorId(99L);
    }

    // ---------- DELETE /usuarios/{id} ----------

    @Test
    @DisplayName("eliminar() debe devolver 204 cuando el recurso fue eliminado")
    void eliminar_devuelve204CuandoExiste() {
        when(service.eliminar(1L)).thenReturn(true);

        ResponseEntity<Void> respuesta = controller.eliminar(1L);

        assertEquals(HttpStatus.NO_CONTENT, respuesta.getStatusCode());
        verify(service, times(1)).eliminar(1L);
    }

    @Test
    @DisplayName("eliminar() debe devolver 404 cuando el recurso no existe")
    void eliminar_devuelve404CuandoNoExiste() {
        when(service.eliminar(99L)).thenReturn(false);

        ResponseEntity<Void> respuesta = controller.eliminar(99L);

        assertEquals(HttpStatus.NOT_FOUND, respuesta.getStatusCode());
        verify(service, times(1)).eliminar(99L);
    }
}
