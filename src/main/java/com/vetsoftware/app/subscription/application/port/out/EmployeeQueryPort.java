package com.vetsoftware.app.subscription.application.port.out;

import com.vetsoftware.app.subscription.domain.EmployeeRef;
import java.util.Optional;

/**
 * Sin variante ancha a proposito. R14 —<em>el empleado que firma un otrosi es
 * de la misma empresa que el contrato</em>— no la puede imponer la base:
 * {@code fk_subscription_amendments_employee} es una FK <strong>simple</strong>
 * a {@code employees(id)}, y hacerla compuesta exigiria una clave auxiliar
 * sobre una tabla caliente de otra feature. Es una regla de codigo, y dejar
 * disponible un {@code findById(id)} es dejar la fuga a mano del proximo
 * copy-paste: un otrosi de la clinica A firmado por un empleado de la B.
 */
public interface EmployeeQueryPort {
    Optional<EmployeeRef> findByIdAndCompanyId(Long employeeId, Long companyId);
}
