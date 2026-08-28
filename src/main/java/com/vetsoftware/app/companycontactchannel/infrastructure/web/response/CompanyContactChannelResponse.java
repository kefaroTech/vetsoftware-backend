package com.vetsoftware.app.companycontactchannel.infrastructure.web.response;

import com.vetsoftware.app.companycontactchannel.application.dto.CompanyContactChannelDto;
import com.vetsoftware.app.companycontactchannel.domain.ContactChannelType;
import com.vetsoftware.app.companycontactchannel.domain.ContactPurpose;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/**
 * El canal tal como sale por HTTP.
 *
 * <p>
 * <strong>{@code revokedAt} y {@code revokedReason} van sin {@code REQUIRED} a
 * proposito</strong>: son nulos mientras el canal sigue vivo, que es el estado
 * normal, y marcarlos obligatorios haria que el tipo generado para el front
 * prometiera un valor que la mayoria de las filas no tiene. Lo que si tiene que
 * pasar es que la clave <em>aparezca</em> con valor nulo: es como el front
 * distingue un canal vigente de uno cerrado.
 *
 * <p>
 * <strong>{@code primary} es un dato de la fila, no un calculo del
 * cliente.</strong> Quien lo pinte no debe deducirlo del orden del listado
 * aunque el primario venga arriba: el orden es una comodidad, el marcador es la
 * verdad.
 */
public record CompanyContactChannelResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long companyId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) ContactChannelType channelType,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String address,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) ContactPurpose purpose,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime authorizedAt,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String authorizationEvidence,
        LocalDateTime revokedAt, String revokedReason,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean primary,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime createdDate) {

    public static CompanyContactChannelResponse from(CompanyContactChannelDto dto) {
        return new CompanyContactChannelResponse(dto.id(), dto.companyId(), dto.channelType(),
                dto.address(), dto.purpose(), dto.authorizedAt(), dto.authorizationEvidence(),
                dto.revokedAt(), dto.revokedReason(), dto.primary(), dto.createdDate());
    }
}
