package org.fiap.gs_final.service;

import org.fiap.gs_final.dto.OcorrenciaRequestDTO;
import org.fiap.gs_final.mensageria.OcorrenciaRegistradaEvent;
import org.fiap.gs_final.model.Ocorrencia;
import org.fiap.gs_final.model.StatusOcorrencia;
import org.fiap.gs_final.model.Usuario;
import org.fiap.gs_final.repository.OcorrenciaRepository;
import org.fiap.gs_final.security.UsuarioAtual;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;

@Service
public class OcorrenciaService {
    private final OcorrenciaRepository repository;
    private final ApplicationEventPublisher eventos;

    public OcorrenciaService(OcorrenciaRepository repository, ApplicationEventPublisher eventos) {
        this.repository = repository;
        this.eventos = eventos;
    }

    /**
     * Registra o relato ligado ao autor e anuncia o evento. A publicação no
     * RabbitMQ acontece depois do commit (OcorrenciaEventPublisher), e a
     * orientação da IA é gerada pelo consumidor da fila.
     */
    @Transactional
    public Ocorrencia criar(OcorrenciaRequestDTO dto, Usuario autor) {
        Ocorrencia salva = repository.save(new Ocorrencia(dto.descricao(), dto.urgencia().trim().toUpperCase(Locale.ROOT),
                dto.latitude(), dto.longitude(), perfil(dto.perfil()), autor));
        eventos.publishEvent(new OcorrenciaRegistradaEvent(salva.getId(), salva.getPerfil()));
        return salva;
    }

    @Transactional(readOnly = true)
    public Page<Ocorrencia> listarPara(Usuario solicitante, Pageable pageable) {
        if (solicitante == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Autenticação necessária");
        return UsuarioAtual.ehGestor(solicitante) ? repository.findAll(pageable)
                : repository.findByAutorId(solicitante.getId(), pageable);
    }

    @Transactional(readOnly = true)
    public Page<Ocorrencia> listarDoAutor(Usuario autor, Pageable pageable) {
        return repository.findByAutorId(autor.getId(), pageable);
    }

    @Transactional(readOnly = true)
    public Ocorrencia buscar(Long id) { return repository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ocorrência não encontrada")); }

    @Transactional(readOnly = true)
    public Ocorrencia buscarPara(Long id, Usuario solicitante) {
        if (solicitante == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Autenticação necessária");
        Ocorrencia ocorrencia = buscar(id);
        if (UsuarioAtual.ehGestor(solicitante) || ocorrencia.pertenceA(solicitante)) return ocorrencia;
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Esta ocorrência pertence a outra pessoa ou não possui autor");
    }

    @Transactional
    public Ocorrencia atualizar(Long id, OcorrenciaRequestDTO dto) {
        Ocorrencia ocorrencia = buscar(id);
        ocorrencia.setDescricao(dto.descricao());
        ocorrencia.setUrgencia(dto.urgencia().trim().toUpperCase(Locale.ROOT));
        ocorrencia.setLatitude(dto.latitude());
        ocorrencia.setLongitude(dto.longitude());
        if (dto.perfil() != null) ocorrencia.setPerfil(perfil(dto.perfil()));
        return repository.save(ocorrencia);
    }

    /** O status segue o ciclo de {@link StatusOcorrencia}; pular etapa ou reabrir resolvida dá 409. */
    @Transactional
    public Ocorrencia atualizarStatus(Long id, String status) {
        StatusOcorrencia destino = StatusOcorrencia.deTexto(status);
        if (destino == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Status deve ser PENDENTE, EM_ANALISE ou RESOLVIDA");
        }
        Ocorrencia ocorrencia = buscar(id);
        if (!ocorrencia.moverPara(destino)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Não é possível passar de " + ocorrencia.getStatus() + " para " + destino);
        }
        return repository.save(ocorrencia);
    }

    @Transactional
    public void excluir(Long id) { repository.delete(buscar(id)); }

    private static String perfil(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim().toUpperCase(Locale.ROOT);
    }
}
