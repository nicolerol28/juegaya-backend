package com.juegaya.backend.reserva;

import com.juegaya.backend.cancha.Cancha;
import com.juegaya.backend.cancha.CanchaRepository;
import com.juegaya.backend.reserva.dto.CrearReservaDTO;
import com.juegaya.backend.reserva.dto.ReservaResponseDTO;
import com.juegaya.backend.usuario.Usuario;
import com.juegaya.backend.usuario.UsuarioRepository;
import com.juegaya.backend.shared.exception.RecursoNoEncontradoException;
import com.juegaya.backend.shared.exception.ReglaDeNegocioException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
public class ReservaService {

    private static final int LIMITE_RESERVAS_ACTIVAS = 5;
    private static final LocalTime HORA_APERTURA = LocalTime.of(6, 0);
    private static final LocalTime HORA_CIERRE = LocalTime.of(22, 0);

    private final ReservaRepository reservaRepository;
    private final CanchaRepository canchaRepository;
    private final UsuarioRepository usuarioRepository;

    public ReservaService(ReservaRepository reservaRepository,
                          CanchaRepository canchaRepository,
                          UsuarioRepository usuarioRepository) {
        this.reservaRepository = reservaRepository;
        this.canchaRepository = canchaRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional
    public ReservaResponseDTO crear(Long usuarioId, CrearReservaDTO dto) {

        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe un usuario con id " + usuarioId));

        Cancha cancha = canchaRepository.findById(dto.canchaId())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe una cancha con id " + dto.canchaId()));

        if (!dto.fin().isAfter(dto.inicio())) {
            throw new ReglaDeNegocioException(
                    "La hora de fin debe ser posterior a la hora de inicio");
        }

        if (!dto.inicio().toLocalDate().equals(dto.fin().toLocalDate())) {
            throw new ReglaDeNegocioException(
                    "La reserva debe comenzar y terminar el mismo día");
        }

        LocalTime horaInicio = dto.inicio().toLocalTime();
        LocalTime horaFin = dto.fin().toLocalTime();
        if (horaInicio.isBefore(HORA_APERTURA) || horaFin.isAfter(HORA_CIERRE)) {
            throw new ReglaDeNegocioException(
                    "La reserva debe estar dentro del horario del club (6:00 a 22:00)");
        }

        if (cancha.getEstado() == com.juegaya.backend.cancha.EstadoCancha.MANTENIMIENTO) {
            throw new ReglaDeNegocioException(
                    "La cancha está en mantenimiento y no se puede reservar");
        }

        if (reservaRepository.existeChoque(cancha.getId(), dto.inicio(), dto.fin())) {
            throw new ReglaDeNegocioException(
                    "Ya existe una reserva en esa cancha para ese horario");
        }

        long activas = reservaRepository.contarReservasActivasFuturas(
                usuarioId, LocalDateTime.now());
        if (activas >= LIMITE_RESERVAS_ACTIVAS) {
            throw new ReglaDeNegocioException(
                    "Has alcanzado el límite de " + LIMITE_RESERVAS_ACTIVAS
                            + " reservas activas");
        }

        Reserva reserva = new Reserva();
        reserva.setUsuario(usuario);
        reserva.setCancha(cancha);
        reserva.setInicio(dto.inicio());
        reserva.setFin(dto.fin());
        reserva.setEstado(EstadoReserva.CONFIRMADA);
        reserva.setPrecioCobrado(cancha.getPrecioBase());

        Reserva guardada = reservaRepository.save(reserva);
        return toResponseDTO(guardada);
    }

    @Transactional(readOnly = true)
    public List<ReservaResponseDTO> listarPorUsuario(Long usuarioId) {
        if (!usuarioRepository.existsById(usuarioId)) {
            throw new RecursoNoEncontradoException(
                    "No existe un usuario con id " + usuarioId);
        }
        return reservaRepository.findByUsuarioIdConDetalles(usuarioId)
                .stream()
                .map(this::toResponseDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ReservaResponseDTO> listarTodas() {
        return reservaRepository.findAllConDetalles()
                .stream()
                .map(this::toResponseDTO)
                .toList();
    }

    @Transactional
    public ReservaResponseDTO cancelar(Long reservaId) {
        Reserva reserva = buscarEntidadPorId(reservaId);

        if (reserva.getEstado() != EstadoReserva.CONFIRMADA) {
            throw new ReglaDeNegocioException("Solo se pueden cancelar reservas confirmadas");
        }

        LocalDateTime limiteCancelacion = reserva.getInicio().minusHours(2);
        if (LocalDateTime.now().isAfter(limiteCancelacion)) {
            throw new ReglaDeNegocioException("Solo se puede cancelar hasta 2 horas antes del inicio");
        }

        reserva.setEstado(EstadoReserva.CANCELADA);
        return toResponseDTO(reserva);
    }

    @Transactional(readOnly = true)
    public ReservaResponseDTO obtenerPorId(Long id) {
        Reserva reserva = buscarEntidadPorId(id);
        return toResponseDTO(reserva);
    }

    private Reserva buscarEntidadPorId(Long id) {
        return reservaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe una reserva con id " + id));
    }

    private ReservaResponseDTO toResponseDTO(Reserva reserva) {
        return new ReservaResponseDTO(
                reserva.getId(),
                reserva.getUsuario().getId(),
                reserva.getUsuario().getNombre(),
                reserva.getCancha().getId(),
                reserva.getCancha().getNombre(),
                reserva.getInicio(),
                reserva.getFin(),
                reserva.getEstado(),
                reserva.getPrecioCobrado(),
                reserva.getFechaCreacion()
        );
    }
}