package com.eduardo.clientdocs.dto;

import com.eduardo.clientdocs.entity.Client;

import java.time.LocalDateTime;

public class ClientResponse {

    private Long id;
    private String name;
    private String cpfCnpj;
    private String email;
    private Boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public ClientResponse(Client client) {
        this.id = client.getId();
        this.name = client.getName();
        this.cpfCnpj = client.getCpfCnpj();
        this.email = client.getEmail();
        this.active = client.getActive();
        this.createdAt = client.getCreatedAt();
        this.updatedAt = client.getUpdatedAt();
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getCpfCnpj() {
        return cpfCnpj;
    }

    public String getEmail() {
        return email;
    }

    public Boolean getActive() {
        return active;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}