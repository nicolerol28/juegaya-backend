package com.juegaya.backend.cancha;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CanchaRepository extends JpaRepository<Cancha, Long> {

    List<Cancha> findByNombreContainingIgnoreCase(String nombre);

    List<Cancha> findByEstado(EstadoCancha estado);
}