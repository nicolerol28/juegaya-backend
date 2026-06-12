package com.juegaya.backend.reserva;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;

public interface ReservaRepository extends JpaRepository<Reserva, Long> {

    // Existe alguna reserva activa que choque con la franja (inicio, fin) en esta cancha?
    @Query("""
           SELECT COUNT(r) > 0 FROM Reserva r
           WHERE r.cancha.id = :canchaId
             AND r.estado = com.juegaya.backend.reserva.EstadoReserva.CONFIRMADA
             AND r.inicio < :fin
             AND r.fin > :inicio
           """)
    boolean existeChoque(
            @Param("canchaId") Long canchaId,
            @Param("inicio") LocalDateTime inicio,
            @Param("fin") LocalDateTime fin);

    List<Reserva> findByUsuarioId(Long usuarioId);

    // Contar reservas activas futuras de un usuario (límite de 5)
    @Query("""
           SELECT COUNT(r) FROM Reserva r
           WHERE r.usuario.id = :usuarioId
             AND r.estado = com.juegaya.backend.reserva.EstadoReserva.CONFIRMADA
             AND r.inicio > :ahora
           """)
    long contarReservasActivasFuturas(
            @Param("usuarioId") Long usuarioId,
            @Param("ahora") LocalDateTime ahora);

    // Reservas confirmadas que deben pasar a finalizada (fin < ahora)
    @Query("""
           SELECT r FROM Reserva r
           WHERE r.estado = com.juegaya.backend.reserva.EstadoReserva.CONFIRMADA
             AND r.fin < :ahora
           """)
    List<Reserva> findReservasParaFinalizar(@Param("ahora") LocalDateTime ahora);

    @Query("""
       SELECT r FROM Reserva r
       JOIN FETCH r.usuario
       JOIN FETCH r.cancha
       """)
    List<Reserva> findAllConDetalles();

    @Query("""
       SELECT r FROM Reserva r
       JOIN FETCH r.usuario
       JOIN FETCH r.cancha
       WHERE r.usuario.id = :usuarioId
       """)
    List<Reserva> findByUsuarioIdConDetalles(@Param("usuarioId") Long usuarioId);

}