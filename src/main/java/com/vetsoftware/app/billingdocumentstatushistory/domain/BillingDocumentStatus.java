package com.vetsoftware.app.billingdocumentstatushistory.domain;

/**
 * En que punto de su vida esta un documento de cobro. Dominio cerrado y espejo
 * <strong>literal</strong> de {@code chk_bdsh_statuses}: los cuatro nombres se
 * escriben aqui igual que en la comprobacion, porque
 * {@code @Enumerated(EnumType.STRING)} guarda el {@code name()} tal cual y un
 * valor que la constraint no admita lo rechaza el motor con un error que no
 * menciona ni la columna ni el valor.
 *
 * <p>
 * <strong>Este enum se duplica a proposito y la duplicacion es la regla, no la
 * excepcion.</strong> {@code subscriptionbilling} declara su propio
 * {@code IssueStatus} para la columna {@code issue_status} de la tabla de
 * documentos; importarlo desde aqui seria el cruce de dominios que el vertical
 * slicing prohibe —y que ArchUnit rompe—. Lo que esta feature guarda no es «el
 * estado del documento» sino <em>el hecho de que cambio</em>, y ese hecho es
 * suyo.
 *
 * <p>
 * <strong>Que pasa si los dos enums divergen.</strong> Es el unico riesgo real
 * de la copia, y no es teorico: la razon de ser de esta tabla es reconstruir el
 * estado de la cartera a una fecha —«cuantos documentos estaban esperando
 * factura externa a 31 de marzo»—. Si aqui se anadiera un quinto valor que
 * {@code subscription_billing_documents.issue_status} no admite, esa
 * reconstruccion devolveria para algun documento un estado que la tabla de
 * documentos <em>no puede tener</em>: un informe fiscal que no cuadra contra
 * nada y que nadie sabria como leer. Al reves —quitar aqui un valor que alla
 * existe— la pelicula se corta y el documento aparece detenido en un estado que
 * ya abandono.
 *
 * <p>
 * La barandilla que impide que la divergencia entre en la base es
 * {@code chk_bdsh_statuses}, que enumera los cuatro valores contra los mismos
 * que acepta la tabla de documentos. Tocar este enum <strong>obliga</strong> a
 * revisar esa comprobacion y la de la otra feature en el mismo cambio.
 */
public enum BillingDocumentStatus {

    /** Emitido en el sistema y todavia sin representacion externa. */
    DRAFT,

    /** Pendiente de que el proveedor externo registre la factura. */
    AWAITING_EXTERNAL,

    /** Ya existe la factura externa que lo respalda. */
    EXTERNAL_REGISTERED,

    /**
     * Anulado. Es terminal en la practica —de aqui no se sale— pero el enum no lo
     * impone: quien decide que transiciones son legales es el modelo de
     * {@code subscriptionbilling}, no la bitacora que las apunta. Esta feature solo
     * exige que el cambio <em>sea</em> un cambio.
     */
    VOIDED
}
