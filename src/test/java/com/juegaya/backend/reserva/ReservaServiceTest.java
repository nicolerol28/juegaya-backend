package com.juegaya.backend.reserva;

import com.juegaya.backend.cancha.Cancha;
import com.juegaya.backend.cancha.CanchaRepository;
import com.juegaya.backend.cancha.EstadoCancha;
import com.juegaya.backend.reserva.dto.CrearReservaDTO;
import com.juegaya.backend.reserva.dto.ReservaResponseDTO;
import com.juegaya.backend.shared.exception.RecursoNoEncontradoException;
import com.juegaya.backend.shared.exception.ReglaDeNegocioException;
import com.juegaya.backend.usuario.Usuario;
import com.juegaya.backend.usuario.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReservaService")
class ReservaServiceTest {

    @Mock private ReservaRepository reservaRepository;
    @Mock private CanchaRepository canchaRepository;
    @Mock private UsuarioRepository usuarioRepository;

    @InjectMocks private ReservaService reservaService;

    private Usuario usuario;
    private Cancha cancha;

    private static final Long USUARIO_ID = 1L;
    private static final Long CANCHA_ID = 10L;

    @BeforeEach
    void setUp() {
        usuario = new Usuario();
        usuario.setId(USUARIO_ID);
        usuario.setNombre("Nicole");

        cancha = new Cancha();
        cancha.setId(CANCHA_ID);
        cancha.setNombre("Cancha Central");
        cancha.setEstado(EstadoCancha.DISPONIBLE);
        cancha.setPrecioBase(new BigDecimal("50000.00"));
    }

    // Franja valida:
    private CrearReservaDTO dtoValido() {
        LocalDate manana = LocalDate.now().plusDays(1);
        return new CrearReservaDTO(
                CANCHA_ID,
                manana.atTime(10, 0),
                manana.atTime(11, 0));
    }

    private void mockCaminoFeliz() {
        when(usuarioRepository.findById(USUARIO_ID)).thenReturn(java.util.Optional.of(usuario));
        when(canchaRepository.findById(CANCHA_ID)).thenReturn(java.util.Optional.of(cancha));
    }

    @Nested
    @DisplayName("crear - camino feliz")
    class CrearCaminoFeliz {

        @Test
        @DisplayName("crea la reserva como CONFIRMADA cuando todo es válido")
        void creaReservaConfirmada() {
            mockCaminoFeliz();
            when(reservaRepository.existeChoque(eq(CANCHA_ID), any(), any())).thenReturn(false);
            when(reservaRepository.contarReservasActivasFuturas(eq(USUARIO_ID), any())).thenReturn(0L);
            when(reservaRepository.save(any(Reserva.class))).thenAnswer(inv -> {
                Reserva r = inv.getArgument(0);
                r.setId(99L);
                return r;
            });

            CrearReservaDTO dto = dtoValido();
            ReservaResponseDTO res = reservaService.crear(USUARIO_ID, dto);

            assertThat(res.id()).isEqualTo(99L);
            assertThat(res.estado()).isEqualTo(EstadoReserva.CONFIRMADA);
            assertThat(res.canchaId()).isEqualTo(CANCHA_ID);
            assertThat(res.usuarioId()).isEqualTo(USUARIO_ID);
        }

        @Test
        @DisplayName("congela el precioCobrado con el precioBase de la cancha")
        void congelaPrecio() {
            mockCaminoFeliz();
            when(reservaRepository.existeChoque(eq(CANCHA_ID), any(), any())).thenReturn(false);
            when(reservaRepository.contarReservasActivasFuturas(eq(USUARIO_ID), any())).thenReturn(0L);
            when(reservaRepository.save(any(Reserva.class))).thenAnswer(inv -> inv.getArgument(0));

            ArgumentCaptor<Reserva> captor = ArgumentCaptor.forClass(Reserva.class);
            reservaService.crear(USUARIO_ID, dtoValido());

            verify(reservaRepository).save(captor.capture());
            assertThat(captor.getValue().getPrecioCobrado())
                    .isEqualByComparingTo(new BigDecimal("50000.00"));
        }
    }

    @Nested
    @DisplayName("crear - recursos inexistentes")
    class CrearRecursos {

        @Test
        @DisplayName("falla si el usuario no existe")
        void usuarioNoExiste() {
            when(usuarioRepository.findById(USUARIO_ID)).thenReturn(java.util.Optional.empty());

            assertThatThrownBy(() -> reservaService.crear(USUARIO_ID, dtoValido()))
                    .isInstanceOf(RecursoNoEncontradoException.class);

            verify(reservaRepository, never()).save(any());
        }

