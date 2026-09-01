package com.vetsoftware.app.aiproposal.application.port.out;

/**
 * &#9940; <strong>Por donde sale el unico bit que el repartidor de cupo
 * necesita: si esta peticion llego a invocar el modelo de pago.</strong>
 *
 * <p>
 * <strong>Por que un puerto y no el valor de retorno.</strong> Quien reparte el
 * cupo diario es {@code LoginRateLimitFilter}, un filtro de servlet de la
 * rodaja {@code auth}; quien conoce el desenlace es este caso de uso, en
 * {@code application}, donde no hay —ni puede haber— un
 * {@code HttpServletRequest}. La alternativa era llevar el dato dentro de
 * {@code ProposalViewDto}, y esa puerta se cerro a proposito: el DTO es <em>lo
 * unico</em> que sale por los cuatro endpoints, y {@code ProposalPresentation}
 * colapsa las degradaciones en {@code DETERMINISTIC} justamente para que un
 * anonimo con {@code curl} no pueda saber <em>cuando</em> se agoto el
 * presupuesto del dia. Un campo «&#191;se invoco?» en ese record deja ese
 * secreto a una linea de distancia del cable, y a merced de quien anada el
 * siguiente campo a la respuesta.
 *
 * <p>
 * <strong>Se senalan los DOS desenlaces, no solo uno.</strong> Marcar
 * unicamente las degradaciones dejaria «no se marco» significando dos cosas
 * —«hubo invocacion» y «nadie llego a decidir»—, y el dia que alguien anadiera
 * un camino nuevo sin marcarlo, el cupo se devolveria solo. Con las dos ramas
 * escritas, la ausencia de marca es siempre lo mismo: un desenlace desconocido,
 * que se cobra.
 */
public interface PaidInvocationSignalPort {

    /**
     * @param huboInvocacionDePago
     *            {@code true} si se llamo al modelo, <strong>incluida la llamada
     *            que fallo</strong>: una invocacion fallida se paga igual y el
     *            guardian de gasto ya la reconcilia como gasto real
     */
    void signal(boolean huboInvocacionDePago);
}
