package com.vetsoftware.app.aiproposal.application.command;

/**
 * Un anadido sobre una propuesta que ya existe.
 *
 * <p>
 * &#9940; <strong>El token viaja en el cuerpo, nunca en un segmento de
 * ruta</strong> (plan S4.2.1). {@code RequestLoggingContextFilter} mete
 * {@code getRequestURI()} en el MDC en <em>toda</em> peticion, la clave esta
 * declarada en {@code LogFieldPolicy.SCANNED} -es decir, sometida al
 * enmascarado por patrones- y ningun patron de {@code LogRedactor} casa con 43
 * caracteres de base64url sueltos: {@code JWT} exige el prefijo {@code eyJ} y
 * tres segmentos, {@code KEYED_VALUE} exige una clave sensible delante. El
 * token -la unica frontera de autorizacion de la feature- acabaria en claro en
 * CloudWatch y en Loki, con 31 dias de retencion. Es el precedente que ya
 * dejaron el reseteo de contrasena y las dos validaciones del alta de
 * plataforma.
 *
 * @param expectedVersion
 *            la {@code version} que el cliente leyo. Sin ella dos pestanas se
 *            pisan en silencio
 */
public record RefineProposalCommand(String publicToken, String text, Long expectedVersion) {

    public RefineProposalCommand {
        if (publicToken == null || publicToken.isBlank())
            throw new IllegalArgumentException("publicToken is required");
        if (text == null || text.isBlank())
            throw new IllegalArgumentException("text is required");
    }
}
