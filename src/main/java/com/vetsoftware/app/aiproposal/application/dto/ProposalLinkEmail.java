package com.vetsoftware.app.aiproposal.application.dto;

import java.time.LocalDateTime;

/**
 * Todo lo que el correo del enlace puede llevar. <strong>Y la lista es
 * deliberadamente esta y no una mas.</strong>
 *
 * <p>
 * &#9940; <strong>No hay aqui ni un campo de texto libre, ni los motivos, ni el
 * nombre de la clinica, ni los codigos del carrito, ni un resumen.</strong> La
 * direccion del prospecto <strong>no esta verificada</strong> -la escribio el
 * en un formulario anonimo y nadie comprobo que sea suya-, asi que cualquiera
 * puede hacer que este producto, con su SPF y su DKIM en regla, entregue un
 * mensaje a un tercero. Si el cuerpo llevara prosa del modelo -que la escribe
 * el atacante a traves del texto libre- eso seria un rele de phishing firmado
 * por nosotros, y peor que uno de un dominio cualquiera precisamente porque el
 * nuestro pasa los filtros.
 *
 * <p>
 * Lo que se lleva es: el destinatario, el token que abre el enlace y la fecha
 * de caducidad. La propuesta se ve al abrir el enlace, que es donde el control
 * de acceso existe. Un record cerrado convierte esa decision en algo que hay
 * que romper a proposito para incumplirla, en vez de en una nota que alguien
 * lee seis meses despues.
 *
 * <p>
 * <strong>Si algun dia se decide meter contenido</strong>, pasa por el mismo
 * {@code ProposalReasonSanitizer} del servidor y por el escapado de HTML del
 * renderizador de correo. Se escribe como decision, no se hereda.
 */
public record ProposalLinkEmail(String contactEmail, String publicToken, LocalDateTime expiresAt) {

    public ProposalLinkEmail {
        if (contactEmail == null || contactEmail.isBlank()) {
            throw new IllegalArgumentException("contactEmail is required");
        }
        if (publicToken == null || publicToken.isBlank()) {
            throw new IllegalArgumentException("publicToken is required");
        }
        if (expiresAt == null) {
            throw new IllegalArgumentException("expiresAt is required");
        }
    }
}
