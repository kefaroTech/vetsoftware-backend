package com.vetsoftware.app.subscriptionpaymentmethod.infrastructure.persistence;

import com.vetsoftware.app.subscriptionpaymentmethod.domain.MandateStatus;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SubscriptionPaymentMethodJpaRepository
        extends
            JpaRepository<SubscriptionPaymentMethodJpaEntity, Long> {

    Optional<SubscriptionPaymentMethodJpaEntity> findByIdAndCompanyId(Long id, Long companyId);

    /**
     * <strong>Sin filtro de empresa a proposito, y no es una fuga:</strong>
     * {@code uq_subscription_payment_methods_token} es una unicidad global sobre
     * {@code (gateway, token)}, asi que la unica forma de saber si un testigo ya
     * esta tomado es preguntar sin acotar. Devuelve una fila como maximo —no es un
     * listado— y el caso de uso comprueba la empresa antes de exponer nada: si el
     * testigo es de otra clinica, rechaza sin revelar de quien.
     */
    Optional<SubscriptionPaymentMethodJpaEntity> findByGatewayAndToken(String gateway,
            String token);

    Page<SubscriptionPaymentMethodJpaEntity> findAllByCompanyId(Long companyId, Pageable pageable);

    /**
     * Tarjetas con mandato vivo que caducan antes de la fecha dada.
     *
     * <p>
     * Cross-tenant a proposito: es el barrido que sostiene el aviso previo, y su
     * indice ({@code ix_subscription_payment_methods_expiring}) va sin la empresa
     * delante justo para esto. Solo lo alcanza un caso de uso cerrado a
     * {@code hasRole('SYSTEM')}.
     *
     * <p>
     * Filtra por {@code ACTIVE} porque un mandato ya revocado o ya marcado como
     * caducado no tiene a quien avisar: reaparecerian en cada pasada del barrido.
     */
    @Query("""
            select m from SubscriptionPaymentMethodJpaEntity m
            where m.expiresOn is not null
              and m.expiresOn < :before
              and m.mandateStatus = :active
            """)
    Page<SubscriptionPaymentMethodJpaEntity> findAllExpiringBefore(
            @Param("before") LocalDate before, @Param("active") MandateStatus active,
            Pageable pageable);

    /**
     * Le quita la marca de predeterminado a los medios vigentes de una empresa,
     * excluyendo el que se esta marcando.
     *
     * <p>
     * <strong>Acotado por empresa en el {@code WHERE}</strong>
     * ({@code MUTACIONES_SQL_ACOTADAS_POR_EMPRESA}): aqui no hay lectura previa que
     * valide la propiedad de las filas, asi que el {@code WHERE} es toda la
     * seguridad. Sin {@code company_id}, esta instruccion le quitaria el
     * predeterminado a las quinientas clinicas.
     *
     * <p>
     * <strong>Y mueve {@code version} en el {@code SET}</strong>
     * ({@code UPDATE_MASIVO_MUEVE_LA_VERSION}, #53): un {@code UPDATE} por
     * {@code @Query} va directo a la base sin pasar por el ciclo
     * leer-modificar-guardar, asi que ni comprueba ni incrementa la version. Sin
     * esta linea, un {@code save} concurrente que venga de una lectura anterior
     * casaria con la version vieja y desharia la limpieza en silencio. Va en el
     * {@code SET} y nunca en el {@code WHERE}: ahi solo conseguiria actualizar cero
     * filas.
     *
     * <p>
     * Solo toca los mandatos {@code ACTIVE}: en los revocados
     * {@code default_marker} ya vale {@code NULL} —el hueco esta libre— y su
     * {@code is_default} es el rastro de cual lo fue.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update SubscriptionPaymentMethodJpaEntity m
               set m.defaultMethod = false,
                   m.version = m.version + 1
             where m.companyId = :companyId
               and m.defaultMethod = true
               and m.mandateStatus = :active
               and m.id <> :excludedId
            """)
    int clearDefaultForCompany(@Param("companyId") Long companyId,
            @Param("excludedId") Long excludedId, @Param("active") MandateStatus active);
}
