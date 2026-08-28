package com.vetsoftware.app.externalinvoicingoutage.application.command;

import com.vetsoftware.app.externalinvoicingoutage.domain.OutageResolution;

/**
 * Registra a una clinica en el reparto de una caida.
 *
 * <p>
 * <strong>Aqui el {@code companyId} SI viaja, y no es una fuga.</strong> Este
 * es un caso de uso de plataforma cerrado a {@code hasRole('SYSTEM')} a secas:
 * un principal SYSTEM no tiene empresa propia y tiene que poder <em>elegir</em>
 * a cual afecta, exactamente como en tesoreria. Lo que la regla
 * {@code EMPRESA_NO_VIAJA_EN_EL_CUERPO} prohibe es que la empresa llegue en el
 * cuerpo de un endpoint de tenant, donde convertiria
 * {@code @authz.isMyCompany(#command.companyId)} en una comparacion del numero
 * consigo mismo; por eso en la capa web entra como {@code @PathVariable} y no
 * dentro del JSON.
 *
 * @param failedDocumentCount
 *            documentos que se quedaron sin transmitir. Cero es legitimo: una
 *            clinica puede estar dentro del alcance sin haber intentado emitir
 *            nada en esa franja
 * @param resolvedBy
 *            como salio adelante. {@code CONTINGENCY_NUMBERING} es el que hay
 *            que poder demostrar ante la autoridad
 */
public record RegisterAffectedCompanyCommand(Long outageId, Long companyId, int failedDocumentCount,
        OutageResolution resolvedBy) {
}
