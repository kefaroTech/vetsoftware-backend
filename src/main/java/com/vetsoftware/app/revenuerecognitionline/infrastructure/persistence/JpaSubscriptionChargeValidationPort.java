package com.vetsoftware.app.revenuerecognitionline.infrastructure.persistence;

import com.vetsoftware.app.revenuerecognitionline.application.port.out.SubscriptionChargeValidationPort;
import com.vetsoftware.app.subscriptionbilling.infrastructure.persistence.SubscriptionChargeJpaRepository;
import org.springframework.stereotype.Component;

/**
 * El unico archivo de este slice que conoce a {@code subscriptionbilling}.
 *
 * <p>
 * <strong>Se apoya en {@code findByIdAndCompanyId}, que ya existia, y comprueba
 * solo su presencia.</strong> No lee un getter de la entidad ajena: a este
 * libro no le hace falta ningun campo del cargo —el importe reconocido lo
 * calcula quien llama, con el prorrateo por dias— y depender de la forma de una
 * entidad de otra feature es como un cambio inocente alli rompe esto.
 *
 * <p>
 * <strong>Las dos columnas van juntas a proposito.</strong> Es el espejo Java
 * de la clave foranea compuesta {@code fk_rrl_charge (company_id, charge_id) ->
 * subscription_charges(company_id, id)}: sin la empresa dentro, el
 * reconocimiento de la clinica A podria colgar del cargo de la B y el ingreso
 * de una acabaria contado en el libro de la otra.
 *
 * <p>
 * El nombre de bean va cualificado porque el vertical slicing repite nombres de
 * clase entre features.
 */
@Component("revenueRecognitionLineJpaSubscriptionChargeValidationPort")
public class JpaSubscriptionChargeValidationPort implements SubscriptionChargeValidationPort {

    private final SubscriptionChargeJpaRepository subscriptionChargeJpaRepository;

    public JpaSubscriptionChargeValidationPort(
            SubscriptionChargeJpaRepository subscriptionChargeJpaRepository) {
        this.subscriptionChargeJpaRepository = subscriptionChargeJpaRepository;
    }

    @Override
    public boolean existsByIdAndCompanyId(Long chargeId, Long companyId) {
        return chargeId != null && companyId != null && subscriptionChargeJpaRepository
                .findByIdAndCompanyId(chargeId, companyId).isPresent();
    }
}
