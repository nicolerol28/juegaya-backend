package com.juegaya.backend.cancha.dto;

import com.juegaya.backend.cancha.EstadoCancha;
import com.juegaya.backend.cancha.Superficie;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record ActualizarCanchaDTO(

        @NotBlank(message = "El nombre es obligatorio")
        String nombre,

        @NotNull(message = "La superficie es obligatoria")
        Superficie superficie,

        @NotNull(message = "El estado es obligatorio")
        EstadoCancha estado,

        @NotNull(message = "El precio base es obligatorio")
        @DecimalMin(value = "0.0", inclusive = false, message = "El precio debe ser mayor a 0")
        BigDecimal precioBase
) {}