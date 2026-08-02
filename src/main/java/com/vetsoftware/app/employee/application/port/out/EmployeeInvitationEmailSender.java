package com.vetsoftware.app.employee.application.port.out;

/**
 * Envía el correo de invitación (plantilla de Resend) al staff creado por un admin, con sus datos
 * de acceso: usuario (código), contraseña temporal y rol. La contraseña va en claro porque es el
 * primer acceso.
 */
public interface EmployeeInvitationEmailSender {
  void send(
      String toEmail,
      String employeeName,
      String companyName,
      String employeeCode,
      String tempPassword,
      String roleName);
}
