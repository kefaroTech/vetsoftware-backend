package com.vetsoftware.app.electronicdocument.application.usecase;

import com.vetsoftware.app.electronicdocument.application.command.RegisterPosSaleCommand;
import com.vetsoftware.app.electronicdocument.application.command.RegisterPosSaleCommand.SaleLine;
import com.vetsoftware.app.electronicdocument.application.port.out.CatalogLineQueryPort;
import com.vetsoftware.app.electronicdocument.application.port.out.CatalogLineQueryPort.CatalogItem;
import com.vetsoftware.app.electronicdocument.application.port.out.CompanyFiscalProfileQueryPort;
import com.vetsoftware.app.electronicdocument.application.port.out.CompanyFiscalProfileQueryPort.CompanyFiscalProfile;
import com.vetsoftware.app.electronicdocument.application.port.out.ElectronicDocumentRepository;
import com.vetsoftware.app.electronicdocument.application.port.out.SaleCustomerQueryPort;
import com.vetsoftware.app.electronicdocument.application.port.out.SaleCustomerQueryPort.SaleCustomer;
import com.vetsoftware.app.electronicdocument.domain.CustomerSnapshot;
import com.vetsoftware.app.electronicdocument.domain.ElectronicDocument;
import com.vetsoftware.app.electronicdocument.domain.ElectronicDocumentLine;
import com.vetsoftware.app.electronicdocument.domain.ElectronicDocumentPayment;
import com.vetsoftware.app.electronicdocument.domain.PaymentForm;
import com.vetsoftware.app.electronicdocument.domain.TaxCategory;
import com.vetsoftware.app.electronicdocument.domain.TaxScheme;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Construye el documento PENDIENTE desde una venta de POS (payload, no cuenta cerrada). Reusable y SIN
 * control de acceso. El emisor (issuer) y las tarifas de retencion salen del perfil fiscal de la empresa;
 * la clasificacion tributaria de cada linea la pone el catalogo (autoritativa, no el cliente). El documento
 * nace SIN numero: la numeracion fiscal se asigna en la emision (justo antes de transmitir).
 */
@Component
public class PosSaleDocumentBuilder {
    private static final String UNIT_MEASURE = "94"; // UN/ECE: unidad
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final CompanyFiscalProfileQueryPort fiscalProfileQueryPort;
    private final SaleCustomerQueryPort saleCustomerQueryPort;
    private final CatalogLineQueryPort catalogLineQueryPort;
    private final ElectronicDocumentRepository repository;

    public PosSaleDocumentBuilder(CompanyFiscalProfileQueryPort fiscalProfileQueryPort,
                                  SaleCustomerQueryPort saleCustomerQueryPort,
                                  CatalogLineQueryPort catalogLineQueryPort,
                                  ElectronicDocumentRepository repository) {
        this.fiscalProfileQueryPort = fiscalProfileQueryPort;
        this.saleCustomerQueryPort = saleCustomerQueryPort;
        this.catalogLineQueryPort = catalogLineQueryPort;
        this.repository = repository;
    }

    public ElectronicDocument build(RegisterPosSaleCommand command) {
        Long companyId = command.companyId();
        CompanyFiscalProfile fiscal = fiscalProfileQueryPort.findByCompany(companyId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "La empresa no tiene perfil fiscal (CompanyTaxProfile) configurado: " + companyId));

        boolean finalConsumer = command.finalConsumer() || command.customerOwnerId() == null;
        CustomerSnapshot customer;
        boolean withholdingAgent;
        if (finalConsumer) {
            customer = CustomerSnapshot.finalConsumer();
            withholdingAgent = false;
        } else {
            SaleCustomer sc = saleCustomerQueryPort.findOwner(command.customerOwnerId(), companyId)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Adquiriente (owner) no encontrado: " + command.customerOwnerId()));
            customer = sc.snapshot();
            withholdingAgent = sc.withholdingAgent();
        }

        List<ElectronicDocumentLine> lines = new ArrayList<>();
        int n = 0;
        for (SaleLine l : command.lines()) {
            lines.add(toLine(++n, l, companyId));
        }
        List<ElectronicDocumentPayment> payments = command.payments().stream()
                .map(p -> new ElectronicDocumentPayment(null, p.means(), p.amount()))
                .toList();

        ElectronicDocument document = ElectronicDocument.createPending(
                companyId, null /* sin cuenta abierta: la venta POS ES el registro */,
                command.documentType(), fiscal.issuer(), customer, lines, payments,
                PaymentForm.CONTADO, null,
                withholdingAgent, fiscal.reteFuenteRate(), fiscal.reteIvaRate(), fiscal.reteIcaRate());
        return repository.save(document);
    }

    private ElectronicDocumentLine toLine(int lineNumber, SaleLine l, Long companyId) {
        if (l.quantity() == null || l.quantity().signum() <= 0)
            throw new IllegalArgumentException("line quantity must be > 0");
        if (l.unitPrice() == null || l.unitPrice().signum() < 0)
            throw new IllegalArgumentException("line unitPrice is required");

        String description;
        TaxCategory category;
        TaxScheme scheme;
        BigDecimal rate;
        switch (l.kind()) {
            case PRODUCT -> {
                CatalogItem item = requireCatalog(catalogLineQueryPort.findProduct(refId(l), companyId), "Producto", l);
                description = item.name();
                category = item.taxCategory();
                scheme = item.taxScheme();
                rate = item.taxRate();
            }
            case SERVICE -> {
                CatalogItem item = requireCatalog(catalogLineQueryPort.findService(refId(l), companyId), "Servicio", l);
                description = item.name();
                category = item.taxCategory();
                scheme = item.taxScheme();
                rate = item.taxRate();
            }
            case GENERAL -> {
                if (l.description() == null || l.description().isBlank())
                    throw new IllegalArgumentException("general line requires a description");
                description = l.description();
                category = TaxCategory.EXCLUIDO; // los items libres del POS no tributan IVA
                scheme = null;
                rate = null;
            }
            default -> throw new IllegalArgumentException("unsupported sale line kind: " + l.kind());
        }

        // El unitPrice del POS viene con IVA incluido (post-promo). Si la linea tributa, se extrae la base.
        BigDecimal gross = l.unitPrice().multiply(l.quantity()).setScale(2, RoundingMode.HALF_UP);
        boolean taxed = scheme != null && rate != null && rate.signum() > 0;
        BigDecimal base = taxed
                ? gross.divide(BigDecimal.ONE.add(rate.divide(HUNDRED, 6, RoundingMode.HALF_UP)), 2, RoundingMode.HALF_UP)
                : gross;
        BigDecimal taxAmount = gross.subtract(base);
        return new ElectronicDocumentLine(null, lineNumber, description, l.quantity(), UNIT_MEASURE,
                l.unitPrice(), base, category, taxed ? scheme : null, taxed ? rate : null, taxAmount, gross);
    }

    private static Long refId(SaleLine l) {
        if (l.refId() == null)
            throw new IllegalArgumentException(l.kind() + " sale line requires refId");
        return l.refId();
    }

    private static CatalogItem requireCatalog(java.util.Optional<CatalogItem> item, String label, SaleLine l) {
        return item.orElseThrow(() -> new IllegalArgumentException(
                label + " no encontrado o no pertenece a la empresa: " + l.refId()));
    }
}
