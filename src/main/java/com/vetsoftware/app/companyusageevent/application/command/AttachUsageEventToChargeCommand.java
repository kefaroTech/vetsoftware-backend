package com.vetsoftware.app.companyusageevent.application.command;

/**
 * Colgar un hecho de uso del cargo que lo facturo.
 *
 * <p>
 * Lleva {@code companyId} <b>y la carga se acota con el</b>: es lo que impide
 * que el cierre de una clinica se lleve por delante el hecho de otra. La base
 * lo respalda con {@code fk_cue_charge (company_id, charge_id)}, compuesta a
 * proposito.
 */
public record AttachUsageEventToChargeCommand(Long id, Long companyId, Long chargeId) {
}
