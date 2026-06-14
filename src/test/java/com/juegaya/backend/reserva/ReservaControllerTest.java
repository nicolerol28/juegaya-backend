package com.juegaya.backend.reserva;

import tools.jackson.databind.ObjectMapper;
import com.juegaya.backend.reserva.dto.CrearReservaDTO;
import com.juegaya.backend.reserva.dto.ReservaResponseDTO;
import com.juegaya.backend.security.JwtService;
import com.juegaya.backend.security.SecurityConfig;
import com.juegaya.backend.security.UsuarioDetailsService;
import com.juegaya.backend.shared.exception.GlobalExceptionHandler;
import com.juegaya.backend.shared.exception.RecursoNoEncontradoException;
import com.juegaya.backend.shared.exception.ReglaDeNegocioException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ReservaController.class)
@Import({GlobalExceptionHandler.class, SecurityConfig.class})
@WithMockUser
@DisplayName("ReservaController")
class ReservaControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private ReservaService reservaService;
    @MockitoBean private JwtService jwtService;
    @MockitoBean private UsuarioDetailsService usuarioDetailsService;

    private static final Long USUARIO_ID = 1L;
    private static final Long CANCHA_ID = 10L;

    private ReservaResponseDTO ejemploResponse() {
        LocalDate manana = LocalDate.now().plusDays(1);
        return new ReservaResponseDTO(
                99L, USUARIO_ID, "Nicole", CANCHA_ID, "Cancha Central",
                manana.atTime(10, 0), manana.atTime(11, 0),
                EstadoReserva.CONFIRMADA, new BigDecimal("50000.00"),
                LocalDateTime.now());
    }

    private CrearReservaDTO dtoValido() {
        LocalDate manana = LocalDate.now().plusDays(1);
        return new CrearReservaDTO(CANCHA_ID, manana.atTime(10, 0), manana.atTime(11, 0));
    }

    @Test
    @DisplayName("POST crea reserva y devuelve 201")
    void postCrea201() throws Exception {
        when(reservaService.crear(eq(USUARIO_ID), any())).thenReturn(ejemploResponse());

        mockMvc.perform(post("/api/reservas")
                        .param("usuarioId", USUARIO_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dtoValido())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(99))
                .andExpect(jsonPath("$.estado").value("CONFIRMADA"))
                .andExpect(jsonPath("$.precioCobrado").value(50000.00));

        verify(reservaService).crear(eq(USUARIO_ID), any());
    }

    @Test
    @DisplayName("POST sin usuarioId devuelve 400")
    void postSinUsuarioId400() throws Exception {
        mockMvc.perform(post("/api/reservas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dtoValido())))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST con canchaId null devuelve 400 y lista de errores")
    void postCanchaNull400() throws Exception {
        LocalDate manana = LocalDate.now().plusDays(1);
        CrearReservaDTO invalido = new CrearReservaDTO(
                null, manana.atTime(10, 0), manana.atTime(11, 0));

        mockMvc.perform(post("/api/reservas")
                        .param("usuarioId", USUARIO_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalido)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Error de validación"))
                .andExpect(jsonPath("$.errores.canchaId").exists());
    }

    @Test
    @DisplayName("POST con fechas en el pasado dispara @Future y devuelve 400")
    void postFechasPasado400() throws Exception {
        LocalDate ayer = LocalDate.now().minusDays(1);
        CrearReservaDTO pasado = new CrearReservaDTO(
                CANCHA_ID, ayer.atTime(10, 0), ayer.atTime(11, 0));

        mockMvc.perform(post("/api/reservas")
                        .param("usuarioId", USUARIO_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(pasado)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.inicio").exists());
    }

    @Test
    @DisplayName("POST propaga 422 cuando el service lanza ReglaDeNegocioException")
    void postReglaNegocio422() throws Exception {
        when(reservaService.crear(eq(USUARIO_ID), any()))
                .thenThrow(new ReglaDeNegocioException("Ya existe una reserva en esa cancha para ese horario"));

        mockMvc.perform(post("/api/reservas")
                        .param("usuarioId", USUARIO_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dtoValido())))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.title").value("Regla de negocio violada"));
    }

    @Test
    @DisplayName("POST propaga 404 cuando el service lanza RecursoNoEncontradoException")
    void postRecursoNoEncontrado404() throws Exception {
        when(reservaService.crear(eq(USUARIO_ID), any()))
                .thenThrow(new RecursoNoEncontradoException("No existe una cancha con id 10"));

        mockMvc.perform(post("/api/reservas")
                        .param("usuarioId", USUARIO_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dtoValido())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Recurso no encontrado"));
    }

    @Test
    @DisplayName("GET lista todas devuelve 200 y serializa fechas en ISO-8601")
    void getListaTodas200() throws Exception {
        when(reservaService.listarTodas()).thenReturn(List.of(ejemploResponse()));

        mockMvc.perform(get("/api/reservas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(99))
                // ISO-8601, no array de números: confirma jackson-jsr310
                .andExpect(jsonPath("$[0].inicio").isString());
    }

    @Test
    @DisplayName("GET por usuario devuelve 200")
    void getPorUsuario200() throws Exception {
        when(reservaService.listarPorUsuario(USUARIO_ID)).thenReturn(List.of(ejemploResponse()));

        mockMvc.perform(get("/api/reservas/usuario/{usuarioId}", USUARIO_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].usuarioId").value(1));
    }

    @Test
    @DisplayName("GET por id devuelve 200")
    void getPorId200() throws Exception {
        when(reservaService.obtenerPorId(99L)).thenReturn(ejemploResponse());

        mockMvc.perform(get("/api/reservas/{id}", 99L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(99));
    }

    @Test
    @DisplayName("PATCH cancelar devuelve 200")
    void patchCancela200() throws Exception {
        ReservaResponseDTO cancelada = new ReservaResponseDTO(
                99L, USUARIO_ID, "Nicole", CANCHA_ID, "Cancha Central",
                LocalDate.now().plusDays(1).atTime(10, 0),
                LocalDate.now().plusDays(1).atTime(11, 0),
                EstadoReserva.CANCELADA, new BigDecimal("50000.00"), LocalDateTime.now());
        when(reservaService.cancelar(99L)).thenReturn(cancelada);

        mockMvc.perform(patch("/api/reservas/{id}/cancelar", 99L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CANCELADA"));
    }

    @Test
    @DisplayName("PATCH cancelar fuera de ventana propaga 422")
    void patchCancela422() throws Exception {
        when(reservaService.cancelar(99L))
                .thenThrow(new ReglaDeNegocioException("Solo se puede cancelar hasta 2 horas antes del inicio"));

        mockMvc.perform(patch("/api/reservas/{id}/cancelar", 99L))
                .andExpect(status().isUnprocessableEntity());
    }
}