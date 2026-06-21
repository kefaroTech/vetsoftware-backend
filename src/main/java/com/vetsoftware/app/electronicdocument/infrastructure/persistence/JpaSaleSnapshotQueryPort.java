package com.vetsoftware.app.electronicdocument.infrastructure.persistence;

import com.vetsoftware.app.debtopenaccount.domain.PaymentMethod;
import com.vetsoftware.app.debtopenaccount.infrastructure.persistence.DebtOpenAccountJpaRepository;
import com.vetsoftware.app.electronicdocument.application.port.out.CompanyFiscalProfileQueryPort;
import com.vetsoftware.app.electronicdocument.application.port.out.CompanyFiscalProfileQueryPort.CompanyFiscalProfile;
import com.vetsoftware.app.electronicdocument.application.port.out.SaleSnapshotQueryPort;
import com.vetsoftware.app.electronicdocument.domain.CustomerSnapshot;
import com.vetsoftware.app.electronicdocument.domain.ElectronicDocumentLine;
import com.vetsoftware.app.electronicdocument.domain.ElectronicDocumentPayment;
import com.vetsoftware.app.electronicdocument.domain.IssuerSnapshot;
import com.vetsoftware.app.electronicdocument.domain.PaymentForm;
import com.vetsoftware.app.electronicdocument.domain.PaymentMeans;
import com.vetsoftware.app.electronicdocument.domain.TaxCategory;
import com.vetsoftware.app.electronicdocument.domain.TaxScheme;
import com.vetsoftware.app.generalchargeopenaccount.infrastructure.persistence.GeneralChargeOpenAccountJpaRepository;
import com.vetsoftware.app.openaccount.domain.OpenAccountStatus;
import com.vetsoftware.app.openaccount.infrastructure.persistence.OpenAccountJpaEntity;
import com.vetsoftware.app.openaccount.infrastructure.persistence.OpenAccountJpaRepository;
import com.vetsoftware.app.owner.infrastructure.persistence.OwnerJpaEntity;
import com.vetsoftware.app.productchargeopenaccount.infrastructure.persistence.ProductChargeOpenAccountJpaRepository;
import com.vetsoftware.app.servicechargeopenaccount.infrastructure.persistence.ServiceChargeOpenAccountJpaRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Traduce los datos de otras features al read model de esta. Es el unico punto que conoce
 * open account, cargos, perfil fiscal, owner y abonos. Construye los snapshots de emisor/adquiriente,
 * mapea cada cargo no anulado a una linea (deriva categoria/esquema tributario del impuesto congelado)
 * y cada abono no anulado a un pago con medio codificado DIAN.
 */
@Component
public class JpaSaleSnapshotQueryPort implements SaleSnapshotQueryPort {
    private static final String UNIT_MEASURE = "94"; // UN/ECE: unidad
    private static final BigDecimal ONE = BigDecimal.ONE;

    private final OpenAccountJpaRepository openAccountRepository;
    private final ProductChargeOpenAccountJpaRepository productChargeRepository;
    private final ServiceChargeOpenAccountJpaRepository serviceChargeRepository;
    private final GeneralChargeOpenAccountJpaRepository generalChargeRepository;
    private final DebtOpenAccountJpaRepository debtRepository;
    private final CompanyFiscalProfileQueryPort fiscalProfileQueryPort;

    public JpaSaleSnapshotQueryPort(OpenAccountJpaRepository openAccountRepository,
                                    ProductChargeOpenAccountJpaRepository productChargeRepository,
                                    ServiceChargeOpenAccountJpaRepository serviceChargeRepository,
                                    GeneralChargeOpenAccountJpaRepository generalChargeRepository,
                                    DebtOpenAccountJpaRepository debtRepository,
                                    CompanyFiscalProfileQueryPort fiscalProfileQueryPort) {
        this.openAccountRepository = openAccountRepository;
        this.productChargeRepository = productChargeRepository;
        this.serviceChargeRepository = serviceChargeRepository;
        this.generalChargeRepository = generalChargeRepository;
        this.debtRepository = debtRepository;
        this.fiscalProfileQueryPort = fiscalProfileQueryPort;
    }

    @Override
    public Optional<SaleSnapshot> findByOpenAccount(Long openAccountId, Long companyId) {
        OpenAccountJpaEntity account = openAccountRepository.findById(openAccountId).orElse(null);
        if (account == null || account.getCompany() == null
                || !companyId.equals(account.getCompany().getId())) {
            return Optional.empty();
        }

        CompanyFiscalProfile fiscalProfile = fiscalProfileQueryPort.findByCompany(companyId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "La empresa no tiene perfil fiscal (CompanyTaxProfile) configurado: " + companyId));
        IssuerSnapshot issuer = fiscalProfile.issuer();

