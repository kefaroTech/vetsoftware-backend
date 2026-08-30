package com.vetsoftware.app.aiproposal.application.port.in;

import com.vetsoftware.app.aiproposal.application.command.SuppressProposalDataCommand;
import com.vetsoftware.app.aiproposal.application.dto.ProposalSuppressionDto;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Supresion a peticion del titular (articulo 8, literal e, de la Ley 1581).
 *
 * <p>
 * <strong>{@code hasRole('SYSTEM')} a secas, y es lo correcto</strong>: no hay
 * {@code companyId} que revalidar porque una propuesta no pertenece a ninguna
 * empresa -esa es justamente la feature-, y quien responde ante la SIC por el
 * tratamiento de estos datos es el responsable del producto, no un tenant. No
 * es el hueco que {@code LISTADOS_SIN_EMPRESA_SOLO_SYSTEM} vigila: no devuelve
 * filas de nadie, devuelve tres contadores.
 *
 * <p>
 * &#9940; <strong>Y no es un endpoint publico.</strong> Abrirlo al anonimo lo
 * convertiria en un borrador de datos ajenos operado por cualquiera y, de paso,
 * en un oraculo de enumeracion: los contadores dirian si un correo dado ha
 * pedido propuesta alguna vez.
 */
public interface SuppressProposalDataUseCase {

    @PreAuthorize("hasRole('SYSTEM')")
    ProposalSuppressionDto execute(SuppressProposalDataCommand command);
}
