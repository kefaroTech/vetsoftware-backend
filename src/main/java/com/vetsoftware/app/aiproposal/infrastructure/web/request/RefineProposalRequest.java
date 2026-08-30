package com.vetsoftware.app.aiproposal.infrastructure.web.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * El cuadro "&#191;se nos olvido algo?".
 *
 * <p>
 * &#9940; <strong>El token va aqui, en el cuerpo, y no en la ruta</strong>: en
 * el cuerpo no lo ve ni el contexto de log -que escribe {@code getRequestURI()}
 * en toda peticion- ni el {@code Referer} que el navegador manda a terceros.
 *
 * <p>
 * &#9940; <strong>El minimo es 10 y no puede heredar el 15 de la descripcion
 * inicial.</strong> El texto de refinamiento es <em>aditivo</em> sobre una
 * descripcion que ya existe: "Tenemos dos sedes" son 17 caracteres y es una
 * frase perfectamente informativa. Con el minimo de 30 que llego a circular,
 * los propios botones de relleno rapido que la interfaz ofrece devolvian 400 -
 * "Tenemos dos sedes" (17) y "Tambien hacemos peluqueria" (26)-, que es el peor
 * error posible en un formulario: el usuario hizo literalmente lo que se le
 * pidio.
 */
public record RefineProposalRequest(@NotBlank @Size(min = 43, max = 43) String token,
        @NotBlank @Size(min = 10, max = 400) String text, Long version) {
}
