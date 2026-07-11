package com.vetsoftware.app.electronicdocument.application.usecase;

import com.vetsoftware.app.electronicdocument.application.command.RegisterPosSaleCommand;
import com.vetsoftware.app.electronicdocument.application.command.RegisterPosSaleCommand.SaleLine;
import com.vetsoftware.app.electronicdocument.application.port.out.BranchResolverPort;
import com.vetsoftware.app.electronicdocument.application.port.out.CatalogLineQueryPort;
import com.vetsoftware.app.electronicdocument.application.port.out.CatalogLineQueryPort.CatalogItem;
import com.vetsoftware.app.electronicdocument.application.port.out.CompanyFiscalProfileQueryPort;
import com.vetsoftware.app.electronicdocument.application.port.out.CompanyFiscalProfileQueryPort.CompanyFiscalProfile;
import com.vetsoftware.app.electronicdocument.application.port.out.ElectronicDocumentRepository;
import com.vetsoftware.app.electronicdocument.application.port.out.SaleCustomerQueryPort;
import com.vetsoftware.app.electronicdocument.application.port.out.SaleCustomerQueryPort.SaleCustomer;
import com.vetsoftware.app.electronicdocument.application.port.out.SalePromotionQueryPort;
import com.vetsoftware.app.electronicdocument.application.port.out.UvtQueryPort;
import com.vetsoftware.app.electronicdocument.application.port.out.SalePromotionQueryPort.SalePromotion;
import com.vetsoftware.app.electronicdocument.domain.CustomerSnapshot;
import com.vetsoftware.app.electronicdocument.domain.ElectronicDocument;
import com.vetsoftware.app.electronicdocument.domain.ElectronicDocumentLine;
import com.vetsoftware.app.electronicdocument.domain.ElectronicDocumentPayment;
import com.vetsoftware.app.electronicdocument.domain.PaymentForm;
import com.vetsoftware.app.electronicdocument.domain.TaxCategory;
import com.vetsoftware.app.electronicdocument.domain.TaxScheme;
import com.vetsoftware.app.shared.domain.Money;
import java.math.BigDecimal;
import java.time.LocalDate;
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
    /** Tolerancia (1 peso) al validar el unitPrice del cliente contra el precio canonico: absorbe el redondeo a peso entero (COP sin centavos). */
    private static final BigDecimal PRICE_TOLERANCE = BigDecimal.ONE;

    private final CompanyFiscalProfileQueryPort fiscalProfileQueryPort;
    private final SaleCustomerQueryPort saleCustomerQueryPort;
    private final CatalogLineQueryPort catalogLineQueryPort;
    private final SalePromotionQueryPort salePromotionQueryPort;
    private final ElectronicDocumentRepository repository;
    private final PosTicketLimitValidator posTicketLimitValidator;
    private final UvtQueryPort uvtQueryPort;
    private final BranchResolverPort branchResolverPort;

    public PosSaleDocumentBuilder(CompanyFiscalProfileQueryPort fiscalProfileQueryPort,
                                  SaleCustomerQueryPort saleCustomerQueryPort,
                                  CatalogLineQueryPort catalogLineQueryPort,
                                  SalePromotionQueryPort salePromotionQueryPort,
                                  ElectronicDocumentRepository repository,
                                  PosTicketLimitValidator posTicketLimitValidator,
                                  UvtQueryPort uvtQueryPort,
                                  BranchResolverPort branchResolverPort) {
        this.fiscalProfileQueryPort = fiscalProfileQueryPort;
        this.saleCustomerQueryPort = saleCustomerQueryPort;
        this.catalogLineQueryPort = catalogLineQueryPort;
        this.salePromotionQueryPort = salePromotionQueryPort;
        this.repository = repository;
        this.posTicketLimitValidator = posTicketLimitValidator;
        this.uvtQueryPort = uvtQueryPort;
        this.branchResolverPort = branchResolverPort;
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

        List<SalePromotion> activePromos = salePromotionQueryPort.findActive(companyId, LocalDate.now());
        List<ElectronicDocumentLine> lines = new ArrayList<>();
        int n = 0;
        for (SaleLine l : command.lines()) {
            lines.add(toLine(++n, l, companyId, activePromos));
        }
        List<ElectronicDocumentPayment> payments = command.payments().stream()
                .map(p -> new ElectronicDocumentPayment(null, p.means(), p.amount()))
                .toList();

        BigDecimal uvt = uvtQueryPort.currentUvt().orElse(null);
        // Sede emisora: request branchId validado contra la empresa, o la "Principal" por defecto.
        Long branchId = branchResolverPort.resolve(companyId, command.branchId())
                .orElseThrow(() -> new IllegalArgumentException("La empresa no tiene sucursal: " + companyId));
        ElectronicDocument document = ElectronicDocument.createPending(
                companyId, null /* sin cuenta abierta: la venta POS ES el registro */,
                command.documentType(), fiscal.issuer(), customer, lines, payments,
                PaymentForm.CONTADO,
                withholdingAgent, fiscal.reteFuenteRate(), fiscal.reteIvaRate(), fiscal.reteIcaRate(),
                uvt, command.clientRequestId(), command.issuedByEmployeeId(), branchId);
        // 3.2: un tiquete POS (DOC_EQUIV_POS) no puede superar 5 UVT; por encima exige FE_VENTA.
        posTicketLimitValidator.validate(document);
        return repository.save(document);
    }

    private ElectronicDocumentLine toLine(int lineNumber, SaleLine l, Long companyId, List<SalePromotion> promos) {
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
                validateUnitPrice(l, item, promos);
            }
            case SERVICE -> {
                CatalogItem item = requireCatalog(catalogLineQueryPort.findService(refId(l), companyId), "Servicio", l);
                description = item.name();
                category = item.taxCategory();
                scheme = item.taxScheme();
                rate = item.taxRate();
                validateUnitPrice(l, item, promos);
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

        // El unitPrice del POS viene con IVA incluido (post-promo). Solo se extrae base cuando hay tarifa > 0
        // (GRAVADO/INC). EXENTO conserva su esquema IVA con tasa 0: base = bruto, IVA 0, pero mantiene el
        // esquema para que el XML lo distinga de EXCLUIDO (sin esquema). EXCLUIDO/GENERAL viajan sin esquema.
        BigDecimal gross = Money.multiply(l.unitPrice(), l.quantity());
        boolean hasPositiveRate = rate != null && rate.signum() > 0;
        BigDecimal base = hasPositiveRate ? Money.extractBase(gross, rate) : gross;
        BigDecimal taxAmount = gross.subtract(base);
        return new ElectronicDocumentLine(null, lineNumber, description, l.quantity(), UNIT_MEASURE,
                l.unitPrice(), base, category, scheme, rate, taxAmount, gross);
    }

    /**
     * El servidor es la autoridad del precio: recalcula el precio canonico (lista del catalogo + promo activa)
     * y rechaza la linea si el unitPrice del cliente se desvia mas alla de la tolerancia. Asi un usuario no
     * puede emitir un documento fiscal con un monto arbitrario aunque manipule el payload del POS.
     */
    private void validateUnitPrice(SaleLine l, CatalogItem item, List<SalePromotion> promos) {
        BigDecimal expected = PromotionPriceCalculator.expectedUnitPrice(
                item.basePrice(), l.kind(), l.refId(), item.categoryId(), promos);
        if (l.unitPrice().subtract(expected).abs().compareTo(PRICE_TOLERANCE) > 0) {
            throw new IllegalArgumentException(
                    "El precio unitario de la linea no coincide con el catalogo (" + l.kind() + " refId "
                            + l.refId() + "): esperado " + expected + ", recibido " + l.unitPrice());
        }
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
