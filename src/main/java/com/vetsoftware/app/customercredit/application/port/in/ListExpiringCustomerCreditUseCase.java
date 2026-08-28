package com.vetsoftware.app.customercredit.application.port.in;

import com.vetsoftware.app.customercredit.application.dto.CustomerCreditEntryDto;
import com.vetsoftware.app.shared.pagination.PageResult;
import java.time.LocalDate;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * <strong>Uno de los nueve barridos de plataforma</strong> —«saldos que
 * caducan»—, y por tanto una de las nueve consultas que recorren todas las
 * clinicas a proposito.
 *
 * <p>
 * El indice que lo sirve va <strong>sin la empresa delante</strong>
 * ({@code ix_cce_expiring} por su fecha, {@code ix_ccb_applicable} para la
 * proyeccion): ponersela lo haria inutil, porque lo que hace falta es
 * exactamente recorrerlo entero. Que el indice este declarado asi en el
 * changeset <strong>no exime al caso de uso de la regla de aislamiento</strong>
 * —esa regla recorre codigo, no documentos—, asi que el barrido nace con puerto
 * declarado y con su autorizacion restringida a plataforma.
 *
 * <p>
 * <strong>Su hermano acotado por empresa es
 * {@link ListCustomerCreditEntriesUseCase}</strong>, que es por donde el
 * cliente ve lo suyo. Escribir este barrido como un proceso suelto sin puerto
 * declarado seria peor que el problema: lo haria invisible a las reglas de
 * aislamiento y lo dejaria sin ninguna autorizacion, y el dia que alguien le
 * pusiera un boton, cualquiera listaria los saldos por caducar de las
 * quinientas clinicas.
 */
public interface ListExpiringCustomerCreditUseCase {

    @PreAuthorize("hasRole('SYSTEM')")
    PageResult<CustomerCreditEntryDto> listExpiring(LocalDate before, int page, int pageSize);
}
