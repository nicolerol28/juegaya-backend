package com.juegaya.backend.cancha;

import com.juegaya.backend.cancha.dto.ActualizarCanchaDTO;
import com.juegaya.backend.cancha.dto.CanchaResponseDTO;
import com.juegaya.backend.cancha.dto.CrearCanchaDTO;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/canchas")
public class CanchaController {

    private final CanchaService canchaService;

    public CanchaController(CanchaService canchaService) {
        this.canchaService = canchaService;
    }

    @PostMapping
    public ResponseEntity<CanchaResponseDTO> crear(
            @Valid @RequestBody CrearCanchaDTO dto) {
        CanchaResponseDTO creada = canchaService.crear(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(creada);
    }

    @GetMapping
    public ResponseEntity<List<CanchaResponseDTO>> listarTodas() {
        return ResponseEntity.ok(canchaService.listarTodas());
    }

    @GetMapping("/buscar")
    public ResponseEntity<List<CanchaResponseDTO>> buscarPorNombre(
            @RequestParam String nombre) {
        return ResponseEntity.ok(canchaService.buscarPorNombre(nombre));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CanchaResponseDTO> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(canchaService.obtenerPorId(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CanchaResponseDTO> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody ActualizarCanchaDTO dto) {
        return ResponseEntity.ok(canchaService.actualizar(id, dto));
    }

    @PatchMapping("/{id}/estado")
    public ResponseEntity<CanchaResponseDTO> cambiarEstado(
            @PathVariable Long id,
            @RequestParam EstadoCancha estado) {
        return ResponseEntity.ok(canchaService.cambiarEstado(id, estado));
    }
}