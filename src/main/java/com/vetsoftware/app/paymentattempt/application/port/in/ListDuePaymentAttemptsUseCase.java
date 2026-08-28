package com.vetsoftware.app.paymentattempt.application.port.in;

import com.vetsoftware.app.paymentattempt.application.dto.PaymentAttemptDto;
import com.vetsoftware.app.shared.pagination.PageResult;
import java.time.LocalDateTime;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * La cola de reintentos: <strong>uno de los nueve barridos de
 * plataforma</strong> ("cobros por reintentar").
 *
 * <p>
 * Recorre <em>todas</em> las clinicas a proposito, y por eso
 * {@code ix_payment_attempts_retry_queue} va sobre {@code next_attempt_at}
 * <strong>sin la empresa delante</strong>: ponersela lo haria inutil.
 *
 * <p>
 * <strong>Declararlo asi en el changeset NO exime al caso de uso de la regla de
 * aislamiento</strong> — esa regla recorre codigo, no documentos—. De ahi las
 * dos condiciones que este puerto cumple y que el documento maestro exige a los
 * nueve:
 *
 * <ol>
 * <li><strong>Puerto declarado.</strong> Escribir el barrido como proceso
 * suelto seria la salida facil y es peor que el problema: lo dejaria invisible
 * a las cinco reglas de aislamiento y <em>sin ninguna autorizacion</em>. El dia
 * que alguien le pusiera un boton, cualquiera listaria los cobros por
 * reintentar de las quinientas clinicas.</li>
 * <li><strong>Autorizacion solo de plataforma.</strong>
 * {@code hasRole('SYSTEM')} a secas, sin excepcion posible, porque no filtra
 * por empresa ({@code LISTADOS_SIN_EMPRESA_SOLO_SYSTEM}, dura).</li>
 * </ol>
 *
 * <p>
 * Su hermano acotado, para lo que el cliente necesita ver, es
 * {@link ListPaymentAttemptsUseCase}.
 */
public interface ListDuePaymentAttemptsUseCase {

    /**
     * @param dueBefore
     *            corte superior de {@code next_attempt_at}. Lo pasa el llamador con
     *            el reloj inyectado en vez de calcularlo aqui, para que un barrido
     *            sea reproducible en un test
     */
    @PreAuthorize("hasRole('SYSTEM')")
    PageResult<PaymentAttemptDto> listDue(LocalDateTime dueBefore, int page, int pageSize);
}
