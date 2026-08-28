package com.vetsoftware.app.withholdingcertificate.application.port.in;

import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.withholdingcertificate.application.dto.WithholdingCertificateDto;
import java.time.LocalDate;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * El reloj que le faltaba a la tabla, servido como listado: que certificados
 * vencen antes de una fecha y todavia no han llegado.
 *
 * <p>
 * Es el motivo por el que {@code legal_deadline_on} se guarda como dato y no se
 * calcula. Con la fecha en la columna, esto es un rango sobre
 * {@code ix_withholding_certificates_missing}; derivandola al vuelo habria que
 * leer la tabla entera y aplicar el calendario de festivos fila a fila, asi que
 * el aviso llegaria tarde o no llegaria.
 */
public interface ListMissingWithholdingCertificatesUseCase {

    /**
     * <strong>{@code hasRole('SYSTEM')} a secas.</strong> Barre por vencimiento y
     * por «aun no ha llegado», sin filtrar por empresa: devuelve filas de todos los
     * tenants a la vez. Acotarlo por el vencimiento no cuenta como filtro de
     * empresa -una fecha no es de nadie-, que es exactamente el criterio de
     * {@code LISTADOS_SIN_EMPRESA_SOLO_SYSTEM} (BE-29).
     *
     * <p>
     * Lo que el tenant necesita va en el hermano acotado,
     * {@link ListMissingWithholdingCertificatesByCompanyUseCase}. Son dos puertos y
     * no uno con un parametro opcional: mezclarlos obligaria a un unico gate, y el
     * unico gate que serviria para las dos formas es el mas debil.
     *
     * @param deadlineBefore
     *            se listan los que vencen <em>estrictamente antes</em> de esta
     *            fecha. La consola pasa hoy, o hoy mas el margen que quiera dar
     */
    @PreAuthorize("hasRole('SYSTEM')")
    PageResult<WithholdingCertificateDto> listMissing(LocalDate deadlineBefore, int page,
            int pageSize);
}
