package com.vetsoftware.app.gatewaysettlement.infrastructure.web;

import com.vetsoftware.app.gatewaysettlement.application.command.AttachProviderInvoiceCommand;
import com.vetsoftware.app.gatewaysettlement.application.command.LinkBankReceiptCommand;
import com.vetsoftware.app.gatewaysettlement.application.command.RegisterGatewaySettlementCommand;
import com.vetsoftware.app.gatewaysettlement.application.port.in.AttachProviderInvoiceUseCase;
import com.vetsoftware.app.gatewaysettlement.application.port.in.FindGatewaySettlementUseCase;
import com.vetsoftware.app.gatewaysettlement.application.port.in.LinkBankReceiptUseCase;
import com.vetsoftware.app.gatewaysettlement.application.port.in.ListGatewaySettlementsUseCase;
import com.vetsoftware.app.gatewaysettlement.application.port.in.ReconcileGatewaySettlementUseCase;
import com.vetsoftware.app.gatewaysettlement.application.port.in.RegisterGatewaySettlementUseCase;
import com.vetsoftware.app.gatewaysettlement.infrastructure.web.request.AttachProviderInvoiceRequest;
import com.vetsoftware.app.gatewaysettlement.infrastructure.web.request.LinkBankReceiptRequest;
import com.vetsoftware.app.gatewaysettlement.infrastructure.web.request.RegisterGatewaySettlementRequest;
import com.vetsoftware.app.gatewaysettlement.infrastructure.web.response.GatewaySettlementReconciliationResponse;
import com.vetsoftware.app.gatewaysettlement.infrastructure.web.response.GatewaySettlementResponse;
import com.vetsoftware.app.infrastructure.web.PageResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Las liquidaciones de la pasarela, enteras y solo desde la consola de
 * plataforma.
 *
 * <h2>No hay controller de tenant, y eso es LA decision de esta feature</h2>
 *
 * <p>
 * Este es el <strong>unico</strong> controller de la rodaja. No existe un
 * {@code GatewaySettlementController} bajo la ruta del tenant y no falta por
 * escribir: <strong>una fila de {@code gateway_settlements} agrupa los cobros
 * de muchas clinicas</strong>. La pasarela paga en lotes y con dias de retraso,
 * juntando sesenta cobros de sesenta empresas distintas en un unico abono con
 * un unico bruto, una unica comision y un unico neto. No hay forma de acotar
 * esa fila por empresa porque no es de una empresa.
 *
 * <p>
 * <strong>La fuga concreta, escrita para el dia que llegue la
 * peticion.</strong> El detalle del pago de un cliente ya le enseña su
 * {@code settlementReference} —esta en {@code subscription_payments}, y con
 * razon: es como el cliente sabe en que remesa viajo su cobro—. El siguiente
 * paso que cualquiera propondria es hacer ese dato pinchable, «para que vea su
 * liquidacion». <b>Si esa referencia abre el lote, le estas mostrando los
 * importes de las otras cincuenta y nueve</b>: cuanto factura cada competidora
 * suya al mes, con fecha. Quien atienda esa peticion no leera el changelog,
 * leera este javadoc.
 *
 * <p>
 * Lo que <em>si</em> se puede abrir el dia que se pida es lo ya conciliado y
 * atribuido a ESE cobro —su importe, su fecha, si cuadro— que es otro recurso,
 * vive en la rodaja del pago y esta acotado por empresa. La regla es simple: la
 * referencia del lote puede ser una etiqueta que el cliente lee, nunca una
 * llave que el cliente usa.
 *
 * <p>
 * Por eso los seis puertos de entrada llevan {@code hasRole('SYSTEM')} a secas
 * —incluidas la lectura por id y la conciliacion—: sin {@code companyId} en la
 * tabla no hay nada que acotar, y una alternativa por {@code hasAuthority}
 * daria a cualquier empleado autenticado el mapa de ingresos de la plataforma
 * entera.
 *
 * <p>
 * <strong>Sin borrado.</strong> No hay {@code DELETE} porque un lote es lo que
 * hizo la pasarela: la clave hacia atras desde {@code subscription_payments} es
 * {@code ON DELETE RESTRICT} justamente para que una liquidacion no pueda
 * desaparecer dejando sin explicacion los cobros que colgaban de ella.
 */
