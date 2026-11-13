package org.fiap.gs_final.service;

import org.fiap.gs_final.dto.PontoRiscoRequestDTO;
import org.fiap.gs_final.model.PontoRisco;
import org.fiap.gs_final.repository.PontoRiscoRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;

@Service
public class PontoRiscoService {
    private final PontoRiscoRepository repository;

    public PontoRiscoService(PontoRiscoRepository repository) { this.repository = repository; }

    @Transactional
    public PontoRisco criar(PontoRiscoRequestDTO dto) {
        return repository.save(new PontoRisco(dto.titulo(), dto.nivelPerigo().trim().toUpperCase(Locale.ROOT),
                dto.latitude(), dto.longitude()));
    }

    @Transactional(readOnly = true)
    public Page<PontoRisco> listar(Pageable pageable) { return repository.findAll(pageable); }

    @Transactional(readOnly = true)
    public PontoRisco buscar(Long id) { return repository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ponto de risco não encontrado")); }

    @Transactional
    public PontoRisco atualizar(Long id, PontoRiscoRequestDTO dto) {
        PontoRisco ponto = buscar(id);
        ponto.setTitulo(dto.titulo());
        ponto.setNivelPerigo(dto.nivelPerigo().trim().toUpperCase(Locale.ROOT));
        ponto.setLatitude(dto.latitude());
        ponto.setLongitude(dto.longitude());
        return repository.save(ponto);
    }

    @Transactional
    public void excluir(Long id) { repository.delete(buscar(id)); }
}
