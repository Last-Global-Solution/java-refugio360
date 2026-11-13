package org.fiap.gs_final.repository;

import org.fiap.gs_final.model.LocalSeguro;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LocalSeguroRepository extends JpaRepository<LocalSeguro, Long> {
}