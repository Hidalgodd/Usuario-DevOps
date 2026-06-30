package com.homemed.usuario.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LoginResponse {
    private String token;
    private String tipo = "Bearer";
    private Long usuarioId;
    private String rol;

    public LoginResponse(String token, Long usuarioId, String rol) {
        this.token = token;
        this.usuarioId = usuarioId;
        this.rol = rol;
    }
}
