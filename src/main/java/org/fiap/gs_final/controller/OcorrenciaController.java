package org.fiap.gs_final.controller;

import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.fiap.gs_final.ai.OrientacaoService;
import org.fiap.gs_final.dto.OcorrenciaRequestDTO;
import org.fiap.gs_final.dto.OcorrenciaResponseDTO;
import org.fiap.gs_final.dto.OrientacaoResponseDTO;
import org.fiap.gs_final.security.UsuarioAtual;
import org.fiap.gs_final.service.OcorrenciaService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ocorrencias")
public class OcorrenciaController {
    private final OcorrenciaService service;
    private final OrientacaoService orientacoes;
    private final UsuarioAtual usuarioAtual;

    public OcorrenciaController(OcorrenciaService service, OrientacaoService orientacoes, UsuarioAtual usuarioAtual) {
        this.service = service;
        this.orientacoes = orientacoes;
        this.usuarioAtual = usuarioAtual;
    }

    /** Registra o relato. A orientação da IA é gerada em segundo plano, via RabbitMQ. */
    @PostMapping @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("hasAnyRole('CIDADAO','GESTOR')")
    public OcorrenciaResponseDTO criar(@Valid @RequestBody OcorrenciaRequestDTO dto,
                                       @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {
        return OcorrenciaResponseDTO.de(service.criar(dto, usuarioAtual.de(jwt)));
    }

    /** Cidadãos veem apenas seus relatos; gestor pode consultar todos. */
    @GetMapping @PreAuthorize("hasAnyRole('CIDADAO','GESTOR')")
    public Page<OcorrenciaResponseDTO> listar(@PageableDefault(size = 10, sort = "id", direction = Sort.Direction.DESC) Pageable pageable,
                                             @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {
        return service.listarPara(usuarioAtual.de(jwt), pageable).map(OcorrenciaResponseDTO::de);
    }

    /** Relatos feitos por quem está logado. */
    @GetMapping("/minhas") @PreAuthorize("hasAnyRole('CIDADAO','GESTOR')")
    public Page<OcorrenciaResponseDTO> minhas(@PageableDefault(size = 10, sort = "id", direction = Sort.Direction.DESC) Pageable pageable,
                                              @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {
        return service.listarDoAutor(usuarioAtual.de(jwt), pageable).map(OcorrenciaResponseDTO::de);
    }

    @GetMapping("/{id}") @PreAuthorize("hasAnyRole('CIDADAO','GESTOR')")
    public OcorrenciaResponseDTO buscar(@PathVariable Long id,
                                       @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {
        return OcorrenciaResponseDTO.de(service.buscarPara(id, usuarioAtual.de(jwt)));
    }

    /** Orientações já geradas para o relato (a automática e as pedidas depois). */
    @GetMapping("/{id}/orientacoes") @PreAuthorize("hasAnyRole('CIDADAO','GESTOR')")
    public Page<OrientacaoResponseDTO> orientacoes(@PathVariable Long id,
                                                   @PageableDefault(size = 10, sort = "criadaEm", direction = Sort.Direction.DESC) Pageable pageable,
                                                   @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {
        return orientacoes.listar(id, usuarioAtual.de(jwt), pageable);
    }

    @PutMapping("/{id}") @PreAuthorize("hasRole('GESTOR')")
    public OcorrenciaResponseDTO atualizar(@PathVariable Long id, @Valid @RequestBody OcorrenciaRequestDTO dto) {
        return OcorrenciaResponseDTO.de(service.atualizar(id, dto));
    }

    @PatchMapping("/{id}/status") @PreAuthorize("hasRole('GESTOR')")
    public OcorrenciaResponseDTO atualizarStatus(@PathVariable Long id, @Valid @RequestBody StatusRequest body) {
        return OcorrenciaResponseDTO.de(service.atualizarStatus(id, body.status()));
    }

    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) @PreAuthorize("hasRole('GESTOR')")
    public void excluir(@PathVariable Long id) { service.excluir(id); }

    public record StatusRequest(@NotBlank String status) {}
}
