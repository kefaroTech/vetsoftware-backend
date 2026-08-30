package com.vetsoftware.app.aiproposal.application.port.out;

import com.vetsoftware.app.aiproposal.domain.LegalDocumentVersionRef;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * La frontera con {@code legaldocumentversion}: que texto legal se le enseno al
 * prospecto y cual acepto.
 *
 * <p>
 * &#9940; <strong>Una casilla que se valida y no se persiste no es prueba de
 * nada.</strong> Bajo la Ley 1581 pedir el correo antes de entregar nada exige
 * autorizacion previa, expresa e informada, y hasta el changeset 387 este
 * producto no tenia <em>donde</em> escribir que alguien aceptara: se guardaba
 * que texto se mostro -que es la mitad de la prueba, y no la que exige el
 * articulo 9-.
 *
 * <p>
 * <strong>Son dos documentos, no uno</strong>: la politica de privacidad y -
 * porque el texto libre viaja a {@code us-east-1}- la transferencia
 * internacional, que el literal a) del articulo 26 obliga a nombrar con su
 * destino. Dos aceptaciones no caben en una columna, que es por lo que la
 * evidencia vive en una tabla propia y revocable.
 *
 * <p>
 * <strong>Un puerto y no dos</strong>, aunque el nombre canonico seria
 * {@code QueryPort}: resolver la version y escribir la aceptacion son las dos
 * mitades del mismo hecho -no se puede registrar una aceptacion sobre una
 * version que no se resolvio- y partirlas produciria dos adaptadores sobre las
 * mismas dos tablas.
 *
 * <p>
 * &#9940; <strong>Ni un metodo con {@code companyId} ni con {@code Company} en
 * el nombre</strong>, ni siquiera uno que nadie use: esa es la senal exacta con
 * la que {@code LISTADOS_SIN_EMPRESA_SOLO_SYSTEM} decide que un puerto sabe
 * filtrar por empresa, y basta uno para exigir {@code hasRole('SYSTEM')} a todo
 * el que lo consuma -que es justo lo que un prospecto anonimo no puede tener-.
 */
public interface LegalConsentPort {

    /**
     * La version exacta que el prospecto vio, por el par
     * {@code (code, documentVersion)} que devuelve la casilla del front.
     * {@link Optional#empty()} si ese par no existe: la peticion trae una
     * aceptacion que no se puede probar y el caso de uso la rechaza.
     */
    Optional<LegalDocumentVersionRef> findVersion(String code, int documentVersion);

    /**
     * Deja escrita la aceptacion.
     *
     * @param subjectRef
     *            &#9940; el <strong>id</strong> de la propuesta, jamas su
     *            {@code public_token}: el token es el secreto de la URL, y copiarlo
     *            a una segunda tabla lo multiplica por dos y lo saca del control de
     *            acceso que lo protege
     * @param acceptedIpHash
     *            hash, nunca la IP. Puede ser nulo
     */
    void recordAcceptance(Long legalDocumentVersionId, Long subjectRef, LocalDateTime acceptedAt,
            String acceptedIpHash, String userAgentHash);
}
