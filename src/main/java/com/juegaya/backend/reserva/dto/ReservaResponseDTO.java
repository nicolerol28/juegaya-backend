package com.juegaya.backend.reserva.dto;

import com.juegaya.backend.reserva.EstadoReserva;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ReservaResponseDTO(
        Long id,
        Long usuarioId,
        String usuarioNombre,
        Long canchaId,
        String canchaNombre,
        LocalDateTime inicio,
        LocalDateTime fin,
        EstadoReserva estado,
        BigDecimal precioCobrado,
        LocalDateTime fechaCreacion
) {}