package com.vetsoftware.app.accountingaccount.infrastructure.persistence;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * <strong>Sin una sola {@code @Query}.</strong> Las tres consultas las expresa
 * el derivador de nombres de Spring Data, asi que aqui no hay SQL que pueda
 * olvidarse de mover la {@code version} en su {@code SET}
 * ({@code UPDATE_MASIVO_MUEVE_LA_VERSION}) ni proyectar un literal booleano
 * ({@code PROYECCION_SIN_LITERAL_BOOLEANO}). Toda escritura pasa por el ciclo
 * leer-modificar-guardar, que es el unico camino que {@code @Version} protege.
 *
 * <p>
 * <strong>Sin {@code @EntityGraph}</strong>, y no por descuido: la entidad no
 * tiene ni una asociacion —{@code parent_code} es un escalar— asi que no hay
 * N+1 que evitar.
 *
 * <p>
 * Ningun metodo recibe {@code companyId} porque la tabla no tiene esa columna.
 */
public interface AccountingAccountJpaRepository
        extends
            JpaRepository<AccountingAccountJpaEntity, Long> {

    /**
     * <strong>La igualdad la resuelve el motor bajo la colacion de la columna, que
     * es {@code ascii_bin}</strong> — byte a byte, la misma comparacion que hace
     * {@code uq_accounting_accounts_code}. Asi el {@code exists} y la unicidad no
     * pueden discrepar: un codigo con relleno no se encuentra aqui y tampoco choca
     * alli.
     */
    Optional<AccountingAccountJpaEntity> findByCode(String code);

    boolean existsByCode(String code);

    /**
     * Que la cuenta exista <b>y</b> admita asiento. Lo consulta
     * {@code accountmapping} antes de publicar un mapeo: ninguna clave foranea
     * puede comprobarlo —{@code fk_account_mappings_debit} garantiza que la cuenta
     * existe, no que sea de nivel 6— y un mapeo contra un grupo descuadra el
     * balance de prueba por arrastre sin dar un solo error.
     *
     * <p>
     * <strong>Consulta derivada y no {@code @Query}</strong>, a proposito: la
     * version en JPQL seria un {@code select case when count(a) > 0 then true …},
     * que Hibernate 7 tipa como {@code Integer} y revienta al extraer el resultado
     * ({@code PROYECCION_SIN_LITERAL_BOOLEANO}).
     */
    boolean existsByCodeAndPostableTrue(String code);

    Page<AccountingAccountJpaEntity> findAllByEnabledTrue(Pageable pageable);
}
