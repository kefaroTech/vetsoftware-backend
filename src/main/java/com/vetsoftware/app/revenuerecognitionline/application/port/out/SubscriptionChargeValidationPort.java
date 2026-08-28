package com.vetsoftware.app.revenuerecognitionline.application.port.out;

/**
 * La clave foranea compuesta {@code fk_rrl_charge}
 * {@code (company_id, charge_id) -> subscription_charges(company_id, id)}, que
 * es de otra feature.
 *
 * <p>
 * <strong>El metodo recibe las DOS columnas, y esa es toda la gracia.</strong>
 * Un {@code existsById(chargeId)} pelado dejaria colgar el reconocimiento de la
 * clinica A del cargo de la clinica B: el ingreso de una acabaria contado en el
 * libro de la otra. La clave foranea compuesta lo impide en la base —por eso la
 * especificacion exigio {@code uq_subscription_charges_company_id} como
 * prerrequisito— y esta firma lo impide aqui, para que el fallo salga como «ese
 * cargo no es de esa empresa» y no como una violacion de integridad sin
 * explicacion.
 *
 * <p>
 * {@code ValidationPort} y no {@code QueryPort} porque de este slice no se lee
 * un solo campo del cargo: el importe reconocido lo calcula quien llama, con el
 * prorrateo por dias que este libro no conoce.
 */
public interface SubscriptionChargeValidationPort {

    /** {@code true} si ese cargo existe <b>y</b> pertenece a esa empresa. */
    boolean existsByIdAndCompanyId(Long chargeId, Long companyId);
}
