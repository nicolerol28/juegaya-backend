package com.juegaya.backend.cancha;

import com.juegaya.backend.shared.EntidadAuditable;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Entity
@Table(name = "canchas")
@Getter
@Setter
@NoArgsConstructor
public class Cancha extends EntidadAuditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nombre;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Superficie superficie;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoCancha estado;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal precioBase;
}