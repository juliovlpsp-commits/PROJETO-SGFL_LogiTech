package com.logitech.sgfl.repository;

import com.logitech.sgfl.me.AuditoriaRegistro;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface AuditoriaRegistroRepository
        extends JpaRepository<AuditoriaRegistro, Long>,
                JpaSpecificationExecutor<AuditoriaRegistro> {
}
