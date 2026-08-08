package com.vetsoftware.app.openaccount.application.port.out;

import com.vetsoftware.app.openaccount.application.command.SearchOpenAccountsCommand;
import com.vetsoftware.app.openaccount.application.dto.PageResult;
import com.vetsoftware.app.openaccount.domain.OpenAccount;
import java.util.List;
import java.util.Optional;

public interface OpenAccountRepository {
    OpenAccount save(OpenAccount openAccount);

    Optional<OpenAccount> findById(Long id);

    /**
     * Lectura scoped a la empresa: evita IDOR cross-tenant al consultar una cuenta
     * por id directo.
     */
    Optional<OpenAccount> findByIdAndCompanyId(Long id, Long companyId);

    /**
     * Carga la cuenta con bloqueo pesimista (FOR UPDATE) para serializar el
     * recálculo de totales.
     */
    Optional<OpenAccount> findByIdForUpdate(Long id);

    /**
     * Igual que {@link #findByIdForUpdate} pero scoped a la empresa: el FOR UPDATE
     * solo bloquea la fila si pertenece a {@code companyId}. Evita que un caller (o
     * bug futuro) tome el lock pesimista sobre una cuenta ajena (defensa en
     * profundidad contra IDOR / DoS por lock cross-tenant).
     */
    Optional<OpenAccount> findByIdForUpdateAndCompanyId(Long id, Long companyId);

    List<OpenAccount> findAll();

    PageResult<OpenAccount> findAllByCompanyId(Long companyId, Long branchId, int page,
            int pageSize);

    /**
     * true si el propietario ya tiene una cuenta abierta (enabled) — regla: 1 por
     * propietario.
     */
    boolean existsActiveByOwnerIdAndBranchId(Long ownerId, Long branchId);

    PageResult<OpenAccount> search(SearchOpenAccountsCommand command);

    void delete(Long id);

    int reactivate(Long id);
}
