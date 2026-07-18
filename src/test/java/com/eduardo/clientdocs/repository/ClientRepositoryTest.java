package com.eduardo.clientdocs.repository;

import com.eduardo.clientdocs.entity.Client;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Testcontainers
class ClientRepositoryTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("clientdocs_test")
            .withUsername("postgres")
            .withPassword("postgres");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", postgres::getDriverClassName);

        registry.add("spring.jpa.hibernate.ddl-auto", () -> "none");
        registry.add("spring.flyway.enabled", () -> "true");
    }

    @Autowired
    private ClientRepository clientRepository;

    @Test
    void shouldFindClientByCpfCnpj() {
        Client client = new Client(
                "Test Client",
                "12345678000199",
                "client@test.com"
        );

        clientRepository.save(client);

        Optional<Client> result = clientRepository.findByCpfCnpj("12345678000199");

        assertTrue(result.isPresent());
        assertEquals("Test Client", result.get().getName());
        assertEquals("12345678000199", result.get().getCpfCnpj());
        assertEquals("client@test.com", result.get().getEmail());
        assertTrue(result.get().getActive());
    }

    @Test
    void shouldReturnEmptyWhenCpfCnpjDoesNotExist() {
        Optional<Client> result = clientRepository.findByCpfCnpj("00000000000000");

        assertTrue(result.isEmpty());
    }

    @Test
    void shouldReturnTrueWhenCpfCnpjExists() {
        Client client = new Client(
                "Test Client",
                "12345678000199",
                "client@test.com"
        );

        clientRepository.save(client);

        boolean exists = clientRepository.existsByCpfCnpj("12345678000199");

        assertTrue(exists);
    }

    @Test
    void shouldReturnFalseWhenCpfCnpjDoesNotExist() {
        boolean exists = clientRepository.existsByCpfCnpj("00000000000000");

        assertFalse(exists);
    }
}