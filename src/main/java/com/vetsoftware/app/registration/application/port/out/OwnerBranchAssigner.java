package com.vetsoftware.app.registration.application.port.out;

import java.util.List;

/**
 * Ata al dueño recien creado con las sedes de su empresa (hoy, la unica que el
 * alta acaba de crear: "Principal").
 *
 * <p>
 * Es la pieza gemela de {@link EmployeeRoleAssigner}: aquella resuelve
 * <em>que</em> puede hacer el dueño, esta resuelve <em>donde</em>. Sin ella el
 * alta creaba las dos puntas —la sede y el empleado— y no escribia la fila de
 * {@code employee_branches} que las une, asi que
 * {@code Authz.currentBranchIds()} devolvia el conjunto vacio y la primera
 * invitacion de personal moria con <b>403 BRANCH_NOT_ALLOWED</b>. Incidencia
 * <b>#510</b>.
 */
public interface OwnerBranchAssigner {

    /**
     * @return las sedes que quedan efectivamente asignadas, <strong>leidas de
     *         vuelta de la base</strong> y no las que se pidieron. Es la diferencia
     *         entre una guarda y un adorno: el {@code INSERT … SELECT} que
     *         materializa la asignacion no produce ninguna fila si el empleado o la
     *         sede no son de {@code companyId}, y lo hace <em>sin error</em>, asi
     *         que devolver el objetivo pedido seria exactamente el «verde que
     *         miente» que esta incidencia vino a cerrar.
     */
    List<Long> assignAllCompanyBranches(Long employeeId, Long companyId);
}
