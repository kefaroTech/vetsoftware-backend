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
import com.vetsoftware.app.electronicdocument.domain.FiscalResponsibility;
import com.vetsoftware.app.electronicdocument.domain.TaxRegime;
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
        OpenAccountJpaEntity account = openAccountRepository.findByIdAndCompany_Id(openAccountId, companyId)
                .orElse(null);
        if (account == null) {
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
                TaxRegime.fromName(owner.getTaxRegime() == null ? null : owner.getTaxRegime().name()),
                FiscalResponsibility.fromName(
                        owner.getFiscalResponsibility() == null ? null : owner.getFiscalResponsibility().name()));

        List<ElectronicDocumentLine> lines = buildLines(openAccountId, companyId);
        List<ElectronicDocumentPayment> payments = buildPayments(openAccountId, companyId);
        boolean closed = account.getStatus() == OpenAccountStatus.CLOSE;

        // F6 - retenciones: el adquiriente es agente retenedor + las tarifas configuradas por el emisor.
        boolean withholdingAgent = owner.isWithholdingAgent();

        return Optional.of(new SaleSnapshot(companyId, openAccountId, closed, issuer, customer,
                lines, payments, PaymentForm.CONTADO, withholdingAgent,
                fiscalProfile.reteFuenteRate(), fiscalProfile.reteIvaRate(), fiscalProfile.reteIcaRate()));
    }

    private List<ElectronicDocumentLine> buildLines(Long openAccountId, Long companyId) {
        List<ElectronicDocumentLine> lines = new ArrayList<>();
        int n = 0;
        for (var c : productChargeRepository.findByOpenAccount_IdAndOpenAccount_Company_Id(
                openAccountId, companyId)) {
            if (c.isVoided()) continue;
            String description = c.getProduct() == null ? "Producto" : c.getProduct().getName();
            lines.add(line(++n, description, BigDecimal.valueOf(c.getQuantity()), c.getUnitPrice(), c.getBaseAmount(),
                    c.isHasTax(), c.getTaxPercentage(), c.getTaxAmount(), c.getTotalAmount(),
                    c.getTaxScheme(), c.getTaxTreatment()));
        }
        for (var c : serviceChargeRepository.findByOpenAccount_IdAndOpenAccount_Company_Id(
                openAccountId, companyId)) {
            if (c.isVoided()) continue;
            String description = c.getService() == null ? "Servicio" : c.getService().getName();
            lines.add(line(++n, description, ONE, c.getUnitPrice(), c.getBaseAmount(),
                    c.isHasTax(), c.getTaxPercentage(), c.getTaxAmount(), c.getTotalAmount(),
                    c.getTaxScheme(), c.getTaxTreatment()));
        }
        for (var c : generalChargeRepository.findByOpenAccount_IdAndOpenAccount_Company_Id(
                openAccountId, companyId)) {
            if (c.isVoided()) continue;
            // Cargo general (sin catálogo): no tiene tratamiento congelado → null cae a la heurística
            // (EXCLUIDO si no tributa; GRAVADO/INC si lleva impuesto). No existe "general exento".
            lines.add(line(++n, c.getName(), c.getQuantity(), c.getUnitAmount(), c.getBaseAmount(),
                    c.isHasTax(), c.getTaxPercentage(), c.getTaxAmount(), c.getTotalAmount(),
                    c.getTaxScheme(), null));
        }
        return lines;
    }

    /**
     * Deriva la clasificacion tributaria de la linea desde lo congelado en el cargo (taxTreatment + esquema +
     * tasa). EXENTO -> (EXENTO, IVA, 0%): la DIAN lo distingue de EXCLUIDO (sin esquema). GRAVADO/INC -> su
     * esquema y tasa. Los cargos previos a congelar el tratamiento (frozenTreatment null) caen a la heuristica:
     * gravado -> (GRAVADO/INC, esquema); sin impuesto -> (EXCLUIDO, null).
     */
    private ElectronicDocumentLine line(int lineNumber, String description, BigDecimal quantity,
                                        BigDecimal unitPrice, BigDecimal base, boolean hasTax,
                                        BigDecimal rate, BigDecimal taxAmount, BigDecimal total,
                                        String frozenScheme, String frozenTreatment) {
        // EXENTO = gravado a tarifa 0%: lleva esquema IVA con tasa 0 (la base ya es el total y el IVA 0). Es lo
        // que permite al XML diferenciarlo de un excluido (que no lleva esquema).
        if ("EXENTO".equals(frozenTreatment)) {
            return new ElectronicDocumentLine(null, lineNumber, description, quantity, UNIT_MEASURE,
                    unitPrice, base, TaxCategory.EXENTO, TaxScheme.IVA, BigDecimal.ZERO, taxAmount, total);
        }
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

    private List<ElectronicDocumentPayment> buildPayments(Long openAccountId, Long companyId) {
        List<ElectronicDocumentPayment> payments = new ArrayList<>();
        for (var d : debtRepository.findByOpenAccount_IdAndOpenAccount_Company_Id(openAccountId, companyId)) {
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
