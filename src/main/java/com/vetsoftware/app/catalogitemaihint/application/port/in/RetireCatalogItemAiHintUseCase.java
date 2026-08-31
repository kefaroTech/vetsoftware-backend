package com.vetsoftware.app.catalogitemaihint.application.port.in;

import com.vetsoftware.app.catalogitemaihint.application.command.RetireCatalogItemAiHintCommand;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Retira la pista de un articulo: la marca como reemplazada <em>sin</em>
 * sucesora. El articulo pasa a no tener pista y el modelo deja de proponerlo.
 *
 * <p>
 * &#9940; <strong>No borra nada.</strong> El {@code DELETE} de la ruta es la
 * semantica HTTP del recurso «la pista vigente de este articulo», no un
 * {@code DELETE} de fila: la revision retirada se queda en el historial con su
 * texto, su firmante y su fecha, y sigue apareciendo en
 * {@link ListCatalogItemAiHintRevisionsUseCase}. Volver a publicar despues
 * continua la numeracion —revision 3 tras retirar la 2—, no la reinicia.
 *
 * <p>
 * <strong>Queda firmado, desde el changeset 393.</strong> Hasta entonces la
 * tabla auditaba quien <em>publica</em> ({@code published_by_system_user_id},
 * {@code NOT NULL}) y nadie quien apaga: la fila retirada conservaba el
 * firmante de quien la habia publicado —el actor equivocado—, y quien decidio
 * que un articulo dejara de proponerse no quedaba escrito en ninguna parte. 393
 * anadio {@code superseded_by_system_user_id} y este puerto lo recibe ya
 * resuelto en el command.
 *
 * <p>
 * &#9940; <strong>El firmante sale de la sesion, nunca del cuerpo</strong>, y
 * viaja en {@link RetireCatalogItemAiHintCommand} y no como un segundo
 * {@code Long} suelto: dos identificadores del mismo tipo en la misma firma se
 * pueden intercambiar sin que el compilador ni ningun test lo noten. Ver el
 * Javadoc del command.
 *
 * <p>
 * <strong>Sin {@code companyId} y abierto solo a {@code hasRole('SYSTEM')} a
 * secas</strong>: ni {@code catalog_item_ai_hints} ni {@code catalog_items}
 * alcanzan {@code companies}, asi que no hay tenant que acotar y las cuatro
 * reglas de la familia BE-COV no aplican a este slice.
 */
public interface RetireCatalogItemAiHintUseCase {

    @PreAuthorize("hasRole('SYSTEM')")
    void retire(RetireCatalogItemAiHintCommand command);
}
