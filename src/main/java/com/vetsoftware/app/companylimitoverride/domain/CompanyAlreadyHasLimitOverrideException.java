package com.vetsoftware.app.companylimitoverride.domain;

/**
 * Ya hay una excepción viva sobre ese eje para esa empresa. Dos serían un techo
 * indeterminado: dos respuestas válidas a la misma pregunta.
 *
 * <p>
 * Ojo al matiz que se escapa: lo que se impide es <em>sobre el mismo eje</em>,
 * no por empresa. Negociar 300 mascotas y 5 usuarios en la misma llamada son
 * dos excepciones vivas y las dos son legítimas.
 */
public class CompanyAlreadyHasLimitOverrideException extends RuntimeException {

    public CompanyAlreadyHasLimitOverrideException(Long companyId, Long limitDimensionId) {
        super("Company " + companyId + " already has a live limit override on dimension "
                + limitDimensionId + ": two would make the ceiling indeterminate");
    }
}
