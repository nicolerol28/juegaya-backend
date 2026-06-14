package com.juegaya.backend.config;

import com.juegaya.backend.usuario.Rol;
import com.juegaya.backend.usuario.Usuario;
import com.juegaya.backend.usuario.UsuarioRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Slf4j
@Configuration
public class AdminSeeder {

    @Bean
    public CommandLineRunner sembrarAdmin(
            UsuarioRepository usuarioRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.admin.email}") String adminEmail,
            @Value("${app.admin.password}") String adminPassword,
            @Value("${app.admin.nombre}") String adminNombre) {

        return args -> {
            if (usuarioRepository.existsByEmail(adminEmail)) {
                log.info("Admin demo ya existe ({}), no se siembra.", adminEmail);
                return;
            }

            Usuario admin = new Usuario();
            admin.setEmail(adminEmail);
            admin.setPassword(passwordEncoder.encode(adminPassword));
            admin.setNombre(adminNombre);
            admin.setRol(Rol.ADMIN);

            usuarioRepository.save(admin);
            log.info("Admin demo agregado: {}", adminEmail);
        };
    }
}