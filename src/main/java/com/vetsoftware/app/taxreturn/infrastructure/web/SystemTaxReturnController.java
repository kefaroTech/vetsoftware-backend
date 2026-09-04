package com.vetsoftware.app.taxreturn.infrastructure.web;

import com.vetsoftware.app.auth.infrastructure.security.Authz;
import com.vetsoftware.app.infrastructure.web.PageResponse;
import com.vetsoftware.app.taxreturn.application.command.CorrectTaxReturnCommand;
import com.vetsoftware.app.taxreturn.application.command.CreateTaxReturnCommand;
import com.vetsoftware.app.taxreturn.application.command.FileTaxReturnCommand;
import com.vetsoftware.app.taxreturn.application.command.UpdateTaxReturnAmountsCommand;
import com.vetsoftware.app.taxreturn.application.port.in.AnnulTaxReturnUseCase;
import com.vetsoftware.app.taxreturn.application.port.in.CorrectTaxReturnUseCase;
import com.vetsoftware.app.taxreturn.application.port.in.CreateTaxReturnUseCase;
import com.vetsoftware.app.taxreturn.application.port.in.FileTaxReturnUseCase;
import com.vetsoftware.app.taxreturn.application.port.in.FindTaxReturnUseCase;
import com.vetsoftware.app.taxreturn.application.port.in.ListTaxReturnsUseCase;
import com.vetsoftware.app.taxreturn.application.port.in.UpdateTaxReturnUseCase;
import com.vetsoftware.app.taxreturn.infrastructure.web.request.CreateTaxReturnRequest;
import com.vetsoftware.app.taxreturn.infrastructure.web.request.FileTaxReturnRequest;
import com.vetsoftware.app.taxreturn.infrastructure.web.request.UpdateTaxReturnAmountsRequest;
import com.vetsoftware.app.taxreturn.infrastructure.web.response.TaxReturnResponse;
import jakarta.validation.Valid;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Las declaraciones de Lumbre, y <strong>solo desde la consola de
 * plataforma</strong>.
 *
 * <p>
 * <strong>No hay controller de tenant, y no es que falte: no debe
 * existir.</strong> {@code tax_returns} son las declaraciones de la plataforma
 * ante la DIAN y los municipios; una clinica no tiene absolutamente nada que
 * consultar ahi. Los siete puertos van cerrados a {@code hasRole('SYSTEM')} a
 * secas y no hay una sola {@code hasAuthority} —lo que exige ademas
 * {@code GATE_COHERENTE_EN_FEATURE_DE_SYSTEM}—.
 *
 * <p>
 * <strong>Quien presenta la declaracion sale de
 * {@code authz.currentSystemUserId()}, nunca del cuerpo.</strong> Es la firma
 * que queda en {@code filed_by_system_user_id}, y aceptarla por HTTP dejaria
 * firmar una presentacion a nombre de otro superadministrador.
 *
 * <p>
 * <strong>No hay endpoint de borrado.</strong> Un borrador se anula —la fila se
 * queda porque el numero de secuencia ya esta gastado— y una declaracion
 * presentada no se toca: se sucede con una correccion, que es una declaracion
 * nueva del mismo periodo.
 */
@RestController
@RequestMapping("/system/tax-returns")
public class SystemTaxReturnController {

    private final CreateTaxReturnUseCase createUseCase;
    private final UpdateTaxReturnUseCase updateUseCase;
    private final FileTaxReturnUseCase fileUseCase;
    private final CorrectTaxReturnUseCase correctUseCase;
    private final AnnulTaxReturnUseCase annulUseCase;
    private final FindTaxReturnUseCase findUseCase;
    private final ListTaxReturnsUseCase listUseCase;
    private final Authz authz;

