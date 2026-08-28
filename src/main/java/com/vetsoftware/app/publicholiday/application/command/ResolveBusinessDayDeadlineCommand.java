package com.vetsoftware.app.publicholiday.application.command;

import java.time.LocalDate;

/**
 * La pregunta «¿cuando vence un plazo de N dias habiles contado desde X?».
 *
 * <p>
 * <strong>Lleva {@code companyId} aunque {@code public_holidays} no tenga
 * empresa</strong>, y esa es la decision deliberada del bloque: la lectura la
 * hacen los dos lados —plataforma y tenant—, y sin el campo el unico gate
 * posible seria una autoridad suelta, que abriria por permiso lo que sus
 * hermanas cierran por tenant. Con el campo, el puerto puede exigir
 * {@code @authz.isMyCompany(#command.companyId)} al empleado y dejar la via
 * ancha solo a {@code ROLE_SYSTEM}.
 *
 * <p>
 * {@code startDate} nulo significa «desde hoy», y ese «hoy» lo pone el reloj
 * inyectado del servicio —zona {@code America/Bogota}—, nunca
 * {@code LocalDate.now()}: a las 19:30 de Bogota el reloj sin zona ya esta en
 * el dia siguiente y el plazo saldria corrido una jornada.
 */
public record ResolveBusinessDayDeadlineCommand(LocalDate startDate, int businessDays,
        Long companyId) {

    /**
     * Tope de cordura. Ningun plazo legal del producto pasa de dos anos, y sin tope
     * un {@code businessDays} enorme haria caminar el bucle dia a dia sobre un
     * tramo que ademas habria que cargar entero.
     */
    public static final int MAX_BUSINESS_DAYS = 730;

    public ResolveBusinessDayDeadlineCommand {
        if (businessDays < 1 || businessDays > MAX_BUSINESS_DAYS) {
            throw new IllegalArgumentException(
                    "businessDays must be between 1 and " + MAX_BUSINESS_DAYS);
        }
    }
}
