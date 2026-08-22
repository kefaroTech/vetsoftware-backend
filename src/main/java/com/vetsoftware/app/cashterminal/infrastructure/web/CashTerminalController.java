package com.vetsoftware.app.cashterminal.infrastructure.web;

import com.vetsoftware.app.auth.infrastructure.security.Authz;
import com.vetsoftware.app.cashterminal.application.dto.CashTerminalDto;
import com.vetsoftware.app.cashterminal.application.usecase.CashTerminalService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/cash-terminals")
public class CashTerminalController {
    private final CashTerminalService service;
    private final Authz authz;

    public CashTerminalController(CashTerminalService service, Authz authz) {
        this.service = service;
        this.authz = authz;
    }

    @GetMapping
    @PreAuthorize("hasRole('SYSTEM') or hasAuthority('cashregister.read') or"
            + " hasAuthority('cashregister.operate')")
    public List<CashTerminalDto> list(@RequestParam Long branchId,
            @RequestParam(defaultValue = "false") boolean activeOnly) {
        Long resolvedBranchId = authz.resolveAccessibleBranch(branchId);
        return service.list(authz.currentCompanyId(), resolvedBranchId, activeOnly);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('SYSTEM') or hasAuthority('branch.update')")
    public CashTerminalDto create(@Valid @RequestBody SaveTerminalRequest request) {
        Long branchId = authz.resolveAccessibleBranch(request.branchId());
        return service.create(authz.currentCompanyId(), branchId, request.name(), request.code());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SYSTEM') or hasAuthority('branch.update')")
    public CashTerminalDto update(@PathVariable Long id,
            @Valid @RequestBody UpdateTerminalRequest request) {
        return service.update(authz.currentCompanyId(), id, request.name(), request.code());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SYSTEM') or hasAuthority('branch.update')")
    public CashTerminalDto deactivate(@PathVariable Long id) {
        return service.setActive(authz.currentCompanyId(), id, false);
    }

    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasRole('SYSTEM') or hasAuthority('branch.update')")
    public CashTerminalDto activate(@PathVariable Long id) {
        return service.setActive(authz.currentCompanyId(), id, true);
    }

    public record SaveTerminalRequest(
            @NotNull(message = "Debes seleccionar la sede.") Long branchId,
            @NotBlank(message = "El nombre de la terminal es obligatorio.") @Size(max = 120, message = "El nombre de la terminal no puede superar los 120 caracteres.") String name,
            @NotBlank(message = "El código de la terminal es obligatorio.") @Size(max = 60, message = "El código de la terminal no puede superar los 60 caracteres.") String code) {
    }

    public record UpdateTerminalRequest(
            @NotBlank(message = "El nombre de la terminal es obligatorio.") @Size(max = 120, message = "El nombre de la terminal no puede superar los 120 caracteres.") String name,
            @NotBlank(message = "El código de la terminal es obligatorio.") @Size(max = 60, message = "El código de la terminal no puede superar los 60 caracteres.") String code) {
    }
}
