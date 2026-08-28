package com.vetsoftware.app.externalinvoicingoutage.application.port.in;

import com.vetsoftware.app.externalinvoicingoutage.application.dto.OutageAffectedCompanyDto;
import com.vetsoftware.app.shared.pagination.PageResult;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ListOutageAffectedCompaniesUseCase {

    /**
     * El reparto de una caida: que clinicas alcanzo, con cuantos documentos
     * fallidos cada una y como salio adelante.
     *
     * <p>
     * <strong>{@code hasRole('SYSTEM')} a secas, y esta es la que mas importa
     * cerrar.</strong> Acotar por una clave ajena —{@code outageId}— <b>no</b>
     * cuenta como filtrar por empresa: es el mismo criterio de BE-29 que descarta
     * {@code findAllByAnimalId}. La caida es de todos, asi que este listado
     * devuelve filas de varios tenants a la vez.
     *
     * <p>
     * Lo que el cliente puede ver es <b>que hubo una caida, nunca a cuantos alcanzo
     * ni a quienes</b>. Es una decision de autorizacion y esta es la linea donde se
     * aplica.
     */
    @PreAuthorize("hasRole('SYSTEM')")
    PageResult<OutageAffectedCompanyDto> listByOutage(Long outageId, int page, int pageSize);
}
