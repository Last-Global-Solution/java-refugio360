package org.fiap.gs_final.service;

import org.fiap.gs_final.dto.OcorrenciaRequestDTO;
import org.fiap.gs_final.model.Ocorrencia;
import org.fiap.gs_final.repository.OcorrenciaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;
import java.util.Set;

@Service
public class OcorrenciaService {
    private static final Set<String> STATUS_VALIDOS = Set.of("PENDENTE", "EM_ANALISE", "RESOLVIDA");
    private final OcorrenciaRepository repository;

    public OcorrenciaService(OcorrenciaRepository repository) { this.repository = repository; }

    @Transactional
    public Ocorrencia criar(OcorrenciaRequestDTO dto) {
        return repository.save(new Ocorrencia(dto.descricao(), dto.urgencia().trim().toUpperCase(Locale.ROOT),
                dto.latitude(), dto.longitude()));
    }

    @Transactional(readOnly = true)
    public Page<Ocorrencia> listar(Pageable pageable) { return repository.findAll(pageable); }

    @Transactional(readOnly = true)
    public Ocorrencia buscar(Long id) { return repository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ocorrência não encontrada")); }

    @Transactional
    public Ocorrencia atualizar(Long id, OcorrenciaRequestDTO dto) {
        Ocorrencia ocorrencia = buscar(id);
        ocorrencia.setDescricao(dto.descricao());
        ocorrencia.setUrgencia(dto.urgencia().trim().toUpperCase(Locale.ROOT));
        ocorrencia.setLatitude(dto.latitude());
        ocorrencia.setLongitude(dto.longitude());
        return repository.save(ocorrencia);
    }

    @Transactional
    public Ocorrencia atualizarStatus(Long id, String status) {
        if (status == null || !STATUS_VALIDOS.contains(status.trim().toUpperCase(Locale.ROOT))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Status deve ser PENDENTE, EM_ANALISE ou RESOLVIDA");
        }
        Ocorrencia ocorrencia = buscar(id);
        ocorrencia.setStatus(status.trim().toUpperCase(Locale.ROOT));
        return repository.save(ocorrencia);
    }

    @Transactional
    public void excluir(Long id) { repository.delete(buscar(id)); }
}
