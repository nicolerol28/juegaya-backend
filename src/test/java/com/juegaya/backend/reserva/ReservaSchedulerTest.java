package com.juegaya.backend.reserva;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReservaScheduler")
class ReservaSchedulerTest {

    @Mock private ReservaRepository reservaRepository;
    @InjectMocks private ReservaScheduler reservaScheduler;

    @Test
    @DisplayName("marca como FINALIZADA cada reserva vencida")
    void finalizaVencidas() {
        Reserva r1 = reservaConEstado(EstadoReserva.CONFIRMADA);
        Reserva r2 = reservaConEstado(EstadoReserva.CONFIRMADA);
        when(reservaRepository.findReservasParaFinalizar(any(LocalDateTime.class)))
                .thenReturn(List.of(r1, r2));

        reservaScheduler.finalizarReservasVencidas();

        assertThat(r1.getEstado()).isEqualTo(EstadoReserva.FINALIZADA);
        assertThat(r2.getEstado()).isEqualTo(EstadoReserva.FINALIZADA);
    }

    @Test
    @DisplayName("no hace nada si no hay reservas vencidas")
    void sinVencidas() {
        when(reservaRepository.findReservasParaFinalizar(any(LocalDateTime.class)))
                .thenReturn(List.of());

        reservaScheduler.finalizarReservasVencidas();

        verify(reservaRepository).findReservasParaFinalizar(any(LocalDateTime.class));
    }

    private Reserva reservaConEstado(EstadoReserva estado) {
        Reserva r = new Reserva();
        r.setEstado(estado);
        r.setInicio(LocalDateTime.now().minusHours(2));
        r.setFin(LocalDateTime.now().minusHours(1));
        return r;
    }
}