package com.juegaya.backend.reserva;

import com.juegaya.backend.cancha.Cancha;
import com.juegaya.backend.cancha.EstadoCancha;
import com.juegaya.backend.cancha.Superficie;
import com.juegaya.backend.usuario.Rol;
import com.juegaya.backend.usuario.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@DisplayName("ReservaRepository")
class ReservaRepositoryTest {

    @Autowired private ReservaRepository reservaRepository;
    @Autowired private TestEntityManager em;

    private Usuario usuario;
    private Cancha cancha;
    private final LocalDate manana = LocalDate.now().plusDays(1);

    @BeforeEach
    void setUp() {
        usuario = new Usuario();
        usuario.setNombre("Nicole");
        usuario.setEmail("nicole@test.com");
        usuario.setPassword("hash");
        usuario.setRol(Rol.CLIENTE);
        em.persist(usuario);

        cancha = new Cancha();
        cancha.setNombre("Cancha Central");
        cancha.setSuperficie(Superficie.ARCILLA);
        cancha.setEstado(EstadoCancha.DISPONIBLE);
        cancha.setPrecioBase(new BigDecimal("50000.00"));
        em.persist(cancha);
    }

    private void crearReserva(LocalDateTime inicio, LocalDateTime fin, EstadoReserva estado) {
        Reserva r = new Reserva();
        r.setUsuario(usuario);
        r.setCancha(cancha);
        r.setInicio(inicio);
        r.setFin(fin);
        r.setEstado(estado);
        r.setPrecioCobrado(new BigDecimal("50000.00"));
        em.persist(r);
    }

    @Test
    @DisplayName("detecta solapamiento parcial")
    void solapamientoParcial() {
        crearReserva(manana.atTime(10, 0), manana.atTime(11, 0), EstadoReserva.CONFIRMADA);
        em.flush();

        boolean choca = reservaRepository.existeChoque(
                cancha.getId(), manana.atTime(10, 30), manana.atTime(11, 30));

        assertThat(choca).isTrue();
    }

    @Test
    @DisplayName("NO choca cuando se tocan exactamente en el borde")
    void bordeExactoNoChoca() {
        crearReserva(manana.atTime(10, 0), manana.atTime(11, 0), EstadoReserva.CONFIRMADA);
        em.flush();

        boolean choca = reservaRepository.existeChoque(
                cancha.getId(), manana.atTime(11, 0), manana.atTime(12, 0));

        assertThat(choca).isFalse();
    }

    @Test
    @DisplayName("detecta cuando una reserva contiene completamente a la otra")
    void unaContieneAOtra() {
        crearReserva(manana.atTime(10, 0), manana.atTime(12, 0), EstadoReserva.CONFIRMADA);
        em.flush();

        boolean choca = reservaRepository.existeChoque(
                cancha.getId(), manana.atTime(10, 30), manana.atTime(11, 0));

        assertThat(choca).isTrue();
    }

    @Test
    @DisplayName("NO choca cuando no hay solapamiento temporal")
    void sinSolapamiento() {
        crearReserva(manana.atTime(10, 0), manana.atTime(11, 0), EstadoReserva.CONFIRMADA);
        em.flush();

        boolean choca = reservaRepository.existeChoque(
                cancha.getId(), manana.atTime(14, 0), manana.atTime(15, 0));

        assertThat(choca).isFalse();
    }

    @Test
    @DisplayName("ignora reservas CANCELADAS al detectar choque")
    void ignoraCanceladas() {
        crearReserva(manana.atTime(10, 0), manana.atTime(11, 0), EstadoReserva.CANCELADA);
        em.flush();

        boolean choca = reservaRepository.existeChoque(
                cancha.getId(), manana.atTime(10, 0), manana.atTime(11, 0));

        assertThat(choca).isFalse();
    }

    @Test
    @DisplayName("cuenta solo reservas CONFIRMADAS futuras del usuario")
    void contarActivasFuturas() {
        crearReserva(manana.atTime(10, 0), manana.atTime(11, 0), EstadoReserva.CONFIRMADA);
        crearReserva(manana.atTime(12, 0), manana.atTime(13, 0), EstadoReserva.CONFIRMADA);
        crearReserva(manana.atTime(14, 0), manana.atTime(15, 0), EstadoReserva.CANCELADA);
        em.flush();

        long activas = reservaRepository.contarReservasActivasFuturas(
                usuario.getId(), LocalDateTime.now());

        assertThat(activas).isEqualTo(2);
    }

    @Test
    @DisplayName("encuentra reservas CONFIRMADAS ya vencidas para finalizar")
    void reservasParaFinalizar() {
        LocalDate ayer = LocalDate.now().minusDays(1);
        crearReserva(ayer.atTime(10, 0), ayer.atTime(11, 0), EstadoReserva.CONFIRMADA);
        crearReserva(manana.atTime(10, 0), manana.atTime(11, 0), EstadoReserva.CONFIRMADA);
        em.flush();

        var paraFinalizar = reservaRepository.findReservasParaFinalizar(LocalDateTime.now());

        assertThat(paraFinalizar).hasSize(1);
        assertThat(paraFinalizar.get(0).getInicio().toLocalDate()).isEqualTo(ayer);
    }
}