package org.fiap.gs_final.service;

import org.fiap.gs_final.dto.LocalSeguroRequestDTO;
import org.fiap.gs_final.model.LocalSeguro;
import org.fiap.gs_final.repository.LocalSeguroRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class LocalSeguroService {
    private final LocalSeguroRepository repository;

    public LocalSeguroService(LocalSeguroRepository repository) { this.repository = repository; }

    @Transactional
    public LocalSeguro criar(LocalSeguroRequestDTO dto) {
        return repository.save(new LocalSeguro(dto.nome(), dto.tipo().trim().toUpperCase(), dto.endereco(),
                dto.latitude(), dto.longitude(), dto.descricao()));
    }

    @Transactional(readOnly = true)
    public Page<LocalSeguro> listar(Pageable pageable) { return repository.findAll(pageable); }

    @Transactional(readOnly = true)
    public LocalSeguro buscar(Long id) { return repository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Local seguro não encontrado")); }

    @Transactional
    public LocalSeguro atualizar(Long id, LocalSeguroRequestDTO dto) {
        LocalSeguro local = buscar(id);
        local.setNome(dto.nome());
        local.setTipo(dto.tipo().trim().toUpperCase());
        local.setEndereco(dto.endereco());
        local.setLatitude(dto.latitude());
        local.setLongitude(dto.longitude());
        local.setDescricao(dto.descricao());
        return repository.save(local);
    }

    @Transactional
    public void excluir(Long id) { repository.delete(buscar(id)); }
}
