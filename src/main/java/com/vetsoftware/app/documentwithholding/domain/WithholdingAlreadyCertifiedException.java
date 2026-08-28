package com.vetsoftware.app.documentwithholding.domain;

/**
 * La retencion ya apuntaba a un certificado y se intento apuntarla a otro
 * distinto.
 *
 * <p>
 * <strong>Es un conflicto (409), no una peticion mal formada</strong>: el
 * cuerpo es valido y los dos certificados pueden existir. Lo que falla es el
 * estado de la retencion en este instante — ya tiene respaldo—. Un 400 le diria
 * al operador que corrija un campo que esta bien escrito.
 *
 * <p>
 * La regla no es burocracia. El certificado es la prueba con la que se imputa
 * la retencion en la declaracion; repuntarla en silencio dejaria una
 * declaracion ya presentada respaldada por un papel distinto del que se uso, y
 * sin rastro del cambio. Si el primer certificado estaba mal, se corrige donde
 * vive el error: en el certificado, no reescribiendo a que apunta la retencion.
 */
public class WithholdingAlreadyCertifiedException extends RuntimeException {

    public WithholdingAlreadyCertifiedException(Long withholdingId, Long currentCertificateId,
            Long requestedCertificateId) {
        super("Withholding " + withholdingId + " is already backed by certificate "
                + currentCertificateId + "; cannot relink it to " + requestedCertificateId);
    }
}
