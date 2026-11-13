package org.fiap.gs_final.controller;

import org.fiap.gs_final.ai.OrientacaoService;
import org.fiap.gs_final.ai.OrientacaoService.Orientacao;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai")
public class OrientacaoController {
    private final OrientacaoService service;
    public OrientacaoController(OrientacaoService service) { this.service = service; }

    @PostMapping("/ocorrencias/{id}/orientacao")
    @PreAuthorize("hasAnyRole('CIDADAO','GESTOR')")
    public Orientacao orientar(@PathVariable Long id, @RequestParam(required = false) String perfil) { return service.orientar(id, perfil); }
}
