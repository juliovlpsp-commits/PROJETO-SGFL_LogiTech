package com.logitech.sgfl.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO utilizado no cadastro público de usuários.
 */
public class RegistroRequest {

    @NotBlank(message = "O username é obrigatório")
    @Size(
            max = 255,
            message = "O username deve ter no máximo 255 caracteres"
    )
    private String username;

    @NotBlank(message = "A senha é obrigatória")
    @Size(
            min = 8,
            max = 72,
            message = "A senha deve ter entre 8 e 72 caracteres"
    )
    private String password;

    public RegistroRequest() {
    }

    public RegistroRequest(
            String username,
            String password
    ) {
        this.username = username;
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(
            String username
    ) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(
            String password
    ) {
        this.password = password;
    }
}