package com.logitech.sgfl.repository;

import com.logitech.sgfl.me.Motorista;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
public interface MotoristaRepository extends JpaRepository<Motorista, Long> {

    boolean existsByCpf(String cpf);

    Page<Motorista> findByNomeContainingIgnoreCaseOrCpfContaining(String nome, String cpf, Pageable pageable);
}
