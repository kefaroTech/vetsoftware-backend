package com.vetsoftware.app.openaccount.infrastructure.persistence;

import com.vetsoftware.app.debtopenaccount.infrastructure.persistence.DebtOpenAccountJpaRepository;
import com.vetsoftware.app.generalchargeopenaccount.infrastructure.persistence.GeneralChargeOpenAccountJpaRepository;
import com.vetsoftware.app.openaccount.application.port.out.OpenAccountTotalsPort;
import com.vetsoftware.app.productchargeopenaccount.infrastructure.persistence.ProductChargeOpenAccountJpaRepository;
import com.vetsoftware.app.servicechargeopenaccount.infrastructure.persistence.ServiceChargeOpenAccountJpaRepository;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/**
 * Computes the derived totals of an OpenAccount by aggregating its child charge/payment tables.
 * This is the single place that crosses into the four child slices' persistence (allowed
 * cross-feature reference: persistence -> persistence). Each child repository filters {@code
 * enabled = true} explicitly in its aggregate query.
 */
@Component
public class JpaOpenAccountTotalsAdapter implements OpenAccountTotalsPort {

  private final ProductChargeOpenAccountJpaRepository productChargeRepository;
  private final ServiceChargeOpenAccountJpaRepository serviceChargeRepository;
  private final GeneralChargeOpenAccountJpaRepository generalChargeRepository;
  private final DebtOpenAccountJpaRepository debtRepository;

  public JpaOpenAccountTotalsAdapter(
      ProductChargeOpenAccountJpaRepository productChargeRepository,
      ServiceChargeOpenAccountJpaRepository serviceChargeRepository,
      GeneralChargeOpenAccountJpaRepository generalChargeRepository,
      DebtOpenAccountJpaRepository debtRepository) {
    this.productChargeRepository = productChargeRepository;
    this.serviceChargeRepository = serviceChargeRepository;
    this.generalChargeRepository = generalChargeRepository;
    this.debtRepository = debtRepository;
  }

  @Override
  public BigDecimal totalCharges(Long openAccountId) {
    BigDecimal products =
        nullToZero(productChargeRepository.sumChargesByOpenAccountId(openAccountId));
    BigDecimal services =
        nullToZero(serviceChargeRepository.sumChargesByOpenAccountId(openAccountId));
    BigDecimal general =
        nullToZero(generalChargeRepository.sumChargesByOpenAccountId(openAccountId));
    return products.add(services).add(general);
  }

  @Override
  public BigDecimal totalPayments(Long openAccountId) {
    return nullToZero(debtRepository.sumPaymentsByOpenAccountId(openAccountId));
  }

  private static BigDecimal nullToZero(BigDecimal value) {
    return value == null ? BigDecimal.ZERO : value;
  }
}
