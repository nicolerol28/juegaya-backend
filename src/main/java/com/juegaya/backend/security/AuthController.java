package com.juegaya.backend.security;

import com.juegaya.backend.security.dto.AuthResponseDTO;
import com.juegaya.backend.security.dto.LoginRequestDTO;
import com.juegaya.backend.shared.exception.AutenticacionException;
import com.juegaya.backend.shared.exception.RecursoNoEncontradoException;
import com.juegaya.backend.usuario.Usuario;
import com.juegaya.backend.usuario.UsuarioService;
import com.juegaya.backend.usuario.dto.RegistroUsuarioDTO;
import com.juegaya.backend.usuario.dto.UsuarioResponseDTO;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UsuarioService usuarioService;

    public AuthController(AuthenticationManager authenticationManager,
                          JwtService jwtService,
                          UsuarioService usuarioService) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.usuarioService = usuarioService;
    }

    @PostMapping("/registro")
    public ResponseEntity<UsuarioResponseDTO> registro(
            @Valid @RequestBody RegistroUsuarioDTO dto) {
        UsuarioResponseDTO creado = usuarioService.registrar(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDTO> login(
            @Valid @RequestBody LoginRequestDTO dto) {

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(dto.email(), dto.password()));

        Usuario usuario;
        try {
            usuario = usuarioService.buscarPorEmail(dto.email());
        } catch (RecursoNoEncontradoException e) {
            throw new AutenticacionException("Credenciales inválidas");
        }

        String token = jwtService.generarToken(usuario.getEmail(), usuario.getId());

        AuthResponseDTO respuesta = new AuthResponseDTO(
                token,
                usuario.getId(),
                usuario.getNombre(),
                usuario.getRol());

        return ResponseEntity.ok(respuesta);
    }
}