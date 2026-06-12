package com.juegaya.backend.cancha.dto;

import com.juegaya.backend.cancha.EstadoCancha;
import com.juegaya.backend.cancha.Superficie;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CanchaResponseDTO(
        Long id,
        String nombre,
        Superficie superficie,
        EstadoCancha estado,
        BigDecimal precioBase,
        LocalDateTime fechaCreacion
) {}