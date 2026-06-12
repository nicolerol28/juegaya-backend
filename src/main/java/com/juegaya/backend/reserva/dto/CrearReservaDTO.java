package com.juegaya.backend.reserva.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public record CrearReservaDTO(

        @NotNull(message = "La cancha es obligatoria")
        Long canchaId,

        @NotNull(message = "La fecha y hora de inicio es obligatoria")
        @Future(message = "La reserva debe ser en el futuro")
        LocalDateTime inicio,

        @NotNull(message = "La fecha y hora de fin es obligatoria")
        @Future(message = "La reserva debe ser en el futuro")
        LocalDateTime fin

) {}