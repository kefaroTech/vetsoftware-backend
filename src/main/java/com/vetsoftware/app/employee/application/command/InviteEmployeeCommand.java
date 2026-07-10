package com.vetsoftware.app.employee.application.command;

import java.util.List;

/** Alta de staff por un admin: crea el empleado, le asigna roles y le envía la invitación por correo. */
public record InviteEmployeeCommand(String employeeCode, String password, String name, String email,
                                    Long companyId, List<Long> roleIds) {}