@RestController
@RequestMapping("/system/gateway-settlements")
public class SystemGatewaySettlementController {

    private final RegisterGatewaySettlementUseCase registerUseCase;
    private final AttachProviderInvoiceUseCase attachProviderInvoiceUseCase;
    private final LinkBankReceiptUseCase linkBankReceiptUseCase;
    private final FindGatewaySettlementUseCase findUseCase;
    private final ListGatewaySettlementsUseCase listUseCase;
    private final ReconcileGatewaySettlementUseCase reconcileUseCase;

    public SystemGatewaySettlementController(RegisterGatewaySettlementUseCase registerUseCase,
            AttachProviderInvoiceUseCase attachProviderInvoiceUseCase,
            LinkBankReceiptUseCase linkBankReceiptUseCase, FindGatewaySettlementUseCase findUseCase,
            ListGatewaySettlementsUseCase listUseCase,
            ReconcileGatewaySettlementUseCase reconcileUseCase) {
        this.registerUseCase = registerUseCase;
        this.attachProviderInvoiceUseCase = attachProviderInvoiceUseCase;
        this.linkBankReceiptUseCase = linkBankReceiptUseCase;
        this.findUseCase = findUseCase;
        this.listUseCase = listUseCase;
        this.reconcileUseCase = reconcileUseCase;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public GatewaySettlementResponse register(
            @Valid @RequestBody RegisterGatewaySettlementRequest request) {
        return GatewaySettlementResponse.from(
                registerUseCase.execute(new RegisterGatewaySettlementCommand(request.gateway(),
                        request.settlementReference(), request.grossAmount(), request.feeAmount(),
                        request.feeTaxAmount(), request.gmfAmount(), request.netAmount(),
                        request.paymentCount(), request.settledOn())));
    }

    /**
     * <strong>{@code PATCH} y no {@code PUT}</strong>: escribe dos columnas de un
     * lote que ya existe y deja las demas intactas. Es la llegada de la factura del
     * proveedor, que ocurre dias despues de la liquidacion.
     */
    @PatchMapping("/{id}/provider-invoice")
    public GatewaySettlementResponse attachProviderInvoice(@PathVariable Long id,
            @Valid @RequestBody AttachProviderInvoiceRequest request) {
        return GatewaySettlementResponse
                .from(attachProviderInvoiceUseCase.execute(new AttachProviderInvoiceCommand(id,
                        request.providerInvoiceRef(), request.providerTaxId())));
    }

    /** Ata el lote a la linea del extracto por la que entro su neto. */
    @PatchMapping("/{id}/bank-receipt")
    public GatewaySettlementResponse linkBankReceipt(@PathVariable Long id,
            @Valid @RequestBody LinkBankReceiptRequest request) {
        return GatewaySettlementResponse.from(linkBankReceiptUseCase
                .execute(new LinkBankReceiptCommand(id, request.bankReceiptId())));
    }

    /**
     * <b>Si dice 37 y hay 36, hay un pago perdido.</b> Es el endpoint por el que
     * {@code payment_count} existe: convierte en una consulta la revision a ojo que
     * hoy se hace todos los meses.
     *
     * <p>
     * Va <em>antes</em> del mapeo por {@code {id}} porque {@code PathPatternParser}
     * da preferencia al literal frente a la variable, pero al ser un sufijo de
     * {@code /{id}} no compiten; dejarlo escrito arriba evita que alguien tenga que
     * comprobarlo.
     */
    @GetMapping("/{id}/reconciliation")
    public GatewaySettlementReconciliationResponse reconcile(@PathVariable Long id) {
        return GatewaySettlementReconciliationResponse.from(reconcileUseCase.reconcile(id));
    }

    @GetMapping("/{id}")
    public GatewaySettlementResponse findById(@PathVariable Long id) {
        return GatewaySettlementResponse.from(findUseCase.findById(id));
    }

    @GetMapping
    public PageResponse<GatewaySettlementResponse> listAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return PageResponse.from(listUseCase.listAll(page, pageSize),
                GatewaySettlementResponse::from);
    }
}
