package com.juegaya.backend.usuario;

import com.juegaya.backend.shared.exception.RecursoDuplicadoException;
import com.juegaya.backend.usuario.dto.RegistroUsuarioDTO;
import com.juegaya.backend.usuario.dto.UsuarioResponseDTO;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository usuarioRepository,
                          PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UsuarioResponseDTO registrar(RegistroUsuarioDTO dto) {

        if (usuarioRepository.existsByEmail(dto.email())) {
            throw new RecursoDuplicadoException("Ya existe un usuario con ese email");
        }

        Usuario usuario = new Usuario();
        usuario.setNombre(dto.nombre());
        usuario.setEmail(dto.email());
        usuario.setPassword(passwordEncoder.encode(dto.password()));
        usuario.setRol(Rol.CLIENTE);

        Usuario guardado = usuarioRepository.save(usuario);

        return toResponseDTO(guardado);
    }

    private UsuarioResponseDTO toResponseDTO(Usuario usuario) {
        return new UsuarioResponseDTO(
                usuario.getId(),
                usuario.getNombre(),
                usuario.getEmail(),
                usuario.getRol(),
                usuario.getFechaCreacion()
        );
    }
}