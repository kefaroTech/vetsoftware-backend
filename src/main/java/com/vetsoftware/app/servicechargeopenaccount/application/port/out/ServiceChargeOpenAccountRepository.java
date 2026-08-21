package com.vetsoftware.app.servicechargeopenaccount.application.port.out;

import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.servicechargeopenaccount.domain.ServiceChargeOpenAccount;
import java.util.List;
import java.util.Optional;

public interface ServiceChargeOpenAccountRepository {
    ServiceChargeOpenAccount save(ServiceChargeOpenAccount charge);

    Optional<ServiceChargeOpenAccount> findById(Long id);

    /**
     * Lectura scoped a la empresa (vía la cuenta): evita IDOR cross-tenant al
     * consultar un cargo por id directo.
     */
    Optional<ServiceChargeOpenAccount> findByIdAndCompanyId(Long id, Long companyId);

    /**
     * Cargo ya registrado con esta idempotency key en la cuenta (para deduplicar
     * reintentos).
     */
    Optional<ServiceChargeOpenAccount> findByOpenAccountIdAndClientRequestId(Long openAccountId,
            String clientRequestId);

    List<ServiceChargeOpenAccount> findAll();

    PageResult<ServiceChargeOpenAccount> findAllByCompanyId(Long companyId, int page, int pageSize);

    List<ServiceChargeOpenAccount> findByOpenAccountIdAndCompanyId(Long openAccountId,
            Long companyId);

    void delete(Long id);

    /**
     * Id de la cuenta a la que cuelga el cargo, lo VEA O NO el
     * {@code @SQLRestriction("enabled = true")} de la entidad. Va acotado por
     * empresa con el MISMO predicado que {@link #reactivate(Long, Long)} —un
     * {@code EXISTS} contra {@code open_accounts}, que es donde vive el tenant
     * porque la fila del cargo no guarda empresa—, asi que si esto resuelve, aquel
     * UPDATE casa exactamente la misma fila y un {@code rows == 0} posterior solo
     * puede ser un borrado concurrente.
     *
     * <p>
     * Existe SOLO para la reactivacion. El cargo a reactivar esta deshabilitado y
     * ningun finder JPA lo alcanza, asi que sin esta consulta no hay forma de saber
     * que cuenta hay que bloquear y comprobar ANTES de volver a encenderlo: por eso
     * la reactivacion se hacia a ciegas, sin lock y sin guarda de estado (#239).
     *
     * @return el id de la cuenta, o vacio si el cargo no existe o no es de esta
     *         empresa
     */
    Optional<Long> findOpenAccountIdIncludingDisabled(Long id, Long companyId);

    int reactivate(Long id, Long companyId);
}
