package com.vetsoftware.app.aiproposal.infrastructure.web;

import com.vetsoftware.app.aiproposal.application.port.out.PaidInvocationSignalPort;
import com.vetsoftware.app.shared.ai.PaidInvocationMark;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * El adaptador del bit: lo escribe en la peticion HTTP en curso, que es donde
 * {@code LoginRateLimitFilter} lo va a leer al volver de la cadena.
 *
 * <p>
 * <strong>Sin peticion ligada, no hace nada — y eso es correcto.</strong> Un
 * test unitario, un hilo de tareas o cualquier invocacion fuera de un servlet
 * dejan {@link RequestContextHolder} vacio; entonces no hay cupo que devolver
 * porque no hubo filtro que lo consumiera. La rama de guarda no es defensiva:
 * es el caso legitimo, y ademas es el que mantiene el sesgo de
 * {@link PaidInvocationMark} — sin marca se cobra—.
 *
 * <p>
 * &#9940; <strong>Se escribe sobre la peticion, no sobre un campo de este
 * bean.</strong> El bean es singleton y lo comparten todas las peticiones a la
 * vez: cualquier estado aqui seria una fuga entre prospectos concurrentes, y la
 * forma que tomaria es la peor posible —devolverle el cupo a quien si invoco al
 * modelo porque otro, en paralelo, no lo hizo—.
 */
@Component
public class RequestScopedPaidInvocationSignal implements PaidInvocationSignalPort {

    @Override
    public void signal(boolean huboInvocacionDePago) {
        RequestAttributes atributos = RequestContextHolder.getRequestAttributes();
        if (atributos instanceof ServletRequestAttributes servlet)
            PaidInvocationMark.marcar(servlet.getRequest(), huboInvocacionDePago);
    }
}
