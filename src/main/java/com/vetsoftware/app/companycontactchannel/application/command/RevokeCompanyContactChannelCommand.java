package com.vetsoftware.app.companycontactchannel.application.command;

/**
 * Cierre de un canal autorizado.
 *
 * <p>
 * <strong>El motivo es obligatorio y viaja aqui</strong>, al contrario que en
 * otras bajas del repositorio donde la columna no existe. La tabla si tiene
 * {@code revoked_reason}, y su {@code CHECK} exige que vaya con
 * {@code revoked_at} o que no vaya ninguno: una revocacion sin motivo obliga a
 * quien audite el ano siguiente a adivinar si el cliente se dio de baja o si
 * fue un error de captura.
 *
 * <p>
 * <strong>No hay {@code revokedAt} en el command.</strong> La fecha de cierre
 * sale del reloj inyectado por el mismo motivo que la de apertura: es la
 * frontera entre los avisos que estaban permitidos y los que no, y no puede ser
 * un campo de formulario.
 */
public record RevokeCompanyContactChannelCommand(Long id, Long companyId, String reason) {
}
