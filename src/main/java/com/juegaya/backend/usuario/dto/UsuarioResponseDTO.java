package com.juegaya.backend.usuario.dto;

import com.juegaya.backend.usuario.Rol;
import java.time.LocalDateTime;

public record UsuarioResponseDTO(
        Long id,
        String nombre,
        String email,
        Rol rol,
        LocalDateTime fechaCreacion
) {}