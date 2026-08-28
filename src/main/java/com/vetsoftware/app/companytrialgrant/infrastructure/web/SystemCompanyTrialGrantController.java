package com.vetsoftware.app.companytrialgrant.infrastructure.web;

import com.vetsoftware.app.companytrialgrant.application.command.ConsumeTrialGrantCommand;
import com.vetsoftware.app.companytrialgrant.application.command.GrantTrialCommand;
import com.vetsoftware.app.companytrialgrant.application.port.in.ConsumeTrialGrantUseCase;
import com.vetsoftware.app.companytrialgrant.application.port.in.GrantTrialUseCase;
import com.vetsoftware.app.companytrialgrant.application.port.in.ListCompanyTrialGrantsUseCase;
import com.vetsoftware.app.companytrialgrant.application.port.in.ListExpiredTrialGrantsUseCase;
import com.vetsoftware.app.companytrialgrant.infrastructure.web.request.ConsumeTrialGrantRequest;
import com.vetsoftware.app.companytrialgrant.infrastructure.web.request.GrantTrialRequest;
import com.vetsoftware.app.companytrialgrant.infrastructure.web.response.CompanyTrialGrantResponse;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * La consola de plataforma sobre las concesiones de prueba: conceder, resolver
 * y barrer vencimientos.
 *
 * <p>
 * <strong>La empresa entra por la ruta.</strong> No viaja en ningún cuerpo —lo
 * prohíbe {@code EMPRESA_NO_VIAJA_EN_EL_CUERPO}— ni se deriva del principal,
 * porque un usuario de plataforma no tiene empresa. Puede llegar por
 * {@code @PathVariable} porque el gate de las dos escrituras es
 * {@code hasRole('SYSTEM')} a secas: regalar software es una decisión comercial
 * y si admitiera al empleado de la clínica, la administradora se concedería los
 * veintiséis artículos del catálogo.
 *
 * <p>
 * <strong>El barrido de vencimientos cuelga de una ruta plana</strong> y no de
 * una empresa, porque lista filas de todas: es exactamente la familia que
 * {@code LISTADOS_SIN_EMPRESA_SOLO_SYSTEM} obliga a cerrar a plataforma, y su
 * hermano acotado por empresa es {@code ListCompanyTrialGrantsUseCase}, servido
 * por {@link CompanyTrialGrantController}. El día es un parámetro y no el reloj
 * del servidor: quien llama pasa el que corresponda en la zona horaria del
 * negocio, y el último día es inclusivo.
 *
 * <p>
 * <strong>Ninguna ruta borra nada, y no hay {@code DELETE} en toda la
 * rodaja</strong> (R-TRIAL-22). Resolver una prueba es un {@code POST} sobre un
 * sub-recurso porque lo que ocurre es que nace un hecho —cuándo acabó y cómo—,
 * no que desaparezca uno viejo. Ese hecho es lo que sostiene la tasa de
 * conversión por módulo, y también lo que impide regalar dos veces el mismo
 * artículo.
 */
@RestController
@RequestMapping("/system/company-trial-grants")
public class SystemCompanyTrialGrantController {

    private final GrantTrialUseCase grantUseCase;
    private final ConsumeTrialGrantUseCase consumeUseCase;
    private final ListCompanyTrialGrantsUseCase listUseCase;
    private final ListExpiredTrialGrantsUseCase listExpiredUseCase;

    public SystemCompanyTrialGrantController(GrantTrialUseCase grantUseCase,
            ConsumeTrialGrantUseCase consumeUseCase, ListCompanyTrialGrantsUseCase listUseCase,
            ListExpiredTrialGrantsUseCase listExpiredUseCase) {
        this.grantUseCase = grantUseCase;
        this.consumeUseCase = consumeUseCase;
        this.listUseCase = listUseCase;
        this.listExpiredUseCase = listExpiredUseCase;
    }

    /** Concede la prueba de un artículo dentro de la ventana viva de la empresa. */
    @PostMapping("/companies/{companyId}")
    @ResponseStatus(HttpStatus.CREATED)
    public CompanyTrialGrantResponse grant(@PathVariable Long companyId,
            @Valid @RequestBody GrantTrialRequest request) {
        return CompanyTrialGrantResponse.from(grantUseCase.execute(new GrantTrialCommand(companyId,
                request.catalogItemId(), request.grantedOn(), request.daysGranted(),
                request.policyTrialDays(), request.policyTrialOutcome(), request.sourceQuoteId(),
                request.grantingAmendmentId())));
    }

    /**
     * Resuelve una prueba escribiendo su desenlace. La concesión no se borra ni se
     * desactiva: se le añade el hecho de cómo acabó.
     */
    @PostMapping("/companies/{companyId}/catalog-items/{catalogItemId}/consumptions")
    public CompanyTrialGrantResponse consume(@PathVariable Long companyId,
            @PathVariable Long catalogItemId,
            @Valid @RequestBody ConsumeTrialGrantRequest request) {
        return CompanyTrialGrantResponse.from(consumeUseCase.execute(
                new ConsumeTrialGrantCommand(companyId, catalogItemId, request.outcome())));
    }

    /**
     * Las concesiones de una clínica, vistas desde plataforma. Es el mismo puerto
     * que sirve a {@link CompanyTrialGrantController}: su {@code @PreAuthorize}
     * admite {@code hasRole('SYSTEM')} <em>o</em> la propia empresa, y aquí entra
     * por la primera mitad.
     */
    @GetMapping("/companies/{companyId}")
    public List<CompanyTrialGrantResponse> listByCompany(@PathVariable Long companyId) {
        return listUseCase.listByCompanyId(companyId).stream().map(CompanyTrialGrantResponse::from)
                .toList();
    }

    /**
     * El barrido: las pruebas vivas que ya pasaron su fecha, de todas las empresas.
     * Barre por la fecha de cada línea y no por el estado del contrato (R-TRIAL-15)
     * — un día de mora no puede matar la prueba para siempre.
     */
    @GetMapping("/expirations")
    public List<CompanyTrialGrantResponse> listExpired(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate day) {
        return listExpiredUseCase.listLiveExpiredOn(day).stream()
                .map(CompanyTrialGrantResponse::from).toList();
    }
}
