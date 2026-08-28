package com.vetsoftware.app.documentwithholding.application.port.in;

import com.vetsoftware.app.documentwithholding.application.dto.DocumentWithholdingDto;
import com.vetsoftware.app.shared.pagination.PageResult;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * La vigilancia de lo NO certificado, vista desde la plataforma: todo lo que se
 * retuvo en un ano gravable y aun no tiene papel que lo respalde, en todas las
 * clinicas.
 *
 * <p>
 * <strong>Esta mitad y su hermana de tenant no son un duplicado.</strong> La
 * vigilancia es, por definicion, un barrido sin empresa —tesoreria necesita ver
 * el total pendiente del ano antes de que venza el plazo—, y un barrido sin
 * empresa solo lo puede servir un principal cross-tenant. Meter este caso y la
 * consulta del cliente en un unico puerto con {@code companyId} opcional habria
 * obligado a un SpEL con {@code or hasAuthority}, que es exactamente la fuga
 * que {@code LISTADOS_SIN_EMPRESA_SOLO_SYSTEM} (BE-29) persigue: bastaria
 * omitir el parametro para leer las retenciones de todos los tenants. Por eso
 * son dos puertos y no uno, y por eso este no acepta {@code companyId} ni
 * siquiera opcional — ver
 * {@link ListUncertifiedDocumentWithholdingsByCompanyUseCase}.
 */
public interface ListUncertifiedDocumentWithholdingsUseCase {

    /**
     * <strong>{@code hasRole('SYSTEM')} a secas.</strong> Devuelve filas de todas
     * las empresas, siempre.
     *
     * @param fiscalYear
     *            el ano gravable por el que se cruza. La consulta de vigilancia es
     *            una resta —lo retenido contra lo certificado— y se declara por
     *            ano, asi que sin el la lista no corresponde a ninguna declaracion
     */
    @PreAuthorize("hasRole('SYSTEM')")
    PageResult<DocumentWithholdingDto> listUncertified(int fiscalYear, int page, int pageSize);
}
