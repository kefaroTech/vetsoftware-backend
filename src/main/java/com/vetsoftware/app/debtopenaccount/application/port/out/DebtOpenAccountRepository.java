package com.vetsoftware.app.debtopenaccount.application.port.out;

import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.debtopenaccount.domain.DebtOpenAccount;
import java.util.List;
import java.util.Optional;

public interface DebtOpenAccountRepository {
    DebtOpenAccount save(DebtOpenAccount debtOpenAccount);

    Optional<DebtOpenAccount> findById(Long id);

    /**
     * Lectura scoped a la empresa (vía la cuenta): evita IDOR cross-tenant al
     * consultar un abono por id directo.
     */
    Optional<DebtOpenAccount> findByIdAndCompanyId(Long id, Long companyId);

    /**
     * Toma el bloqueo pesimista sobre la FILA DEL ABONO y devuelve el id de la
     * cuenta a la que pertenece hoy. Debe invocarse como PRIMERA sentencia de todo
     * caso de uso que mute un abono ya existente (editar, borrar, anular).
     *
     * <p>
     * Existe por dos motivos que van juntos:
     *
     * <p>
     * <b>1. Es una lectura de bloqueo, no una lectura consistente.</b> Bajo
     * REPEATABLE READ —el default de MySQL, y el repositorio no configura
     * {@code isolation}— el snapshot de la transaccion lo abre la primera lectura
     * PLANA, no el BEGIN; los {@code SELECT ... FOR UPDATE} leen siempre lo ultimo
     * committeado. Cargar antes el abono con
     * {@link #findByIdAndCompanyId(Long, Long)} —que ademas trae la cuenta por
     * {@code @EntityGraph}— fijaba el snapshot Y metia la cuenta con valores viejos
     * en el contexto de persistencia, asi que el lock posterior llegaba tarde: el
     * saldo que se leia despues era el de antes de esperar al lock, y la suma de
     * abonos del recalculo tambien. Perdida de actualizacion silenciosa.
     *
     * <p>
     * <b>2. Es la unica forma de conocer la otra cuenta antes de bloquear.</b>
     * Editar un abono puede trasladarlo a otra cuenta, y entonces hay DOS cuentas
     * que bloquear; sin saber cual es la de origen no se pueden tomar los dos locks
     * en orden determinista y el primer traslado cruzado en hora punta es un
     * deadlock.
     *
     * <p>
     * <b>No va acotado por empresa, a proposito.</b> Acotarlo exige un JOIN contra
     * {@code open_accounts}, y el {@code FOR UPDATE} de MySQL bloquea las filas de
     * TODAS las tablas del join: se llevaria por delante el lock de la cuenta de
     * origen fuera del orden ascendente, que es justo el deadlock que este metodo
     * existe para evitar. Lo que se bloquea aqui es una sola fila de
     * {@code debt_open_accounts} —ninguna cuenta— y la propiedad se comprueba en la
     * sentencia siguiente con la carga acotada, que devuelve 404 y hace rollback
     * antes de leer ningun estado y antes de mutar nada.
     *
     * @return el id de la cuenta del abono, o vacio si el abono no existe (o esta
     *         deshabilitado: el {@code @SQLRestriction} de la entidad tambien
     *         aplica aqui)
     */
    Optional<Long> lockAndFindOpenAccountId(Long id);

    /**
     * Abono ya registrado con esta idempotency key en la cuenta (para deduplicar
     * reintentos).
     */
    Optional<DebtOpenAccount> findByOpenAccountIdAndClientRequestId(Long openAccountId,
            String clientRequestId);

    List<DebtOpenAccount> findAll();

    PageResult<DebtOpenAccount> findAllByCompanyId(Long companyId, int page, int pageSize);

    List<DebtOpenAccount> findByOpenAccountIdAndCompanyId(Long openAccountId, Long companyId);

    void delete(Long id, Long companyId);

    int reactivate(Long id, Long companyId);
}
