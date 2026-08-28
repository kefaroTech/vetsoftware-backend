package com.vetsoftware.app.customercredit.application.port.out;

import com.vetsoftware.app.customercredit.domain.CustomerCreditBalance;
import com.vetsoftware.app.shared.pagination.PageResult;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * La fila resumen, que es la barandilla del libro de asientos.
 *
 * <p>
 * <strong>Sin {@code save(CustomerCreditBalance)}, y esa ausencia es el diseno
 * entero.</strong> Un {@code save} invitaria a leer el saldo, decidir en
 * memoria y escribirlo despues, que es exactamente la clase de defecto que el
 * libro de asientos vino a eliminar. Lo unico que mueve el importe es
 * {@link #applyDelta}, y su condicion viaja dentro de la propia instruccion.
 */
public interface CustomerCreditBalanceRepository {

    Optional<CustomerCreditBalance> findByCompanyId(Long companyId);

    /**
     * Mueve el saldo <strong>y comprueba que no queda en negativo en la misma
     * instruccion</strong>.
     *
     * <p>
     * Devuelve las filas afectadas: <strong>cero significa que no hay
     * saldo</strong> y la operacion se aborta. No es un detalle de implementacion
     * que se pueda cambiar por una lectura previa —el bloqueo de fila que toma el
     * {@code UPDATE} es lo que serializa a los dos escritores concurrentes, y la
     * condicion del {@code WHERE} es la barandilla—.
     *
     * @param delta
     *            lo que se suma al saldo, con su signo: positivo al conceder,
     *            negativo al consumir o caducar
     * @return filas afectadas; {@code 1} si se aplico, {@code 0} si no habia saldo
     *         suficiente o la empresa no tiene fila
     */
    int applyDelta(Long companyId, BigDecimal delta, LocalDateTime now);

    /**
     * Abre la fila de una empresa que aun no tenia saldo, escribiendo su cero.
     * Idempotente: dos altas simultaneas de la misma empresa no pueden reventar
     * contra {@code uq_ccb_company}.
     */
    void openIfAbsent(Long companyId, LocalDateTime now);

    /**
     * Reescribe la caducidad mas proxima tras mover el saldo. Va aparte de
     * {@link #applyDelta} porque su valor sale de consultar el libro y no de sumar
     * un delta, y porque mezclarlas obligaria a que la barandilla del saldo
     * dependiera de una consulta que no la protege.
     */
    void refreshNextExpiry(Long companyId, LocalDate nextExpiryOn, LocalDateTime now);

    /**
     * <strong>Barrido de plataforma: sin empresa a proposito.</strong> Saldos vivos
     * de todas las clinicas —incluidas las que ya se fueron—. El indice que lo
     * sirve ({@code ix_ccb_applicable}) va sin la empresa delante por lo mismo, y
     * el unico caso de uso que lo consume esta cerrado a {@code hasRole('SYSTEM')}.
     */
    PageResult<CustomerCreditBalance> findAll(int page, int pageSize);
}
