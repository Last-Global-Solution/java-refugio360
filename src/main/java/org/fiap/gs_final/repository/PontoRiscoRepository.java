package org.fiap.gs_final.repository;

import org.fiap.gs_final.model.PontoRisco;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PontoRiscoRepository extends JpaRepository<PontoRisco, Long> {
}