package com.vetsoftware.app.paymentattempt.application.port.out;

/**
 * La FK compuesta {@code (company_id, billing_document_id)} a
 * {@code subscription_billing_documents}, que es de otra feature.
 *
 * <p>
 * Es un {@code ValidationPort} y no un {@code QueryPort} porque esta feature
 * <strong>no lee ningun campo del documento</strong>: el importe intentado y la
 * pasarela los trae el propio intento. Es el caso que {@code CLAUDE.md}
 * describe como "no necesitas datos del agregado externo, solo el ID".
 *
 * <p>
 * <strong>Acotado por empresa, y no es decorativo.</strong>
 * {@code subscription_billing_documents} pertenece a una empresa, asi que la
 * variante ancha permitiria colgar el intento de esta clinica de la factura de
 * la vecina — la cuarta forma de fuga de BE-COV, la que no se apropia de nada
 * sino que cuelga lo tuyo de un padre ajeno—.
 */
public interface BillingDocumentValidationPort {

    /** {@code true} si el documento existe y pertenece a esa empresa. */
    boolean existsByIdAndCompanyId(Long billingDocumentId, Long companyId);

    /**
     * Toma un candado de escritura sobre la fila del documento de cobro. <strong>Es
     * lo unico que serializa a dos intentos concurrentes sobre la misma
     * factura</strong>, y por eso vive en este puerto aunque «bloquear» no sea
     * literalmente «validar»: el documento es el padre comun de las dos invariantes
     * que este slice tiene que sostener y que la base no puede expresar por si
     * sola.
     *
     * <p>
     * <strong>Se bloquea el padre porque el hijo todavia no existe.</strong> La
     * carrera es sobre <em>insertar</em> una fila de {@code payment_attempts}, asi
     * que no hay ninguna fila hija que bloquear; y bajo {@code READ_COMMITTED} —el
     * aislamiento que este camino necesita, ver
     * {@code RecordPaymentAttemptService}— InnoDB no toma candados de hueco, de
     * modo que un {@code FOR UPDATE} sobre los intentos ya existentes tampoco
     * impediria el insercion rival. Bloquear la fila preexistente del padre es la
     * forma portable de serializar altas en una coleccion hija, y acota el candado
     * exactamente al alcance de las dos invariantes —el documento— y ni un
     * milimetro mas: dos cobros de facturas distintas no se estorban.
     *
     * <p>
     * <strong>Acotado por empresa, y aqui importa mas que en la lectura.</strong>
     * Con la variante ancha se retendria durante toda la transaccion la fila de
     * otro tenant, <em>antes</em> de cualquier comprobacion — es el defecto que
     * {@code JpaOpenAccountQueryPort.lockForUpdate} ya documenta. Si el documento
     * no es de esta empresa no devuelve fila y no bloquea nada; quien reporta el
     * error es {@link #existsByIdAndCompanyId}, no este metodo.
     *
     * <p>
     * Sin resultado a proposito: el objetivo es el candado, no la fila.
     */
    void lockByIdAndCompanyId(Long billingDocumentId, Long companyId);
}
