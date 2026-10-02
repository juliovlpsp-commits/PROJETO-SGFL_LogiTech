package com.logitech.sgfl.controller;

import com.logitech.sgfl.dto.LoginRequest;
import com.logitech.sgfl.dto.RegistroRequest;
import com.logitech.sgfl.enums.Perfil;
import com.logitech.sgfl.me.Usuario;
import com.logitech.sgfl.repository.UsuarioRepository;
import com.logitech.sgfl.security.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final JwtService jwtService;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @org.springframework.beans.factory.annotation.Value("${jwt.expiration-ms:86400000}")
    private long expirationMs;

    @org.springframework.beans.factory.annotation.Value("${app.cookie.secure:false}")
    private boolean cookieSecure;

    public AuthController(
            AuthenticationManager authenticationManager,
            UserDetailsService userDetailsService,
            JwtService jwtService,
            UsuarioRepository usuarioRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.authenticationManager = authenticationManager;
        this.userDetailsService = userDetailsService;
        this.jwtService = jwtService;
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /** Autentica e grava o JWT em um cookie HttpOnly. */
    @GetMapping("/csrf")
    public CsrfToken csrf(CsrfToken csrfToken) {
        return csrfToken;
    }

    @PostMapping("/login")
    public ResponseEntity<Void> login(
            @Valid @RequestBody LoginRequest request
    ) {

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsername(),
                        request.getPassword()
                )
        );

        UserDetails userDetails =
                userDetailsService.loadUserByUsername(
                        request.getUsername()
                );

        String token =
                jwtService.generateToken(userDetails);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, criarCookie(token, expirationMs).toString())
                .build();
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, criarCookie("", 0).toString())
                .build();
    }

    @GetMapping("/session")
    public ResponseEntity<Void> session(Authentication authentication) {
        return authentication != null && authentication.isAuthenticated()
                ? ResponseEntity.noContent().build()
                : ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    private ResponseCookie criarCookie(String token, long maxAgeMs) {
        return ResponseCookie.from("sgfl_session", token)
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite("Lax")
                .path("/api")
                .maxAge(Math.max(0, maxAgeMs / 1000))
                .build();
    }

    /**
     * Cadastro público de usuário.
     *
     * Por segurança, o perfil NÃO é recebido do cliente.
     * Todo cadastro público será criado como ROLE_OPERADOR.
     */
    @PostMapping("/registrar")
    public ResponseEntity<String> registrar(
            @Valid @RequestBody RegistroRequest request
    ) {

        String username = request.getUsername().trim();

        /*
         * No fluxo atual, o username também é utilizado
         * como e-mail (o login é feito por e-mail).
         */
        if (usuarioRepository.existsByUsername(username)
                || usuarioRepository.existsByEmail(username)) {

            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body("Usuário já existe!");
        }

        Usuario usuario = new Usuario();
        usuario.setUsername(username);
        usuario.setEmail(username);
        usuario.setPassword(passwordEncoder.encode(request.getPassword()));

        /*
         * IMPORTANTE:
         * o cliente não pode escolher ROLE_ADMIN.
         */
        usuario.setPerfil(Perfil.ROLE_OPERADOR);

        usuarioRepository.save(usuario);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body("Usuário registrado com sucesso!");
    }
}
