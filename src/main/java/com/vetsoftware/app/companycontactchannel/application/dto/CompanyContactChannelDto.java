package com.vetsoftware.app.companycontactchannel.application.dto;

import com.vetsoftware.app.companycontactchannel.domain.CompanyContactChannel;
import com.vetsoftware.app.companycontactchannel.domain.ContactChannelType;
import com.vetsoftware.app.companycontactchannel.domain.ContactPurpose;
import java.time.LocalDateTime;

/**
 * El canal tal como lo consume la aplicacion.
 *
 * <p>
 * <strong>Publica la revocacion en vez de esconderla.</strong> Un canal cerrado
 * no desaparece de las lecturas: {@code revokedAt} y {@code revokedReason}
 * viajan con la fila porque el valor de esta tabla es poder demostrar que el
 * aviso de marzo iba a una direccion autorizada en marzo. Filtrar los revocados
 * en el DTO seria borrar la prueba en la capa de arriba despues de haberse
 * negado a borrarla en la base.
 *
 * <p>
 * <strong>Sin {@code version}</strong>: es la barandilla del bloqueo optimista,
 * no un dato del expediente. Publicarla invitaria a un cliente a devolverla y a
 * construir un control de concurrencia paralelo al que ya hace Hibernate.
 */
public record CompanyContactChannelDto(Long id, Long companyId, ContactChannelType channelType,
        String address, ContactPurpose purpose, LocalDateTime authorizedAt,
        String authorizationEvidence, LocalDateTime revokedAt, String revokedReason,
        boolean primary, LocalDateTime createdDate) {

    public static CompanyContactChannelDto from(CompanyContactChannel channel) {
        return new CompanyContactChannelDto(channel.getId(), channel.getCompanyId(),
                channel.getChannelType(), channel.getAddress(), channel.getPurpose(),
                channel.getAuthorizedAt(), channel.getAuthorizationEvidence(),
                channel.getRevokedAt(), channel.getRevokedReason(), channel.isPrimary(),
                channel.getCreatedDate());
    }
}
