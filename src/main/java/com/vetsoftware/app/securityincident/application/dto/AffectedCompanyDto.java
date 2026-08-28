package com.vetsoftware.app.securityincident.application.dto;

import com.vetsoftware.app.securityincident.domain.AffectedScope;
import com.vetsoftware.app.securityincident.domain.SecurityIncidentCompany;

/**
 * Una clinica alcanzada, tal como la lee la consola de plataforma.
 *
 * <p>
 * <strong>Este DTO no sale por ningun puerto de cliente.</strong> Lo que una
 * clinica puede saber es que hubo un incidente que la alcanzo; a cuantas mas
 * alcanzo, y cuantos titulares de cada una, es informacion de las demas. La
 * decision es de autorizacion —los siete puertos de esta rodaja son
 * {@code hasRole('SYSTEM')} a secas— y se anota aqui porque es donde se ve el
 * dato que no debe cruzar.
 */
public record AffectedCompanyDto(Long id, Long securityIncidentId, Long companyId,
        AffectedScope affectedScope, int affectedSubjectCount) {

    public static AffectedCompanyDto from(SecurityIncidentCompany affected) {
        return new AffectedCompanyDto(affected.getId(), affected.getSecurityIncidentId(),
                affected.getCompanyId(), affected.getAffectedScope(),
                affected.getAffectedSubjectCount());
    }
}
