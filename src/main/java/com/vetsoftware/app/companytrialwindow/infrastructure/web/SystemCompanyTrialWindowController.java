package com.vetsoftware.app.companytrialwindow.infrastructure.web;

import com.vetsoftware.app.companytrialwindow.application.command.OpenTrialWindowCommand;
import com.vetsoftware.app.companytrialwindow.application.port.in.CloseTrialWindowUseCase;
import com.vetsoftware.app.companytrialwindow.application.port.in.FindCurrentTrialWindowUseCase;
import com.vetsoftware.app.companytrialwindow.application.port.in.OpenTrialWindowUseCase;
import com.vetsoftware.app.companytrialwindow.infrastructure.web.request.OpenTrialWindowRequest;
import com.vetsoftware.app.companytrialwindow.infrastructure.web.response.CompanyTrialWindowResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * La consola de plataforma sobre el reloj de prueba: abrir, cerrar y auditar.
 *
 * <p>
 * <strong>La empresa entra por la ruta y solo por la ruta.</strong> No viaja en
 * ningún cuerpo —lo prohíbe {@code EMPRESA_NO_VIAJA_EN_EL_CUERPO}— y tampoco se
 * deriva del principal, porque un usuario de plataforma no tiene empresa:
 * {@code authz.currentCompanyId()} lanzaría {@code AccessDeniedException}.
 * Puede llegar por {@code @PathVariable} precisamente porque el gate de las dos
 * escrituras es {@code hasRole('SYSTEM')} a secas y elegir empresa es lo que
 * ese principal tiene que poder hacer.
 *
 * <p>
 * <strong>Cerrar es un {@code POST} sobre un sub-recurso, nunca un
 * {@code DELETE}.</strong> Cerrar no borra la ventana ni la acorta: escribe la
 * fecha en que dejó de estar abierta, que es lo que libera el marcador de «una
 * abierta por empresa» y permite que una campaña de recuperación abra otra años
 * después. Lo que ocurre es que nace un hecho nuevo, no que desaparezca uno
 * viejo — y por eso las concesiones que colgaban de ella conservan sus fechas.
 *
 * <p>
 * <strong>Y no hay ninguna operación que estire una ventana</strong>
 * (R-TRIAL-10, D-54). No es un hueco que rellenar más adelante: el fin de la
 * ventana está copiado dentro de cada concesión y atado por clave foránea con
 * {@code ON UPDATE RESTRICT}, así que moverlo con pruebas colgando muere en el
 * motor. Si comercial quiere dar otra oportunidad, cierra esta y abre otra, que
 * queda registrada.
 */
@RestController
@RequestMapping("/system/company-trial-windows")
public class SystemCompanyTrialWindowController {

    private final OpenTrialWindowUseCase openUseCase;
    private final CloseTrialWindowUseCase closeUseCase;
    private final FindCurrentTrialWindowUseCase findUseCase;

    public SystemCompanyTrialWindowController(OpenTrialWindowUseCase openUseCase,
            CloseTrialWindowUseCase closeUseCase, FindCurrentTrialWindowUseCase findUseCase) {
        this.openUseCase = openUseCase;
        this.closeUseCase = closeUseCase;
        this.findUseCase = findUseCase;
    }

    /**
     * Abre el reloj. Se dispara al aceptar la cotización, el único camino de alta.
     */
    @PostMapping("/companies/{companyId}")
    @ResponseStatus(HttpStatus.CREATED)
    public CompanyTrialWindowResponse open(@PathVariable Long companyId,
            @Valid @RequestBody OpenTrialWindowRequest request) {
        return CompanyTrialWindowResponse
                .from(openUseCase.execute(new OpenTrialWindowCommand(companyId, request.startDate(),
                        request.windowDays(), request.sourceQuoteId())));
    }

    /**
     * Cierra el reloj vivo de la empresa. Un segundo cierre responde 409: la
     * ventana ya estaba cerrada y volver a escribir la fecha borraría cuándo lo
     * estuvo de verdad.
     */
    @PostMapping("/companies/{companyId}/closures")
    public CompanyTrialWindowResponse close(@PathVariable Long companyId) {
        return CompanyTrialWindowResponse.from(closeUseCase.execute(companyId));
    }

    /**
     * El reloj vivo de una clínica, visto desde plataforma. Es el mismo puerto que
     * sirve a {@link CompanyTrialWindowController}: su {@code @PreAuthorize} admite
     * {@code hasRole('SYSTEM')} <em>o</em> la propia empresa, y aquí entra por la
     * primera mitad.
     */
    @GetMapping("/companies/{companyId}/current")
    public CompanyTrialWindowResponse current(@PathVariable Long companyId) {
        return CompanyTrialWindowResponse.from(findUseCase.findOpenByCompanyId(companyId));
    }
}
