package com.vetsoftware.app.billingdocumentstatushistory.infrastructure.web;

import com.vetsoftware.app.billingdocumentstatushistory.application.command.RecordBillingDocumentStatusChangeCommand;
import com.vetsoftware.app.billingdocumentstatushistory.application.port.in.ListAllBillingDocumentStatusHistoryUseCase;
import com.vetsoftware.app.billingdocumentstatushistory.application.port.in.RecordBillingDocumentStatusChangeUseCase;
import com.vetsoftware.app.billingdocumentstatushistory.infrastructure.web.request.RecordBillingDocumentStatusChangeRequest;
import com.vetsoftware.app.billingdocumentstatushistory.infrastructure.web.response.BillingDocumentStatusHistoryResponse;
import com.vetsoftware.app.infrastructure.web.PageResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * La cara de plataforma: <strong>el unico sitio desde el que se apunta un
 * fotograma</strong>, y la consulta cross-tenant de la pelicula.
 *
 * <p>
 * El registro esta aqui y no en la cara de tenant porque la historia de estados
 * es una de las seis tablas que el modelo declara irreemplazables: quien la
 * escribe decide que se puede probar en una disputa. Un {@code @PostMapping} de
 * cliente dejaria al administrador de una clinica apuntando sobre su propia
 * factura la transicion que le conviene —no seria fuga entre empresas, seria
 * una fila legitima que falsea el expediente—. Es el mismo reparto que
 * {@code PaymentRefundController} y {@code SystemPaymentRefundController}: la
 * lectura si se le deja al cliente.
 *
 * <p>
 * Aqui el {@code companyId} <strong>viaja como {@code @RequestParam}</strong>,
 * no en el cuerpo y no desde el principal: un principal SYSTEM no tiene empresa
 * propia. En el cuerpo no puede ir —lo prohibe la regla dura
 * {@code EMPRESA_NO_VIAJA_EN_EL_CUERPO}, que mira todo {@code @RequestBody} sin
 * mirar la ruta ni el rol—. La proteccion no es que el servidor inyecte la
 * empresa —no puede— sino que el caso de uso esta cerrado a
 * {@code hasRole('SYSTEM')} a secas.
 *
 * <p>
 * Los dos puertos que sirve estan cerrados a {@code hasRole('SYSTEM')} a secas,
 * que es ademas lo que exige {@code LISTADOS_SIN_EMPRESA_SOLO_SYSTEM} para un
 * listado que puede devolver filas de todas las empresas.
 *
 * <p>
 * <strong>Ni aqui hay edicion ni borrado.</strong> La tabla solo se agrega: un
 * fotograma equivocado se corrige apuntando el movimiento inverso, con su
 * motivo, y los dos quedan.
 */
@RestController
@RequestMapping("/system/billing-document-status-history")
public class SystemBillingDocumentStatusHistoryController {

    private final RecordBillingDocumentStatusChangeUseCase recordUseCase;
    private final ListAllBillingDocumentStatusHistoryUseCase listAllUseCase;

    public SystemBillingDocumentStatusHistoryController(
            RecordBillingDocumentStatusChangeUseCase recordUseCase,
            ListAllBillingDocumentStatusHistoryUseCase listAllUseCase) {
        this.recordUseCase = recordUseCase;
        this.listAllUseCase = listAllUseCase;
    }

    /**
     * @param companyId
     *            la empresa cuyo documento cambio de estado. Va como
     *            {@code @RequestParam} porque quien escribe es tesoreria, que no
     *            tiene empresa propia y la elige
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BillingDocumentStatusHistoryResponse record(@RequestParam Long companyId,
            @Valid @RequestBody RecordBillingDocumentStatusChangeRequest request) {
        return BillingDocumentStatusHistoryResponse
                .from(recordUseCase.execute(new RecordBillingDocumentStatusChangeCommand(companyId,
                        request.billingDocumentId(), request.fromStatus(), request.toStatus(),
                        request.actor(), request.reason())));
    }

    /**
     * @param companyId
     *            filtro opcional de la consola. Va como {@code @RequestParam} y no
     *            en el cuerpo porque aqui la empresa la elige quien pregunta, no el
     *            token; la proteccion no es que el servidor la inyecte —no puede—
     *            sino que el caso de uso esta cerrado a plataforma
     */
    @GetMapping
    public PageResponse<BillingDocumentStatusHistoryResponse> listAll(
            @RequestParam(required = false) Long companyId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return PageResponse.from(listAllUseCase.listAll(companyId, page, pageSize),
                BillingDocumentStatusHistoryResponse::from);
    }
}