        OwnerJpaEntity owner = account.getOwner();
        CustomerSnapshot customer = new CustomerSnapshot(
                owner.getDocumentType() == null ? null : owner.getDocumentType().name(),
                owner.getDocument(), owner.getVerificationDigit(),
                owner.getPersonType() == null ? null : owner.getPersonType().name(),
                owner.getLegalName(), owner.getName(), owner.getEmail(),
                owner.getCity() == null ? null : owner.getCity().getDaneCode(),
                owner.getTaxRegime() == null ? null : owner.getTaxRegime().name());

        List<ElectronicDocumentLine> lines = buildLines(openAccountId);
        List<ElectronicDocumentPayment> payments = buildPayments(openAccountId);
        boolean closed = account.getStatus() == OpenAccountStatus.CLOSE;

        // F6 - retenciones: el adquiriente es agente retenedor + las tarifas configuradas por el emisor.
        boolean withholdingAgent = owner.isWithholdingAgent();

        return Optional.of(new SaleSnapshot(companyId, openAccountId, closed, issuer, customer,
                lines, payments, PaymentForm.CONTADO, withholdingAgent,
                fiscalProfile.reteFuenteRate(), fiscalProfile.reteIvaRate(), fiscalProfile.reteIcaRate()));
    }

    private List<ElectronicDocumentLine> buildLines(Long openAccountId) {
        List<ElectronicDocumentLine> lines = new ArrayList<>();
        int n = 0;
        for (var c : productChargeRepository.findByOpenAccountId(openAccountId)) {
            if (c.isVoided()) continue;
            String description = c.getProduct() == null ? "Producto" : c.getProduct().getName();
            lines.add(line(++n, description, ONE, c.getUnitPrice(), c.getBaseAmount(),
                    c.isHasTax(), c.getTaxPercentage(), c.getTaxAmount(), c.getTotalAmount(),
                    c.getTaxScheme()));
        }
        for (var c : serviceChargeRepository.findByOpenAccountId(openAccountId)) {
            if (c.isVoided()) continue;
            String description = c.getService() == null ? "Servicio" : c.getService().getName();
            lines.add(line(++n, description, ONE, c.getUnitPrice(), c.getBaseAmount(),
                    c.isHasTax(), c.getTaxPercentage(), c.getTaxAmount(), c.getTotalAmount(),
                    c.getTaxScheme()));
        }
        for (var c : generalChargeRepository.findByOpenAccountId(openAccountId)) {
            if (c.isVoided()) continue;
            lines.add(line(++n, c.getName(), c.getQuantity(), c.getUnitAmount(), c.getBaseAmount(),
                    c.isHasTax(), c.getTaxPercentage(), c.getTaxAmount(), c.getTotalAmount(),
                    c.getTaxScheme()));
        }
        return lines;
    }

    /**
     * Deriva la clasificacion tributaria de la linea desde el impuesto congelado del cargo. F2: los cargos
     * solo congelaron has_tax + tasa (no taxTreatment/taxScheme, que no existian al crearse), asi que
     * gravado -> (GRAVADO, IVA); sin impuesto -> (EXCLUIDO, null). El INC fino llegara cuando los cargos
     * congelen su esquema (mejora futura).
     */
    private ElectronicDocumentLine line(int lineNumber, String description, BigDecimal quantity,
                                        BigDecimal unitPrice, BigDecimal base, boolean hasTax,
                                        BigDecimal rate, BigDecimal taxAmount, BigDecimal total,
                                        String frozenScheme) {
        boolean taxed = hasTax && rate != null && rate.signum() > 0;
        // El esquema (IVA/INC) se congeló en el cargo; si falta (cargos previos) cae a IVA, que era el
        // unico esquema que el cierre soportaba. INC tributa con su propia categoria/esquema, como el POS.
        boolean inc = "INC".equals(frozenScheme);
        TaxCategory category = !taxed ? TaxCategory.EXCLUIDO : (inc ? TaxCategory.INC : TaxCategory.GRAVADO);
        TaxScheme scheme = !taxed ? null : (inc ? TaxScheme.INC : TaxScheme.IVA);
        BigDecimal effectiveRate = taxed ? rate : null;
        return new ElectronicDocumentLine(null, lineNumber, description, quantity, UNIT_MEASURE,
                unitPrice, base, category, scheme, effectiveRate, taxAmount, total);
    }

    private List<ElectronicDocumentPayment> buildPayments(Long openAccountId) {
        List<ElectronicDocumentPayment> payments = new ArrayList<>();
        for (var d : debtRepository.findByOpenAccountId(openAccountId)) {
            if (d.isVoided()) continue;
            payments.add(new ElectronicDocumentPayment(null, toPaymentMeans(d.getPaymentMethod()), d.getAmount()));
        }
        return payments;
    }

    private static PaymentMeans toPaymentMeans(PaymentMethod method) {
        return switch (method) {
            case CASH -> PaymentMeans.EFECTIVO;
            case CARD -> PaymentMeans.TARJETA_CREDITO;
            case BANK_TRANSFER -> PaymentMeans.TRANSFERENCIA;
        };
    }
}
