package com.vetsoftware.app.hospitalizationobservation.application.command;

/**
 * El {@code companyId} no viene del cuerpo de la peticion: lo pone el
 * controller desde el principal y el puerto de entrada lo revalida con
 * {@code @authz.isMyCompany}. Esta aqui porque sin el no hay con que acotar la
 * hospitalizacion padre, que es lo que impide colgar la observacion del
 * expediente de otro tenant.
 */
public record CreateHospitalizationObservationCommand(String description, Long hospitalizationId,
        Long createdById, Long companyId) {
}
