package com.vetsoftware.app.aiproposal.domain;

import java.security.SecureRandom;
import java.util.Base64;

/**
 * El token publico: <strong>la unica frontera de seguridad de toda la
 * feature</strong>.
 *
 * <p>
 * Sin empresa, sin JWT y sin principal, lo unico que separa la propuesta de un
 * prospecto de la de otro es que la URL sea imposible de adivinar. 32 bytes de
 * {@link SecureRandom} en base64url sin relleno son 43 caracteres, que es
 * exactamente lo que valida {@code AiProposal} y lo que declara
 * {@code public_token VARCHAR(43)}.
 *
 * <p>
 * &#9940; <strong>{@code SecureRandom} y no {@code Random} ni
 * {@code UUID.randomUUID()}.</strong> Un {@code Random} sembrado con el reloj
 * es predecible con unas pocas muestras, y de las 128 bits de un UUID v4 seis
 * van fijas en la version y la variante. Aqui el secreto <em>es</em> el control
 * de acceso.
 */
public final class ProposalToken {

    private static final SecureRandom RANDOM = new SecureRandom();

    private static final int BYTES = 32;

    private ProposalToken() {
    }

    public static String nuevo() {
        byte[] entropia = new byte[BYTES];
        RANDOM.nextBytes(entropia);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(entropia);
    }
}
