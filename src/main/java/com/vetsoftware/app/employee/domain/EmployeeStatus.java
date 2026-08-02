package com.vetsoftware.app.employee.domain;

/**
 * Estado del ciclo de invitación del empleado.
 *
 * <ul>
 * <li>{@code INVITED}: creado por un admin y aún no ha iniciado sesión por
 * primera vez.
 * <li>{@code ACTIVE}: ya inició sesión al menos una vez (o es el dueño
 * auto-registrado).
 * </ul>
 *
 * Es ortogonal a {@code enabled} (activo/desactivado como soft-delete) y a
 * {@code
 * mustChangePassword} (obligación de cambiar la contraseña temporal): el estado
 * pasa a ACTIVE en el primer login, mientras que {@code mustChangePassword} se
 * limpia recién cuando el empleado cambia efectivamente su contraseña.
 */
public enum EmployeeStatus {
    INVITED, ACTIVE
}
