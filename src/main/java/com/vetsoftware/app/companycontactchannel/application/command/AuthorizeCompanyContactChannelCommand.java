package com.vetsoftware.app.companycontactchannel.application.command;

import com.vetsoftware.app.companycontactchannel.domain.ContactChannelType;
import com.vetsoftware.app.companycontactchannel.domain.ContactPurpose;

/**
 * Alta de un canal autorizado.
 *
 * <p>
 * <strong>El {@code companyId} esta aqui y NO en el {@code Request}.</strong>
 * Lo inyecta el controller desde {@code authz.currentCompanyId()} y el puerto
 * lo revalida con {@code @authz.isMyCompany(#command.companyId)}. Si viajara en
 * el cuerpo, un cliente podria sembrar canales de contacto en la ficha de otra
 * empresa: no es leer dato ajeno, es <em>escribir</em> por donde se le avisa a
 * la competencia.
 *
 * <p>
 * <strong>Sin {@code authorizedAt} y sin {@code primary}, y las dos ausencias
 * tienen motivo.</strong> La fecha sale del reloj inyectado porque es la que
 * decide si un aviso ya enviado estaba permitido —dejarla al cliente seria
 * dejarle antedatar su propio consentimiento—; y el canal nace no primario
 * porque designar el principal es una decision declarada, con su propio caso de
 * uso.
 *
 * @param authorizationEvidence
 *            con que se demuestra el consentimiento: el formulario firmado, la
 *            clausula del contrato, la grabacion. Es obligatorio en la base, y
 *            en blanco seria peor que ausente porque la fila aparentaria estar
 *            respaldada
 */
public record AuthorizeCompanyContactChannelCommand(Long companyId, ContactChannelType channelType,
        String address, ContactPurpose purpose, String authorizationEvidence) {
}
