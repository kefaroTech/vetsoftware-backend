package com.vetsoftware.app.withholdingcertificate.application.port.in;

import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.withholdingcertificate.application.dto.WithholdingCertificateDto;
import java.time.LocalDate;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * El hermano acotado de {@link ListMissingWithholdingCertificatesUseCase}, y es
 * el que de verdad usa la clinica: los certificados que le faltan por recibir
 * antes de que venza el plazo.
 *
 * <p>
 * Es «el caso de uso hermano que si recibe {@code companyId}» que prescribe la
 * seccion de autorizacion del {@code CLAUDE.md}. Sin el, la unica forma de que
 * el tenant viera su propio aviso seria relajar el gate del barrido, que sirve
 * filas de todas las empresas.
 */
public interface ListMissingWithholdingCertificatesByCompanyUseCase {

    /**
     * @param deadlineBefore
     *            se listan los que vencen <em>estrictamente antes</em> de esta
     *            fecha. El filtro de empresa va primero en la consulta, que es como
     *            esta ordenado {@code ix_withholding_certificates_year}
     */
    @PreAuthorize("hasRole('SYSTEM') or (hasAuthority('withholdingCertificate.read')"
            + " and @authz.isMyCompany(#companyId))")
    PageResult<WithholdingCertificateDto> listMissingByCompany(Long companyId,
            LocalDate deadlineBefore, int page, int pageSize);
}
