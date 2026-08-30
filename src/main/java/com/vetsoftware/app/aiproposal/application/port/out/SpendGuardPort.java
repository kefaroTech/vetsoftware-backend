package com.vetsoftware.app.aiproposal.application.port.out;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * El techo de gasto diario del modelo, en <strong>codigo</strong>.
 *
 * <p>
 * ⛔ <strong>Terraform declara el numero y deriva el presupuesto de el, pero
 * nadie lo hace cumplir.</strong> Un presupuesto de AWS no sirve como control:
 * no lleva {@code cost_filter}, avisa con hasta 24 horas de retraso y
 * <strong>no corta nada</strong>. Sin esto, el unico limite real seria el de
 * peticiones por IP —5/hora, 20/dia— multiplicado por cuantas IP tenga quien
 * quiera gastarnos el mes en una tarde.
 *
 * <p>
 * <strong>Reserva y reconcilia, en ese orden, y es fail-closed.</strong> Se
 * reserva un coste estimado <em>antes</em> de invocar y se ajusta al real
 * despues. Al reves —cargar solo lo consumido— deja una ventana en la que N
 * llamadas concurrentes ven el contador a cero y pasan todas. Y si el contador
 * no se puede leer, la respuesta es <strong>no</strong>: degradar de mas cuesta
 * una propuesta determinista; degradar de menos cuesta dinero real sin techo.
 *
 * <p>
 * <strong>Lo que NO hace: no lanza.</strong> Superar el tope devuelve
 * {@link Optional#empty()} y el caso de uso responde 200 con el modo degradado.
 * Un 500 le diria al que esta vaciando el cupo que lo consiguio.
 */
public interface SpendGuardPort {

    /**
     * Aparta {@code estimatedUsd} del cupo de hoy.
     *
     * @return la reserva, o {@link Optional#empty()} si el tope ya esta agotado o
     *         no se pudo comprobar
     */
    Optional<SpendReservation> reserve(BigDecimal estimatedUsd);

    /**
     * Ajusta la reserva al coste real. <strong>Se llama siempre</strong>, tambien
     * cuando la invocacion fallo o el navegador cancelo: cancelar en el navegador
     * no cancela la invocacion ni devuelve el gasto.
     */
    void reconcile(SpendReservation reservation, BigDecimal actualUsd);

    /** Devuelve el cupo entero: solo cuando no se llego a invocar. */
    void release(SpendReservation reservation);

    /** El testigo de una reserva. Sin identidad de negocio, solo correlacion. */
    record SpendReservation(String id, BigDecimal reservedUsd) {

        public SpendReservation {
            if (id == null || id.isBlank())
                throw new IllegalArgumentException("reservation id is required");
            if (reservedUsd == null || reservedUsd.signum() < 0)
                throw new IllegalArgumentException("reserved amount must be zero or positive");
        }
    }
}
