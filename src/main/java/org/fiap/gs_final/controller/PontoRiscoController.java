package org.fiap.gs_final.controller;

import jakarta.validation.Valid;
import org.fiap.gs_final.dto.PontoRiscoRequestDTO;
import org.fiap.gs_final.model.PontoRisco;
import org.fiap.gs_final.service.PontoRiscoService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/pontos-risco")
public class PontoRiscoController {
    private final PontoRiscoService service;
    public PontoRiscoController(PontoRiscoService service) { this.service = service; }

    @PostMapping @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("hasRole('GESTOR')")
    public PontoRisco criar(@Valid @RequestBody PontoRiscoRequestDTO dto) { return service.criar(dto); }

    @GetMapping public Page<PontoRisco> listar(@PageableDefault(size = 10, sort = "titulo") Pageable pageable) { return service.listar(pageable); }
    @GetMapping("/{id}") public PontoRisco buscar(@PathVariable Long id) { return service.buscar(id); }

    @PutMapping("/{id}") @PreAuthorize("hasRole('GESTOR')")
    public PontoRisco atualizar(@PathVariable Long id, @Valid @RequestBody PontoRiscoRequestDTO dto) { return service.atualizar(id, dto); }

    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) @PreAuthorize("hasRole('GESTOR')")
    public void excluir(@PathVariable Long id) { service.excluir(id); }
}
