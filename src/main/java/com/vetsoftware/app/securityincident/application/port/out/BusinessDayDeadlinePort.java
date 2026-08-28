package com.vetsoftware.app.securityincident.application.port.out;

import java.time.LocalDate;

/**
 * El calendario laboral colombiano, visto desde esta rodaja.
 *
 * <p>
 * <strong>Existe para no reimplementar el calendario.</strong> El calculo de
 * dias habiles vive una sola vez en el producto —en el bloque de festivos— y
 * esta rodaja lo consume por un puerto, igual que {@code entitlement} consume
 * la bitacora de limites. Copiarlo aqui significaria que la correccion de un
 * festivo llegara a unos plazos si y a otros no.
 *
 * <p>
 * <strong>Este puerto no importa nada del bloque de festivos</strong>: habla de
 * {@code LocalDate} y de un entero. Quien conoce a la otra feature es el
 * adaptador, que vive en {@code infrastructure/orchestration}, que es el unico
 * sitio donde el vertical slicing lo permite.
 *
 * <p>
 * <strong>Puede fallar, y tiene que poder.</strong> Si el tramo de calendario
 * cargado no cubre el recorrido, la implementacion deja salir
 * {@code HolidayCalendarGapException}. Tratar los dias sin sembrar como habiles
 * daria un vencimiento <em>mas corto</em> del real y un incumplimiento
 * silencioso en la direccion contraria.
 */
public interface BusinessDayDeadlinePort {

    /**
     * La fecha en la que vence un plazo de {@code businessDays} dias habiles
     * contado desde {@code start}.
     *
     * <p>
     * <strong>El dia de partida no cuenta</strong>: el plazo empieza a correr el
     * dia habil siguiente al hecho, que es como lo cuenta la norma colombiana. Esa
     * semantica la fija el calendario y se hereda tal cual; de ella depende que el
     * resultado sea siempre posterior a {@code start}, y con ello que
     * {@code deadline_at > escalated_at} se cumpla por construccion.
     */
    LocalDate resolve(LocalDate start, int businessDays);
}
