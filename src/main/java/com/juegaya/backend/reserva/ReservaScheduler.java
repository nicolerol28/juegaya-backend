package com.juegaya.backend.reserva;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Component
@Slf4j
public class ReservaScheduler {

    private final ReservaRepository reservaRepository;

    public ReservaScheduler(ReservaRepository reservaRepository) {
        this.reservaRepository = reservaRepository;
    }

    @Scheduled(fixedRate = 600000)
    @Transactional
    public void finalizarReservasVencidas() {
        List<Reserva> vencidas = reservaRepository.findReservasParaFinalizar(LocalDateTime.now());

        for (Reserva reserva : vencidas) {
            reserva.setEstado(EstadoReserva.FINALIZADA);
        }

        if (!vencidas.isEmpty()) {
            log.info("Reservas finalizadas: {}", vencidas.size());
        }
    }
}