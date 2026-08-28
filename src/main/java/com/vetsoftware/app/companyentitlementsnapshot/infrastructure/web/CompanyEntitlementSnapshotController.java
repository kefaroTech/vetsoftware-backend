package com.vetsoftware.app.companyentitlementsnapshot.infrastructure.web;

import com.vetsoftware.app.auth.infrastructure.security.Authz;
import com.vetsoftware.app.companyentitlementsnapshot.application.port.in.FindEntitlementSnapshotAsOfUseCase;
import com.vetsoftware.app.companyentitlementsnapshot.infrastructure.web.response.CompanyEntitlementSnapshotResponse;
import java.time.LocalDateTime;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Qué veía mi clínica un día concreto: <strong>solo lectura</strong>.
 *
 * <p>
 * Es la pregunta entera por la que existe la tabla —un salto y una fila—.
 * {@code company_entitlements} se reescribe en cada recálculo, así que sin esta
 * foto «demuéstrame qué permisos tenía el 3 de marzo» no tiene respuesta, ni
 * para el cliente que reclama ni para quien le contesta.
 *
 * <p>
 * <strong>La empresa la pone el backend</strong> con
 * {@code authz.currentCompanyId()}: no viaja en ninguna ruta ni en ningún
 * cuerpo. El puerto la revalida con {@code @authz.isMyCompany(#companyId)}.
 *
 * <p>
 * <strong>Guardar una foto no se publica por HTTP, ni aquí ni en el controlador
 * de plataforma.</strong> {@code RecordEntitlementSnapshotUseCase} admite al
 * tenant en su gate, y tiene que hacerlo porque el recálculo se dispara desde
 * la propia clínica —es la palanca de reparación—; pero ese llamador es
 * {@code CompanyEntitlementSnapshotAdapter}, dentro de la misma transacción del
 * recálculo, no un cliente HTTP. Un endpoint que aceptara el {@code payload} de
 * fuera dejaría que quien reclama escriba él mismo la prueba con la que
 * reclama, y una prueba que fabrica el interesado no prueba nada. La foto se
 * produce como consecuencia de un recálculo y por ningún otro camino; el
 * recálculo sí es público, en {@code POST /entitlements/recalculate}.
 */
@RestController
@RequestMapping("/company-entitlement-snapshots")
public class CompanyEntitlementSnapshotController {

    private final FindEntitlementSnapshotAsOfUseCase findUseCase;
    private final Authz authz;

    public CompanyEntitlementSnapshotController(FindEntitlementSnapshotAsOfUseCase findUseCase,
            Authz authz) {
        this.findUseCase = findUseCase;
        this.authz = authz;
    }

    /**
     * La última foto anterior o igual al instante pedido. Un 404 significa que la
     * empresa no tenía ninguna foto todavía a esa fecha, que es una respuesta
     * correcta y no un fallo.
     */
    @GetMapping
    public CompanyEntitlementSnapshotResponse latestAsOf(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime at) {
        return CompanyEntitlementSnapshotResponse
                .from(findUseCase.findLatestAsOf(authz.currentCompanyId(), at));
    }
}
