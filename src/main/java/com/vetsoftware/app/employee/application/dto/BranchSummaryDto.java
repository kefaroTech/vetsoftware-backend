package com.vetsoftware.app.employee.application.dto;

import com.vetsoftware.app.employee.domain.BranchRef;

/** Sede asignada a un empleado, tal como se expone en el listado/detalle de empleados. */
public record BranchSummaryDto(Long id, String name) {
    public static BranchSummaryDto from(BranchRef ref) {
        return new BranchSummaryDto(ref.id(), ref.name());
    }
}
