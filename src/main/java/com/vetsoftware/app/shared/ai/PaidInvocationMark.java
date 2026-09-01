package com.vetsoftware.app.shared.ai;

import jakarta.servlet.ServletRequest;

/**
 * &#9940; <strong>La marca con la que una peticion declara si llego a invocar
 * el modelo de pago.</strong> Es el unico canal por el que el desenlace viaja
 * desde la rodaja que lo conoce ({@code aiproposal}) hasta la que reparte el
 * cupo ({@code auth}), y esta escrito para ser <em>lo mas flojo que
 * funciona</em>: un atributo de peticion, un nombre y un booleano. Ni una
 * dependencia entre features, ni un tipo compartido que arrastre semantica de
 * ninguna de las dos.
 *
 * <p>
 * <strong>Por que esta en el kernel y no en una de las dos rodajas.</strong>
 * Cumple las cuatro condiciones de admision de {@code shared/}, las mismas que
 * {@link ModelPricing} —que existe por este mismo cruce—: «esta peticion invoco
 * al modelo de pago» no significa nada distinto en {@code aiproposal} que en
 * {@code auth}; es una clase de estaticos sin identidad ni persistencia; no
 * nombra ninguna feature; y duplicarlo obliga a escribir el nombre del atributo
 * dos veces, que es exactamente la «segunda fuente solo vigilada» que el
 * javadoc de {@code ModelPricing} denuncia y que ya se descalibro una vez.
 *
 * <p>
 * &#9940; <strong>La ausencia de marca significa CONSUMIR, nunca
 * devolver.</strong> Los tres estados no son dos:
 *
 * <ul>
 * <li><strong>ausente</strong> — nadie llego a decidir nada (una excepcion
 * antes del desenlace, una ruta que no es del asistente, un caso de uso que
 * devuelve sin pasar por el generador). Se cobra;</li>
 * <li><strong>{@code TRUE}</strong> — hubo invocacion. Se cobra, tambien si
 * fallo: una invocacion fallida se paga igual;</li>
 * <li><strong>{@code FALSE}</strong> — consta que <em>no</em> hubo invocacion.
 * Solo aqui se devuelve el cupo.</li>
 * </ul>
 *
 * <p>
 * El sesgo es deliberado y es lo que separa esto de un {@code finally} que
 * devuelva siempre: si la marca se pierde —un cambio futuro que rompa el hilo
 * de la peticion, un bean sin cablear, un camino nuevo que nadie marco— el
 * fallo es cobrar de mas, que es visible y lo reporta un usuario. El sesgo
 * contrario convierte el cupo diario en decorativo <em>en silencio</em>.
 */
public final class PaidInvocationMark {

    /**
     * El nombre del atributo. Lleva el paquete entero por delante para no chocar
     * con nada que el contenedor o un starter pongan en la misma peticion.
     */
    public static final String ATRIBUTO = "com.vetsoftware.app.shared.ai.paidInvocation";

    private PaidInvocationMark() {
    }

    /**
     * Deja constancia del desenlace. Idempotente por sobrescritura: el ultimo que
     * escribe gana, y el unico escritor por peticion es el caso de uso.
     */
    public static void marcar(ServletRequest peticion, boolean huboInvocacionDePago) {
        if (peticion == null)
            return;
        peticion.setAttribute(ATRIBUTO, huboInvocacionDePago);
    }

    /**
     * Si consta —afirmativamente— que esta peticion no llego a invocar el modelo.
     *
     * <p>
     * <strong>{@code Boolean.FALSE.equals(...)} y no una negacion</strong>: un
     * atributo ausente es {@code null}, y {@code !Boolean.TRUE.equals(null)} seria
     * {@code true}, es decir «devuelve el cupo» para toda peticion que jamas paso
     * por el asistente. La comparacion positiva contra {@code FALSE} es lo que hace
     * que el estado por defecto sea cobrar.
     */
    public static boolean constaQueNoHuboInvocacion(ServletRequest peticion) {
        return peticion != null && Boolean.FALSE.equals(peticion.getAttribute(ATRIBUTO));
    }
}
