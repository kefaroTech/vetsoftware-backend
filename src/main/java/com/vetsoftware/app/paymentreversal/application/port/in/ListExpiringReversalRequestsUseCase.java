package com.vetsoftware.app.paymentreversal.application.port.in;

import com.vetsoftware.app.paymentreversal.application.dto.PaymentReversalRequestDto;
import com.vetsoftware.app.shared.pagination.PageResult;
import java.time.LocalDateTime;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * <strong>Barrido de plataforma: los expedientes que vencen sin
 * resolver.</strong>
 *
 * <p>
 * Es uno de los barridos que recorren <em>todas</em> las clinicas a proposito.
 * El indice que lo sirve —{@code ix_payment_reversal_requests_deadline}, sobre
 * {@code (deadline_at, outcome)}— va <strong>sin la empresa delante</strong>
 * deliberadamente: ponersela lo haria inutil, porque la pregunta no es «que le
 * vence a esta clinica» sino «que le vence a alguien». De ahi que
 * {@code deadline_at} se guarde como dato y no como formula: un plazo que solo
 * existe como calculo no se puede consultar.
 *
 * <p>
 * <strong>Por eso la autorizacion es {@code hasRole('SYSTEM')} a
 * secas</strong>, y no hay forma de abrirla por permiso: un listado que no
 * filtra por empresa devuelve filas de todos los tenants, y
 * {@code LISTADOS_SIN_EMPRESA_SOLO_SYSTEM} es una regla dura sin excepcion
 * posible. Su <strong>hermano acotado</strong>, para lo que el cliente necesita
 * ver, es {@link ListPaymentReversalRequestsUseCase}.
 *
 * <p>
 * Y va declarado como puerto, no como proceso suelto: escribirlo como un job
 * sin puerto lo haria invisible a las reglas de aislamiento y lo dejaria sin
 * ninguna autorizacion, de modo que el dia que alguien le ponga un boton
 * cualquiera listaria los expedientes de las quinientas clinicas.
 */
public interface ListExpiringReversalRequestsUseCase {

    @PreAuthorize("hasRole('SYSTEM')")
    PageResult<PaymentReversalRequestDto> listExpiring(LocalDateTime before, int page,
            int pageSize);
}
