package com.vetsoftware.app.subscriptionbilling.domain;

/**
 * Companion VO del contrato al que pertenece lo que se devenga y lo que se
 * cobra.
 *
 * <p>
 * Vive aquí y no se importa de {@code subscription} porque el vertical slicing
 * lo prohíbe: este slice guarda su propia lectura del contrato y la resuelve
 * por {@code SubscriptionQueryPort}, cuya única variante declarada está acotada
 * por empresa.
 *
 * <p>
 * <b>El {@code companyId} forma parte del VO a propósito.</b> Es la mitad
 * aplicativa de las FK compuestas: la base impide que un cargo de una clínica
 * cuelgue del contrato de otra porque la clave arrastra la empresa
 * ({@code fk_subscription_charges_subscription} sobre
 * {@code (company_id, subscription_id)}), y este VO impide que el código llegue
 * siquiera a intentarlo. No es una regla que haya que recordar: es
 * estructuralmente imposible en los dos lados.
 */
public record SubscriptionRef(Long id, Long companyId) {

    public SubscriptionRef {
        if (id == null)
            throw new IllegalArgumentException("subscription id is required");
        if (companyId == null)
            throw new IllegalArgumentException("subscription companyId is required");
    }

    /**
     * Comprueba que el contrato es de la empresa que dice el caller.
     *
     * <p>
     * Redundante con el {@code findByIdAndCompanyId} que lo trajo y con la FK
     * compuesta, y las tres capas son deliberadas: un pago de una clínica saldando
     * la factura de otra no se detecta en revisión de código porque no es un error
     * de programación, es un hueco de esquema. Aquí se paga una comparación.
     */
    public void exigirEmpresa(Long expectedCompanyId) {
        if (!companyId.equals(expectedCompanyId))
            throw new IllegalArgumentException(
                    "subscription " + id + " does not belong to company " + expectedCompanyId);
    }
}
