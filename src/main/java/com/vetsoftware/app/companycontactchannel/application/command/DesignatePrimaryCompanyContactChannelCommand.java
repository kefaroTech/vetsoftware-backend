package com.vetsoftware.app.companycontactchannel.application.command;

/**
 * Designacion del canal principal de un proposito.
 *
 * <p>
 * <strong>No lleva el proposito, y eso es deliberado.</strong> El proposito es
 * el que ya tenga el canal senalado por {@code id}: aceptarlo aqui permitiria
 * pedir que un canal de {@code MARKETING} pase a ser el primario de
 * {@code BILLING}, que no es una designacion sino una reescritura del
 * consentimiento. Lo que el cliente autorizo fue <em>ese</em> canal para
 * <em>ese</em> fin.
 */
public record DesignatePrimaryCompanyContactChannelCommand(Long id, Long companyId) {
}
