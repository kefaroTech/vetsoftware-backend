package com.vetsoftware.app.companycontactchannel.domain;

/**
 * Un canal revocado no puede ser el primario de su proposito.
 *
 * <p>
 * <strong>Es un conflicto de estado, y la base no lo dice.</strong>
 * {@code primary_marker} vale {@code NULL} en cuanto hay {@code revoked_at},
 * asi que marcar como primario un canal revocado <em>no</em> viola
 * {@code uq_company_contact_channels_primary}: el {@code UPDATE} pasa, la fila
 * queda con {@code is_primary = TRUE}, el hueco de primario sigue libre y la
 * empresa se queda <strong>sin primario</strong> creyendo que acaba de
 * designarlo. El aviso de cobro se manda por el canal que no era, o no se
 * manda. Por eso la barandilla vive aqui: es exactamente el caso que el motor
 * deja pasar en silencio.
 */
public class RevokedContactChannelCannotBePrimaryException extends RuntimeException {

    public RevokedContactChannelCannotBePrimaryException(Long id) {
        super("Company contact channel " + id + " is revoked and cannot be primary");
    }
}
