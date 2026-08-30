package com.vetsoftware.app.registration.application.port.out;

import java.util.Optional;

/**
 * Marca como convertida la propuesta del asistente de la que venia este alta, y
 * devuelve su id.
 *
 * <p>
 * Es el puerto que traduce el <strong>token publico</strong> —lo unico que el
 * prospecto lleva en la URL— al <strong>id</strong>, que es lo unico que se
 * puede escribir en otra tabla sin multiplicar el secreto.
 *
 * <p>
 * <strong>Vacio cuando el token no corresponde a ninguna propuesta
 * viva</strong> —un enlace caducado, o una que ya se llevo la purga de
 * retencion—. El alta sigue adelante: perder la atribucion de un embudo es un
 * dato de analitica, negarle el registro a un cliente que pago no lo es.
 */
public interface ProposalConverter {

    Optional<Long> markConverted(String publicToken);
}
