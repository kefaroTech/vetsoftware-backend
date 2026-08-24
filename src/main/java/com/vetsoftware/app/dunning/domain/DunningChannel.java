package com.vetsoftware.app.dunning.domain;

/**
 * Por donde se aviso. Espejo de {@code chk_dunning_events_channel}. Sirve para
 * las dos cosas practicas del expediente: demostrar que se aviso, y medir que
 * recordatorio funciona.
 */
public enum DunningChannel {
    EMAIL, SMS, WHATSAPP, PHONE, IN_APP
}
