package com.juegaya.backend.security.dto;

import com.juegaya.backend.usuario.Rol;

public record AuthResponseDTO(
        String token,
        Long usuarioId,
        String nombre,
        Rol rol
) {}