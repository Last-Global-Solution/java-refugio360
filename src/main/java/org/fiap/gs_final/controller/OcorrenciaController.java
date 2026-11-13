package org.fiap.gs_final.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.fiap.gs_final.dto.OcorrenciaRequestDTO;
import org.fiap.gs_final.model.Ocorrencia;
import org.fiap.gs_final.service.OcorrenciaService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ocorrencias")
public class OcorrenciaController {
    private final OcorrenciaService service;
    public OcorrenciaController(OcorrenciaService service) { this.service = service; }

    @PostMapping @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("hasAnyRole('CIDADAO','GESTOR')")
    public Ocorrencia criar(@Valid @RequestBody OcorrenciaRequestDTO dto) { return service.criar(dto); }

    @GetMapping @PreAuthorize("hasAnyRole('CIDADAO','GESTOR')")
    public Page<Ocorrencia> listar(@PageableDefault(size = 10, sort = "id", direction = org.springframework.data.domain.Sort.Direction.DESC) Pageable pageable) { return service.listar(pageable); }

    @GetMapping("/{id}") @PreAuthorize("hasAnyRole('CIDADAO','GESTOR')")
    public Ocorrencia buscar(@PathVariable Long id) { return service.buscar(id); }

    @PutMapping("/{id}") @PreAuthorize("hasRole('GESTOR')")
    public Ocorrencia atualizar(@PathVariable Long id, @Valid @RequestBody OcorrenciaRequestDTO dto) { return service.atualizar(id, dto); }

    @PatchMapping("/{id}/status") @PreAuthorize("hasRole('GESTOR')")
    public Ocorrencia atualizarStatus(@PathVariable Long id, @Valid @RequestBody StatusRequest body) { return service.atualizarStatus(id, body.status()); }

    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) @PreAuthorize("hasRole('GESTOR')")
    public void excluir(@PathVariable Long id) { service.excluir(id); }

    public record StatusRequest(@NotBlank String status) {}
}
