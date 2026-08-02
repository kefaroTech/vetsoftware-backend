package com.vetsoftware.app.employee.application.command;

import java.util.List;

/**
 * Alta de staff por un admin: crea el empleado, le asigna roles y sedes, y le envía la invitación
 * por correo. {@code branchIds} debe traer al menos una sede — un empleado no puede crearse sin
 * sede (quedaría bloqueado de los recursos scopeados a sede).
 */
public record InviteEmployeeCommand(
    String employeeCode,
    String password,
    String name,
    String email,
    Long companyId,
    List<Long> roleIds,
    List<Long> branchIds) {}
