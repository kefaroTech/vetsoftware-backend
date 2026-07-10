package com.vetsoftware.app.coderecovery.application.command;

/** Solicitud de "recordar mi código de usuario" por correo. */
public record RecoverEmployeeCodeCommand(String email) {}
