package com.vetsoftware.app.billingdocumentstatushistory.application.port.in;

import com.vetsoftware.app.billingdocumentstatushistory.application.dto.BillingDocumentStatusHistoryDto;
import com.vetsoftware.app.billingdocumentstatushistory.domain.BillingDocumentStatus;
import com.vetsoftware.app.shared.pagination.PageResult;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Los cambios de una empresa que dejaron el documento en un estado concreto.
 *
 * <p>
 * Es la consulta que justifica la ficha: filtrando por
 * {@code AWAITING_EXTERNAL} y mirando hasta una fecha se responde «cuantos
 * documentos estaban esperando factura externa a 31 de marzo», que es la lista
 * de trabajo que el propio modelo propone vigilar.
 */
public interface ListBillingDocumentStatusChangesByStatusUseCase {

    /**
     * <strong>{@code toStatus} es obligatorio y no tiene valor por
     * defecto.</strong> Un defecto silencioso convertiria una bandeja de vigilancia
     * en un listado cualquiera, y quien la mira no notaria que esta contando otra
     * cosa.
     */
    @PreAuthorize("hasRole('SYSTEM') or (hasAuthority('billingDocumentStatusHistory.read')"
            + " and @authz.isMyCompany(#companyId))")
    PageResult<BillingDocumentStatusHistoryDto> listByCompanyAndToStatus(Long companyId,
            BillingDocumentStatus toStatus, int page, int pageSize);
}