        @Test
        @DisplayName("falla si la cancha no existe")
        void canchaNoExiste() {
            when(usuarioRepository.findById(USUARIO_ID)).thenReturn(java.util.Optional.of(usuario));
            when(canchaRepository.findById(CANCHA_ID)).thenReturn(java.util.Optional.empty());

            assertThatThrownBy(() -> reservaService.crear(USUARIO_ID, dtoValido()))
                    .isInstanceOf(RecursoNoEncontradoException.class);

            verify(reservaRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("crear - reglas de negocio")
    class CrearReglas {

        @Test
        @DisplayName("falla si fin no es posterior a inicio")
        void finNoPosteriorAInicio() {
            mockCaminoFeliz();
            LocalDate manana = LocalDate.now().plusDays(1);
            CrearReservaDTO dto = new CrearReservaDTO(
                    CANCHA_ID, manana.atTime(11, 0), manana.atTime(11, 0));

            assertThatThrownBy(() -> reservaService.crear(USUARIO_ID, dto))
                    .isInstanceOf(ReglaDeNegocioException.class)
                    .hasMessageContaining("posterior");

            verify(reservaRepository, never()).save(any());
        }

        @Test
        @DisplayName("falla si inicio y fin son en días distintos")
        void diasDistintos() {
            mockCaminoFeliz();
            LocalDate manana = LocalDate.now().plusDays(1);
            CrearReservaDTO dto = new CrearReservaDTO(
                    CANCHA_ID,
                    manana.atTime(21, 0),
                    manana.plusDays(1).atTime(7, 0));

            assertThatThrownBy(() -> reservaService.crear(USUARIO_ID, dto))
                    .isInstanceOf(ReglaDeNegocioException.class)
                    .hasMessageContaining("mismo día");

            verify(reservaRepository, never()).save(any());
        }

        @Test
        @DisplayName("falla si inicia antes de las 6:00")
        void antesDeApertura() {
            mockCaminoFeliz();
            LocalDate manana = LocalDate.now().plusDays(1);
            CrearReservaDTO dto = new CrearReservaDTO(
                    CANCHA_ID, manana.atTime(5, 30), manana.atTime(6, 30));

            assertThatThrownBy(() -> reservaService.crear(USUARIO_ID, dto))
                    .isInstanceOf(ReglaDeNegocioException.class)
                    .hasMessageContaining("horario del club");

            verify(reservaRepository, never()).save(any());
        }

        @Test
        @DisplayName("falla si termina después de las 22:00")
        void despuesDeCierre() {
            mockCaminoFeliz();
            LocalDate manana = LocalDate.now().plusDays(1);
            CrearReservaDTO dto = new CrearReservaDTO(
                    CANCHA_ID, manana.atTime(21, 30), manana.atTime(22, 30));

            assertThatThrownBy(() -> reservaService.crear(USUARIO_ID, dto))
                    .isInstanceOf(ReglaDeNegocioException.class)
                    .hasMessageContaining("horario del club");

            verify(reservaRepository, never()).save(any());
        }

        @Test
        @DisplayName("acepta el borde exacto: 6:00 a 22:00")
        void bordeHorarioExacto() {
            mockCaminoFeliz();
            when(reservaRepository.existeChoque(eq(CANCHA_ID), any(), any())).thenReturn(false);
            when(reservaRepository.contarReservasActivasFuturas(eq(USUARIO_ID), any())).thenReturn(0L);
            when(reservaRepository.save(any(Reserva.class))).thenAnswer(inv -> inv.getArgument(0));

            LocalDate manana = LocalDate.now().plusDays(1);
            CrearReservaDTO dto = new CrearReservaDTO(
                    CANCHA_ID, manana.atTime(6, 0), manana.atTime(22, 0));

            ReservaResponseDTO res = reservaService.crear(USUARIO_ID, dto);
            assertThat(res.estado()).isEqualTo(EstadoReserva.CONFIRMADA);
        }

        @Test
        @DisplayName("falla si la cancha está en mantenimiento")
        void canchaEnMantenimiento() {
            cancha.setEstado(EstadoCancha.MANTENIMIENTO);
            mockCaminoFeliz();

            assertThatThrownBy(() -> reservaService.crear(USUARIO_ID, dtoValido()))
                    .isInstanceOf(ReglaDeNegocioException.class)
                    .hasMessageContaining("mantenimiento");

            verify(reservaRepository, never()).save(any());
        }

        @Test
        @DisplayName("falla si hay choque de horario en la cancha")
        void choqueDeHorario() {
            mockCaminoFeliz();
            when(reservaRepository.existeChoque(eq(CANCHA_ID), any(), any())).thenReturn(true);

            assertThatThrownBy(() -> reservaService.crear(USUARIO_ID, dtoValido()))
                    .isInstanceOf(ReglaDeNegocioException.class)
                    .hasMessageContaining("horario");

            verify(reservaRepository, never()).save(any());
        }

        @Test
        @DisplayName("falla si el usuario alcanzó el límite de 5 reservas activas")
        void limiteReservasActivas() {
            mockCaminoFeliz();
            when(reservaRepository.existeChoque(eq(CANCHA_ID), any(), any())).thenReturn(false);
            when(reservaRepository.contarReservasActivasFuturas(eq(USUARIO_ID), any())).thenReturn(5L);

            assertThatThrownBy(() -> reservaService.crear(USUARIO_ID, dtoValido()))
                    .isInstanceOf(ReglaDeNegocioException.class)
                    .hasMessageContaining("límite");

            verify(reservaRepository, never()).save(any());
        }

        @Test
        @DisplayName("acepta cuando tiene 4 reservas activas (justo bajo el límite)")
        void justoBajoLimite() {
            mockCaminoFeliz();
            when(reservaRepository.existeChoque(eq(CANCHA_ID), any(), any())).thenReturn(false);
            when(reservaRepository.contarReservasActivasFuturas(eq(USUARIO_ID), any())).thenReturn(4L);
            when(reservaRepository.save(any(Reserva.class))).thenAnswer(inv -> inv.getArgument(0));

            ReservaResponseDTO res = reservaService.crear(USUARIO_ID, dtoValido());
            assertThat(res.estado()).isEqualTo(EstadoReserva.CONFIRMADA);
        }
    }

    @Nested
    @DisplayName("cancelar")
    class Cancelar {

        @Test
        @DisplayName("cancela una reserva CONFIRMADA con más de 2h de anticipación")
        void cancelaConAnticipacion() {
            Reserva reserva = reservaFutura(EstadoReserva.CONFIRMADA, LocalDateTime.now().plusDays(1));
            when(reservaRepository.findById(1L)).thenReturn(java.util.Optional.of(reserva));

            ReservaResponseDTO res = reservaService.cancelar(1L);

            assertThat(res.estado()).isEqualTo(EstadoReserva.CANCELADA);
            assertThat(reserva.getEstado()).isEqualTo(EstadoReserva.CANCELADA);
        }

        @Test
        @DisplayName("falla si la reserva no está CONFIRMADA")
        void noConfirmadaNoSeCancela() {
            Reserva reserva = reservaFutura(EstadoReserva.CANCELADA, LocalDateTime.now().plusDays(1));
            when(reservaRepository.findById(1L)).thenReturn(java.util.Optional.of(reserva));

            assertThatThrownBy(() -> reservaService.cancelar(1L))
                    .isInstanceOf(ReglaDeNegocioException.class)
                    .hasMessageContaining("confirmadas");
        }

        @Test
        @DisplayName("falla si faltan menos de 2h para el inicio")
        void dentroDeVentana2h() {
            Reserva reserva = reservaFutura(EstadoReserva.CONFIRMADA, LocalDateTime.now().plusHours(1));
            when(reservaRepository.findById(1L)).thenReturn(java.util.Optional.of(reserva));

            assertThatThrownBy(() -> reservaService.cancelar(1L))
                    .isInstanceOf(ReglaDeNegocioException.class)
                    .hasMessageContaining("2 horas antes");
        }

        @Test
        @DisplayName("falla si la reserva no existe")
        void reservaNoExiste() {
            when(reservaRepository.findById(1L)).thenReturn(java.util.Optional.empty());

            assertThatThrownBy(() -> reservaService.cancelar(1L))
                    .isInstanceOf(RecursoNoEncontradoException.class);
        }

        private Reserva reservaFutura(EstadoReserva estado, LocalDateTime inicio) {
            Reserva r = new Reserva();
            r.setId(1L);
            r.setUsuario(usuario);
            r.setCancha(cancha);
            r.setInicio(inicio);
            r.setFin(inicio.plusHours(1));
            r.setEstado(estado);
            r.setPrecioCobrado(new BigDecimal("50000.00"));
            return r;
        }
    }

    @Nested
    @DisplayName("listarPorUsuario")
    class ListarPorUsuario {

        @Test
        @DisplayName("falla si el usuario no existe")
        void usuarioNoExiste() {
            when(usuarioRepository.existsById(USUARIO_ID)).thenReturn(false);

            assertThatThrownBy(() -> reservaService.listarPorUsuario(USUARIO_ID))
                    .isInstanceOf(RecursoNoEncontradoException.class);

            verify(reservaRepository, never()).findByUsuarioIdConDetalles(anyLong());
        }
    }
}