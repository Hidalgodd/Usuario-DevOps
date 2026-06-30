package com.homemed.usuario.dto;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UsuarioDTO {
    private Long id;
    private String nombre;
    private String email;
    private String rol;
}
