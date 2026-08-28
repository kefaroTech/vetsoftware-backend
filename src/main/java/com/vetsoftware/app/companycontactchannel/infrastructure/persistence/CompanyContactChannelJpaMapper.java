package com.vetsoftware.app.companycontactchannel.infrastructure.persistence;

import com.vetsoftware.app.companycontactchannel.domain.CompanyContactChannel;
import org.springframework.stereotype.Component;

/**
 * El unico sitio que conoce a la vez el modelo de dominio y la entidad JPA.
 *
 * <p>
 * <strong>Un solo {@code toDomain} y sin sobrecarga de camino de
 * escritura</strong>: el dominio no guarda ningun companion VO —la empresa va
 * como escalar— asi que no hay proxy que se pueda disparar al reconstruir el
 * canal.
 *
 * <p>
 * <strong>La {@code version} viaja en los dos sentidos.</strong> Sin llevarla
 * en la ida, cada {@code save} de un canal ya persistido le pasaria a Hibernate
 * una version nula y la operacion se convertiria en un {@code INSERT}: la
 * revocacion, en vez de cerrar la fila, crearia una segunda autorizacion
 * identica y el canal seguiria vivo. Es el peor fallo posible en una tabla cuyo
 * unico trabajo es demostrar cuando dejo de estar permitido escribir.
 *
 * <p>
 * <strong>{@code primary_marker} no aparece por ningun lado</strong>, y es
 * correcto: la calcula el motor. Ver el javadoc de
 * {@link CompanyContactChannelJpaEntity}.
 */
@Component
public class CompanyContactChannelJpaMapper {

    public CompanyContactChannelJpaEntity toJpa(CompanyContactChannel channel) {
        CompanyContactChannelJpaEntity entity = new CompanyContactChannelJpaEntity();
        entity.setId(channel.getId());
        entity.setCompanyId(channel.getCompanyId());
        entity.setChannelType(channel.getChannelType());
        entity.setAddress(channel.getAddress());
        entity.setPurpose(channel.getPurpose());
        entity.setAuthorizedAt(channel.getAuthorizedAt());
        entity.setAuthorizationEvidence(channel.getAuthorizationEvidence());
        entity.setRevokedAt(channel.getRevokedAt());
        entity.setRevokedReason(channel.getRevokedReason());
        entity.setPrimary(channel.isPrimary());
        entity.setCreatedDate(channel.getCreatedDate());
        entity.setVersion(channel.getVersion());
        return entity;
    }

    public CompanyContactChannel toDomain(CompanyContactChannelJpaEntity entity) {
        return new CompanyContactChannel(entity.getId(), entity.getCompanyId(),
                entity.getChannelType(), entity.getAddress(), entity.getPurpose(),
                entity.getAuthorizedAt(), entity.getAuthorizationEvidence(), entity.getRevokedAt(),
                entity.getRevokedReason(), entity.isPrimary(), entity.getCreatedDate(),
                entity.getVersion());
    }
}
