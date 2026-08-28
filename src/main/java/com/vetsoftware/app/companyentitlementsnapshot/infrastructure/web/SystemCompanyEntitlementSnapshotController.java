package com.vetsoftware.app.companyentitlementsnapshot.infrastructure.web;

import com.vetsoftware.app.companyentitlementsnapshot.application.port.in.FindEntitlementSnapshotAsOfUseCase;
import com.vetsoftware.app.companyentitlementsnapshot.infrastructure.web.response.CompanyEntitlementSnapshotResponse;
import java.time.LocalDateTime;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * La misma foto, vista desde plataforma: qué veía <em>esa</em> clínica un día
 * concreto.
 *
 * <p>
 * <strong>La empresa entra por la ruta y solo por la ruta.</strong> No se
 * deriva del principal porque un usuario de plataforma no tiene empresa:
 * {@code authz.currentCompanyId()} lanzaría {@code AccessDeniedException}. Es
 * el mismo puerto que sirve a {@link CompanyEntitlementSnapshotController} —su
 * {@code @PreAuthorize} admite {@code hasRole('SYSTEM')} <em>o</em> la propia
 * empresa— y aquí entra por la primera mitad.
 *
 * <p>
 * <strong>Que exista este controlador es lo que hace útil a la tabla en
 * soporte.</strong> Quien atiende la reclamación no es la clínica: si la única
 * lectura fuera la del tenant, contestar «qué permisos tenías el 3 de marzo»
 * exigiría pedirle al cliente que lo mirara él, que es justo lo contrario de
 * una prueba.
 *
 * <p>
 * <strong>Tampoco aquí se escribe la foto.</strong> El motivo está escrito en
 * el controlador del tenant y no cambia por el actor: la foto es consecuencia
 * de un recálculo, y un endpoint que aceptara el {@code payload} de fuera
 * convertiría la bitácora probatoria en un formulario.
 */
@RestController
@RequestMapping("/system/company-entitlement-snapshots")
public class SystemCompanyEntitlementSnapshotController {

    private final FindEntitlementSnapshotAsOfUseCase findUseCase;

    public SystemCompanyEntitlementSnapshotController(
            FindEntitlementSnapshotAsOfUseCase findUseCase) {
        this.findUseCase = findUseCase;
    }

    @GetMapping("/companies/{companyId}")
    public CompanyEntitlementSnapshotResponse latestAsOf(@PathVariable Long companyId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime at) {
        return CompanyEntitlementSnapshotResponse.from(findUseCase.findLatestAsOf(companyId, at));
    }
}
