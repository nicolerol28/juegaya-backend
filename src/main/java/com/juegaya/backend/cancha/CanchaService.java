package com.juegaya.backend.cancha;

import com.juegaya.backend.cancha.dto.ActualizarCanchaDTO;
import com.juegaya.backend.cancha.dto.CanchaResponseDTO;
import com.juegaya.backend.cancha.dto.CrearCanchaDTO;
import com.juegaya.backend.shared.exception.RecursoNoEncontradoException;
import org.springframework.stereotype.Service;
import java.util.List;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CanchaService {

    private final CanchaRepository canchaRepository;

    public CanchaService(CanchaRepository canchaRepository) {
        this.canchaRepository = canchaRepository;
    }

    @Transactional
    public CanchaResponseDTO crear(CrearCanchaDTO dto) {
        Cancha cancha = new Cancha();
        cancha.setNombre(dto.nombre());
        cancha.setSuperficie(dto.superficie());
        cancha.setPrecioBase(dto.precioBase());
        cancha.setEstado(EstadoCancha.DISPONIBLE);

        Cancha guardada = canchaRepository.save(cancha);
        return toResponseDTO(guardada);
    }

    @Transactional(readOnly = true)
    public List<CanchaResponseDTO> listarTodas() {
        return canchaRepository.findAll()
                .stream()
                .map(this::toResponseDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CanchaResponseDTO> buscarPorNombre(String nombre) {
        return canchaRepository.findByNombreContainingIgnoreCase(nombre)
                .stream()
                .map(this::toResponseDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public CanchaResponseDTO obtenerPorId(Long id) {
        Cancha cancha = buscarEntidadPorId(id);
        return toResponseDTO(cancha);
    }

    @Transactional
    public CanchaResponseDTO actualizar(Long id, ActualizarCanchaDTO dto) {
        Cancha cancha = buscarEntidadPorId(id);
        cancha.setNombre(dto.nombre());
        cancha.setSuperficie(dto.superficie());
        cancha.setEstado(dto.estado());
        cancha.setPrecioBase(dto.precioBase());
        return toResponseDTO(cancha);
    }

    @Transactional
    public CanchaResponseDTO cambiarEstado(Long id, EstadoCancha nuevoEstado) {
        Cancha cancha = buscarEntidadPorId(id);
        cancha.setEstado(nuevoEstado);
        return toResponseDTO(cancha);
    }

    private Cancha buscarEntidadPorId(Long id) {
        return canchaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe una cancha con id " + id));
    }

    private CanchaResponseDTO toResponseDTO(Cancha cancha) {
        return new CanchaResponseDTO(
                cancha.getId(),
                cancha.getNombre(),
                cancha.getSuperficie(),
                cancha.getEstado(),
                cancha.getPrecioBase(),
                cancha.getFechaCreacion()
        );
    }
}