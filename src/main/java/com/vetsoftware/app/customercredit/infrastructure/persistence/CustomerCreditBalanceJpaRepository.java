package com.vetsoftware.app.customercredit.infrastructure.persistence;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface CustomerCreditBalanceJpaRepository
        extends
            JpaRepository<CustomerCreditBalanceJpaEntity, Long> {

    Optional<CustomerCreditBalanceJpaEntity> findByCompanyId(Long companyId);

    /**
     * <strong>Mueve el saldo y comprueba que no queda en negativo en la MISMA
     * instruccion.</strong> Es la barandilla entera de esta feature.
     *
     * <p>
     * El {@code UPDATE} toma el bloqueo de la fila y lo mantiene hasta el commit,
     * de modo que dos escritores de la misma empresa quedan serializados aqui; la
     * condicion {@code balance_amount + :delta >= 0} dentro del {@code WHERE} es lo
     * que impide gastar credito que no existe. <strong>Si afecta cero filas, no hay
     * saldo y la operacion se aborta.</strong> Leer el saldo, decidir en memoria y
     * escribir despues no equivale a esto, por mucho que se parezca en una prueba
     * de un solo hilo: las dos lecturas verian cien mil, las dos escribirian
     * ochenta mil y el saldo acabaria en menos sesenta mil sin un solo error.
     *
     * <p>
     * {@code chk_ccb_not_negative} es el cinturon encima del tirante: aunque
     * alguien escriba este {@code UPDATE} sin su condicion, el motor rechaza el
     * negativo.
     *
     * <p>
     * <strong>Cumple las dos reglas duras que vigilan un {@code UPDATE}
     * masivo.</strong> {@code version = version + 1} va en el {@code SET} y nunca
     * en el {@code WHERE} ({@code UPDATE_MASIVO_MUEVE_LA_VERSION}, #53): esta tabla
     * va versionada, y sin ese incremento un {@code save} concurrente que venga de
     * una lectura anterior pisaria el cambio sin ruido; ponerlo en el {@code WHERE}
     * en cambio actualizaria cero filas y el servicio lo leeria como «no hay
     * saldo». Y el {@code WHERE} nombra la empresa
     * ({@code MUTACIONES_SQL_ACOTADAS_POR_EMPRESA}, BE-COV): aqui no hay lectura
     * previa que valide la propiedad de la fila, asi que el {@code WHERE} es toda
     * la seguridad.
     *
     * @return filas afectadas: {@code 1} si se aplico, {@code 0} si no habia saldo
     *         suficiente o la empresa no tiene fila
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Transactional
    @Query(value = """
            UPDATE customer_credit_balances
            SET balance_amount = balance_amount + :delta,
                version = version + 1,
                recalculated_at = :now
            WHERE company_id = :companyId
              AND balance_amount + :delta >= 0
            """, nativeQuery = true)
    int applyDelta(@Param("companyId") Long companyId, @Param("delta") BigDecimal delta,
            @Param("now") LocalDateTime now);

    /**
     * Abre la fila de la empresa escribiendo su cero.
     *
     * <p>
     * {@code ON DUPLICATE KEY UPDATE} con una asignacion inerte convierte la
     * carrera en un no-op: dos abonos simultaneos de la misma empresa no pueden
     * reventar contra {@code uq_ccb_company}. Es un {@code INSERT}, asi que ni la
     * regla del {@code UPDATE} masivo ni la de acotacion por empresa le aplican —
     * pero la empresa va en los valores igualmente, que es lo unico que identifica
     * la fila.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Transactional
    @Query(value = """
            INSERT INTO customer_credit_balances
                    (company_id, balance_amount, next_expiry_on, recalculated_at, version)
            VALUES (:companyId, 0, NULL, :now, 0)
            ON DUPLICATE KEY UPDATE company_id = company_id
            """, nativeQuery = true)
    void openIfAbsent(@Param("companyId") Long companyId, @Param("now") LocalDateTime now);

    /**
     * Reescribe la caducidad mas proxima. Mismo tratamiento que
     * {@link #applyDelta}: mueve {@code version} en el {@code SET} y nombra la
     * empresa en el {@code WHERE}. Sin condicion de guarda porque no mueve dinero.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Transactional
    @Query(value = """
            UPDATE customer_credit_balances
            SET next_expiry_on = :nextExpiryOn,
                version = version + 1,
                recalculated_at = :now
            WHERE company_id = :companyId
            """, nativeQuery = true)
    void refreshNextExpiry(@Param("companyId") Long companyId,
            @Param("nextExpiryOn") LocalDate nextExpiryOn, @Param("now") LocalDateTime now);
}