    public SystemTaxReturnController(CreateTaxReturnUseCase createUseCase,
            UpdateTaxReturnUseCase updateUseCase, FileTaxReturnUseCase fileUseCase,
            CorrectTaxReturnUseCase correctUseCase, AnnulTaxReturnUseCase annulUseCase,
            FindTaxReturnUseCase findUseCase, ListTaxReturnsUseCase listUseCase, Authz authz) {
        this.createUseCase = createUseCase;
        this.updateUseCase = updateUseCase;
        this.fileUseCase = fileUseCase;
        this.correctUseCase = correctUseCase;
        this.annulUseCase = annulUseCase;
        this.findUseCase = findUseCase;
        this.listUseCase = listUseCase;
        this.authz = authz;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TaxReturnResponse create(@Valid @RequestBody CreateTaxReturnRequest request) {
        return TaxReturnResponse
                .from(createUseCase.execute(new CreateTaxReturnCommand(request.taxKind(),
                        request.fiscalYear(), request.fiscalPeriodKey(), request.municipalityCode(),
                        request.vatFrequency(), request.totalGenerated(), request.totalDeductible(),
                        request.balancePayable(), request.balanceCredit())));
    }

    @PutMapping("/{id}")
    public TaxReturnResponse update(@PathVariable Long id,
            @Valid @RequestBody UpdateTaxReturnAmountsRequest request) {
        return TaxReturnResponse.from(updateUseCase.execute(new UpdateTaxReturnAmountsCommand(id,
                request.totalGenerated(), request.totalDeductible(), request.balancePayable(),
                request.balanceCredit())));
    }

    /**
     * {@code PATCH} y no {@code PUT}: presentar no reemplaza la declaracion, le
     * escribe su desenlace. La fecha la pone el caso de uso con su reloj inyectado.
     */
    @PatchMapping("/{id}/file")
    public TaxReturnResponse file(@PathVariable Long id,
            @Valid @RequestBody FileTaxReturnRequest request) {
        return TaxReturnResponse
                .from(fileUseCase.execute(new FileTaxReturnCommand(id, authz.currentSystemUserId(),
                        request.receiptRef(), request.fileRef(), request.firmezaUntil())));
    }

    /**
     * <strong>{@code POST} y no {@code PATCH}: una correccion CREA una
     * declaracion.</strong> El id de la ruta es el de la que se corrige; lo que
     * devuelve es el borrador nuevo, con el numero de secuencia siguiente. La
     * anterior pasa a {@code CORRECTED} en la misma transaccion, que es lo que
     * libera el hueco de {@code uq_tax_returns_current}.
     */
    @PostMapping("/{id}/corrections")
    @ResponseStatus(HttpStatus.CREATED)
    public TaxReturnResponse correct(@PathVariable Long id,
            @Valid @RequestBody UpdateTaxReturnAmountsRequest request) {
        return TaxReturnResponse.from(correctUseCase.execute(
                new CorrectTaxReturnCommand(id, request.totalGenerated(), request.totalDeductible(),
                        request.balancePayable(), request.balanceCredit())));
    }

    @PatchMapping("/{id}/annul")
    public TaxReturnResponse annul(@PathVariable Long id) {
        return TaxReturnResponse.from(annulUseCase.execute(id));
    }

    @GetMapping("/{id}")
    public TaxReturnResponse findById(@PathVariable Long id) {
        return TaxReturnResponse.from(findUseCase.findById(id));
    }

    @GetMapping
    public PageResponse<TaxReturnResponse> listAll(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return PageResponse.from(listUseCase.listAll(page, pageSize), TaxReturnResponse::from);
    }

    @GetMapping("/by-period/{fiscalPeriodKey}")
    public PageResponse<TaxReturnResponse> listByPeriod(@PathVariable String fiscalPeriodKey,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return PageResponse.from(listUseCase.listByFiscalPeriod(fiscalPeriodKey, page, pageSize),
                TaxReturnResponse::from);
    }

    /**
     * <strong>Las que quedan en firme antes de una fecha.</strong> No es un informe
     * decorativo: de esta consulta sale hasta cuando <em>no</em> se puede purgar el
     * detalle de {@code company_usage_events}, porque el termino de conservacion de
     * los soportes es el de firmeza de la declaracion que sostienen.
     *
     * <p>
     * {@code before} es obligatorio a proposito: sin fecha, el listado devolveria
     * el archivo entero y el endpoint dejaria de significar «lo que esta a punto de
     * quedar en firme».
     */
    @GetMapping("/becoming-final")
    public PageResponse<TaxReturnResponse> listBecomingFinal(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate before,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return PageResponse.from(listUseCase.listBecomingFinalBefore(before, page, pageSize),
                TaxReturnResponse::from);
    }
}
