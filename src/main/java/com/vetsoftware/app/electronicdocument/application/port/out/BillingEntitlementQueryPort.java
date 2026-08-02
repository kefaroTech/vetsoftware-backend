package com.vetsoftware.app.electronicdocument.application.port.out;

/**
 * ¿La empresa tiene habilitada la facturación electrónica? Es así solo si su membresía incluye el
 * submódulo con código BILLING. Cuando es false, los documentos se guardan localmente pero no se
 * numeran fiscalmente ni se transmiten al proveedor DIAN (MATIAS).
 */
public interface BillingEntitlementQueryPort {
  boolean isElectronicInvoicingEnabled(Long companyId);
}
