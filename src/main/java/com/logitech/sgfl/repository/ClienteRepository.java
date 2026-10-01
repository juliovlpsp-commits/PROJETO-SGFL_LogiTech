package com.logitech.sgfl.repository;

import com.logitech.sgfl.me.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ClienteRepository
        extends JpaRepository<Cliente, Long> {

    boolean existsByCpf(String cpf);

    boolean existsByEmail(String email);

    boolean existsByCpfAndIdNot(
            String cpf,
            Long id
    );

    boolean existsByEmailAndIdNot(
            String email,
            Long id
    );
}