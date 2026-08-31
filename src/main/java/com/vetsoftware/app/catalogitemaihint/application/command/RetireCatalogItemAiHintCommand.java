package com.vetsoftware.app.catalogitemaihint.application.command;

/**
 * Retira la pista de un articulo: cierra la vigente <em>sin</em> publicar
 * sucesora.
 *
 * <p>
 * &#9940; <strong>Existe para que los dos identificadores no viajen sueltos, y
 * ese es todo su motivo de ser.</strong> Retirar necesita el articulo y el
 * firmante, los dos {@code Long}: un {@code retire(Long, Long)} se puede llamar
 * con los argumentos cambiados y <b>ni el compilador ni ningun test lo
 * notarian</b> —el firmante acabaria escrito como {@code catalog_item_id} y la
 * peticion moriria contra una clave foranea, o peor, contra la equivocada—. Con
 * dos campos con nombre, el error no se puede escribir.
 *
 * <p>
 * Es ademas la convencion viva de esta feature: {@code Publish} y
 * {@code Revise} ya llevan command y por el mismo motivo. Retirar era el unico
 * corto que quedaba.
 *
 * <p>
 * <strong>Sin {@code companyId}</strong>: {@code catalog_item_ai_hints} no la
 * tiene ni alcanza {@code companies} por ningun camino. La pista describe el
 * catalogo global de plataforma y su puerto va cerrado a rol de sistema a
 * secas.
 *
 * @param retiredBySystemUserId
 *            &#9940; <strong>Lo pone el controller desde la sesion
 *            ({@code authz.currentSystemUserId()}), nunca el cuerpo de la
 *            peticion.</strong> El {@code DELETE} de la ruta no lleva cuerpo, y
 *            aunque lo llevara este campo no iria ahi: un rastro de auditoria
 *            que escribe el auditado no es un rastro, es un formulario. Mismo
 *            criterio que {@code PublishCatalogItemAiHintCommand} y
 *            {@code SuppressProposalDataCommand}.
 *
 *            <p>
 *            Es obligatorio en el command aunque
 *            {@code superseded_by_system_user_id} sea nulable en la tabla: la
 *            nulabilidad cubre las filas anteriores al changeset 393, no las
 *            retiradas que se firman a partir de ahora. Ver
 *            {@code CatalogItemAiHint#supersede}.
 */
public record RetireCatalogItemAiHintCommand(Long catalogItemId, Long retiredBySystemUserId) {

    public RetireCatalogItemAiHintCommand {
        if (catalogItemId == null) {
            throw new IllegalArgumentException("catalogItemId is required");
        }
        if (retiredBySystemUserId == null) {
            throw new IllegalArgumentException("retiredBySystemUserId is required");
        }
    }
}
