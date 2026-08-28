package com.vetsoftware.app.companycontactchannel.domain;

/**
 * No hay canal con ese id <strong>en esta empresa</strong>.
 *
 * <p>
 * El canal de otra empresa sale por aqui —no encontrado— y no como prohibido, y
 * esa es la respuesta correcta: un 403 confirmaria que la fila existe, y con
 * ids consecutivos eso es un censo de por donde se le escribe a la competencia.
 */
public class CompanyContactChannelNotFoundException extends RuntimeException {

    public CompanyContactChannelNotFoundException(Long id) {
        super("Company contact channel not found: " + id);
    }
}
