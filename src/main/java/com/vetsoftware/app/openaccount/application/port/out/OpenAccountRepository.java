package com.vetsoftware.app.openaccount.application.port.out;

import com.vetsoftware.app.openaccount.application.command.SearchOpenAccountsCommand;
import com.vetsoftware.app.openaccount.application.dto.PageResult;
import com.vetsoftware.app.openaccount.domain.OpenAccount;
import java.util.List;
import java.util.Optional;

public interface OpenAccountRepository {
    OpenAccount save(OpenAccount openAccount);
    Optional<OpenAccount> findById(Long id);
    /** Lectura scoped a la empresa: evita IDOR cross-tenant al consultar una cuenta por id directo. */
    Optional<OpenAccount> findByIdAndCompanyId(Long id, Long companyId);
    /** Carga la cuenta con bloqueo pesimista (FOR UPDATE) para serializar el recálculo de totales. */
    Optional<OpenAccount> findByIdForUpdate(Long id);
    List<OpenAccount> findAll();
    List<OpenAccount> findAllByCompanyId(Long companyId);
    /** true si el propietario ya tiene una cuenta abierta (enabled) — regla: 1 por propietario. */
    boolean existsActiveByOwnerId(Long ownerId);
    PageResult<OpenAccount> search(SearchOpenAccountsCommand command);
    void delete(Long id);
    int reactivate(Long id);
}
