package com.juegaya.backend.usuario;

import com.juegaya.backend.usuario.dto.RegistroUsuarioDTO;
import com.juegaya.backend.usuario.dto.UsuarioResponseDTO;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @PostMapping("/registro")
    public ResponseEntity<UsuarioResponseDTO> registrar(
            @Valid @RequestBody RegistroUsuarioDTO dto) {

        UsuarioResponseDTO usuarioCreado = usuarioService.registrar(dto);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(usuarioCreado);
    }
}