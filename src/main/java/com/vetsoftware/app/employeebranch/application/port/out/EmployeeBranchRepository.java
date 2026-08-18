package com.vetsoftware.app.employeebranch.application.port.out;

import java.util.Collection;
import java.util.List;

public interface EmployeeBranchRepository {

    /** Ids de las sedes asignadas (vigentes) al empleado. */
    List<Long> findBranchIdsByEmployeeId(Long employeeId);

    /**
     * Deja al empleado con EXACTAMENTE estas sedes (desactiva las que sobran,
     * reactiva/inserta las que faltan), dentro de {@code companyId}.
     *
     * <p>
     * La empresa no es decorado: el {@code employeeId} y los {@code branchIds} los
     * elige el cliente, y el UPDATE de reactivación no tiene lectura previa que
     * valide la propiedad. Los dos extremos se acotan en el SQL por {@code EXISTS}
     * contra {@code employees} y {@code branches}.
     */
    void replaceBranches(Long employeeId, Long companyId, Collection<Long> branchIds);
}
