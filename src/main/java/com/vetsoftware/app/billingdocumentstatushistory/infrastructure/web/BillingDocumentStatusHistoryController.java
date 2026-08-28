package com.vetsoftware.app.billingdocumentstatushistory.infrastructure.web;

import com.vetsoftware.app.auth.infrastructure.security.Authz;
import com.vetsoftware.app.billingdocumentstatushistory.application.port.in.FindBillingDocumentStatusHistoryUseCase;
import com.vetsoftware.app.billingdocumentstatushistory.application.port.in.ListBillingDocumentStatusChangesByStatusUseCase;
import com.vetsoftware.app.billingdocumentstatushistory.application.port.in.ListBillingDocumentStatusHistoryUseCase;
import com.vetsoftware.app.billingdocumentstatushistory.domain.BillingDocumentStatus;
import com.vetsoftware.app.billingdocumentstatushistory.infrastructure.web.response.BillingDocumentStatusHistoryResponse;
import com.vetsoftware.app.infrastructure.web.PageResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * La cara de tenant de la pelicula de los documentos de cobro, y es
 * <strong>solo de lectura</strong>.
 *
 * <p>
 * <strong>No hay {@code POST}, no hay {@code PUT}, no hay {@code PATCH} y no
 * hay {@code DELETE}, en ninguna forma.</strong> No es que la escritura falte
 * por hacer: no va aqui. La historia de estados es una de las seis tablas que
 * el modelo declara irreemplazables, y quien la escribe decide que se puede
 * probar en una disputa; un {@code @PostMapping} en esta clase dejaria al
 * administrador de una clinica apuntando sobre su propia factura la transicion
 * que le conviene. No seria fuga entre empresas —el servicio comprueba que el
 * documento sea suyo— y por eso es peor: la fila es legitima, esta en su sitio,
 * y falsea el expediente. El registro vive en
 * {@link SystemBillingDocumentStatusHistoryController}, igual que
 * {@code PaymentRefundController} deja el suyo en la cara de plataforma.
 *
 * <p>
 * De edicion y borrado no hay ni alli: la tabla solo se agrega, y un fotograma
 * equivocado se corrige apuntando el movimiento inverso, con su motivo, y los
 * dos quedan.
 *
 * <p>
 * La empresa sale siempre de {@code authz.currentCompanyId()} y nunca de la URL
 * ni del cuerpo: es lo que impide leer la pelicula de otra clinica escribiendo
 * su id.
 *
 * <p>
 * <strong>El listado por documento cuelga de {@code /documents/{id}} y aun asi
 * lleva la empresa por debajo.</strong> La ruta acota por la FK ajena, que es
 * comodo para el front y <em>no</em> es un filtro de tenant: el documento es de
 * alguien, y quien escribe ese id en la URL es el cliente.
 */
@RestController
@RequestMapping("/billing-document-status-history")
public class BillingDocumentStatusHistoryController {

    private final FindBillingDocumentStatusHistoryUseCase findUseCase;
    private final ListBillingDocumentStatusHistoryUseCase listByDocumentUseCase;
    private final ListBillingDocumentStatusChangesByStatusUseCase listByStatusUseCase;
    private final Authz authz;

    public BillingDocumentStatusHistoryController(
            FindBillingDocumentStatusHistoryUseCase findUseCase,
            ListBillingDocumentStatusHistoryUseCase listByDocumentUseCase,
            ListBillingDocumentStatusChangesByStatusUseCase listByStatusUseCase, Authz authz) {
        this.findUseCase = findUseCase;
        this.listByDocumentUseCase = listByDocumentUseCase;
        this.listByStatusUseCase = listByStatusUseCase;
        this.authz = authz;
    }

    @GetMapping("/{id}")
    public BillingDocumentStatusHistoryResponse findById(@PathVariable Long id) {
        return BillingDocumentStatusHistoryResponse
                .from(findUseCase.findById(id, authz.currentCompanyId()));
    }

    /** La pelicula de un documento, del primer fotograma al ultimo. */
    @GetMapping("/documents/{billingDocumentId}")
    public PageResponse<BillingDocumentStatusHistoryResponse> listByDocument(
            @PathVariable Long billingDocumentId, @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return PageResponse.from(listByDocumentUseCase.listByDocument(authz.currentCompanyId(),
                billingDocumentId, page, pageSize), BillingDocumentStatusHistoryResponse::from);
    }

    /**
     * La bandeja de vigilancia: los cambios que dejaron un documento en el estado
     * pedido.
     *
     * <p>
     * El estado es obligatorio y no tiene valor por defecto <strong>a
     * proposito</strong>. Un defecto silencioso convertiria la bandeja en un
     * listado cualquiera y quien la mira no notaria que esta contando otra cosa.
     */
    @GetMapping("/by-status")
    public PageResponse<BillingDocumentStatusHistoryResponse> listByStatus(
            @RequestParam BillingDocumentStatus toStatus,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return PageResponse.from(listByStatusUseCase
                .listByCompanyAndToStatus(authz.currentCompanyId(), toStatus, page, pageSize),
                BillingDocumentStatusHistoryResponse::from);
    }
}
