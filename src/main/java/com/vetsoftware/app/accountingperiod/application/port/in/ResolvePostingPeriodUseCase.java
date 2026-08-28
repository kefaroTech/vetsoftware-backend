package com.vetsoftware.app.accountingperiod.application.port.in;

import com.vetsoftware.app.accountingperiod.application.dto.AccountingPeriodDto;
import java.time.LocalDate;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * En que mes se registra un hecho que ocurrio en una fecha dada.
 *
 * <p>
 * <strong>Es el puerto que da sentido a toda la ficha</strong>: sin el, «los
 * periodos se cierran» seria una etiqueta sin consecuencia, porque quien
 * registra una conciliacion seis meses tarde seguiria escribiendo la fecha del
 * hecho y alterando un mes ya declarado. Con el, la fecha del hecho decide
 * <em>donde empieza a buscar</em>, no donde se registra.
 */
public interface ResolvePostingPeriodUseCase {

    /**
     * El mes en el que se puede registrar un hecho ocurrido en {@code occurredOn}:
     * ese mismo mes si esta abierto, y si no <strong>el primer mes abierto
     * POSTERIOR</strong>.
     *
     * <h2>Nunca hacia atras, y esa es toda la regla</h2>
     *
     * <p>
     * Buscar hacia atras un mes abierto seria lo intuitivo —«llevalo al ultimo mes
     * que todavia admite escrituras»— y es exactamente lo que no se puede hacer:
     * <strong>el informe de marzo tiene que seguir dando lo que se declaro en
     * marzo</strong>. Un ajuste que aparece en marzo despues de haberlo cerrado
     * cambia un numero que ya salio de la empresa, y el fallo es silencioso por
     * definicion — no hay error, hay una cifra distinta en dos documentos que
     * deberian coincidir, descubierta meses despues por quien los compara.
     * Imputarlo al primer mes abierto posterior es la practica contable normal: el
     * hecho se reconoce cuando se supo, no cuando ocurrio.
     *
     * <p>
     * <strong>{@code hasRole('SYSTEM')} a secas</strong>, igual que el resto de la
     * feature: la tabla no tiene empresa. Y es una lectura, no una escritura —
     * resolver el periodo no abre ni cierra nada.
     *
     * @throws com.vetsoftware.app.accountingperiod.domain.NoOpenAccountingPeriodException
     *             si no hay ningun mes abierto de esa fecha en adelante. La salida
     *             entonces es abrir el periodo siguiente, nunca reabrir el pasado
     */
    @PreAuthorize("hasRole('SYSTEM')")
    AccountingPeriodDto resolve(LocalDate occurredOn);
}
