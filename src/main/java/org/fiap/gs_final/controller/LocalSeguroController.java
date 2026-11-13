package org.fiap.gs_final.controller;

import jakarta.validation.Valid;
import org.fiap.gs_final.dto.LocalSeguroRequestDTO;
import org.fiap.gs_final.model.LocalSeguro;
import org.fiap.gs_final.service.LocalSeguroService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/locais-seguros")
public class LocalSeguroController {
    private final LocalSeguroService service;
    public LocalSeguroController(LocalSeguroService service) { this.service = service; }

    @PostMapping @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("hasRole('GESTOR')")
    public LocalSeguro criar(@Valid @RequestBody LocalSeguroRequestDTO dto) { return service.criar(dto); }

    @GetMapping
    public Page<LocalSeguro> listar(@PageableDefault(size = 10, sort = "nome") Pageable pageable) { return service.listar(pageable); }

    @GetMapping("/{id}") public LocalSeguro buscar(@PathVariable Long id) { return service.buscar(id); }

    @PutMapping("/{id}") @PreAuthorize("hasRole('GESTOR')")
    public LocalSeguro atualizar(@PathVariable Long id, @Valid @RequestBody LocalSeguroRequestDTO dto) { return service.atualizar(id, dto); }

    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) @PreAuthorize("hasRole('GESTOR')")
    public void excluir(@PathVariable Long id) { service.excluir(id); }
}
