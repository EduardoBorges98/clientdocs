package com.eduardo.clientdocs.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CreateClientRequest {

    @NotBlank(message = "Name is required")
    @Size(max = 150, message = "Name must have at most 150 characters")
    private String name;

    @NotBlank(message = "CPF/CNPJ is required")
    @Size(max = 20, message = "CPF/CNPJ must have at most 20 characters")
    private String cpfCnpj;

    @Email(message = "Email must be valid")
    @Size(max = 150, message = "Email must have at most 150 characters")
    private String email;

    public String getName() {
        return name;
    }

    public String getCpfCnpj() {
        return cpfCnpj;
    }

    public String getEmail() {
        return email;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setCpfCnpj(String cpfCnpj) {
        this.cpfCnpj = cpfCnpj;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}