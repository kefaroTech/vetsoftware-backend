package com.vetsoftware.app.employeebranch.infrastructure.web.request;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import java.util.List;

/**
 * Set atómico de sedes de un empleado. {@code allBranches=true} asigna todas
 * las de la empresa (ignora {@code branchIds}); si es false, asigna exactamente
 * {@code branchIds}.
 */
public record SetEmployeeBranchesRequest(
        // Jackson 3: sin @JsonSetter, omitir el campo responde 400 en vez de caer a
        // false (ver CreateAppointmentRequest.forceOverlap).
        @JsonSetter(nulls = Nulls.AS_EMPTY) boolean allBranches, List<Long> branchIds) {
}
