package com.vetsoftware.app.infrastructure.email;

/**
 * Escapado de texto que se inyecta en el HTML de un correo.
 *
 * <p>
 * <b>Vive aqui y no en cada feature porque hace falta por tercera vez</b>, y
 * porque la tercera es la que cambia de naturaleza. Las dos primeras eran
 * defensivas: nombres de veterinaria y codigos de empleado escritos por
 * usuarios ya autenticados, donde escapar solo evitaba romper el markup. La
 * tercera es un control de seguridad de verdad.
 *
 * <p>
 * <b>El problema concreto.</b> Las plantillas de Resend de este repositorio
 * usan la forma de triple llave, que en Mustache y Handlebars significa
 * insercion <b>sin escapar</b>. Mientras las variables las escribieran usuarios
 * autenticados eso era inocuo. Deja de serlo en cuanto el texto lo escribe un
 * anonimo y el destinatario del correo es la persona que puede crear
 * superadministradores de plataforma: un motivo de solicitud que contenga un
 * bloque con su propio enlace produce un correo del remitente legitimo, con la
 * marca correcta, y un boton que apunta al atacante — y con estilos en linea
 * puede ademas ocultar el enlace real.
 *
 * <p>
 * <b>Esto es la mitad de la defensa, no toda.</b> La otra mitad es usar doble
 * llave en la plantilla de Resend para esas variables. Cinturon y tirantes: el
 * escapado en Java es lo que controlamos y probamos; el doble-stache protege el
 * dia que alguien anada otra variable sin acordarse de escaparla.
 *
 * <p>
 * No cubre la inyeccion de cabeceras: un salto de linea en el asunto es otro
 * problema y se ataja de otra forma —el asunto es una constante y nunca se
 * construye con datos de quien solicita—.
 */
public final class HtmlEscaper {

    private HtmlEscaper() {
    }

    /** {@code null} se convierte en cadena vacia, no en el literal "null". */
    public static String escape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }
}
