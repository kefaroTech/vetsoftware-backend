package com.vetsoftware.app.bankreceipt.infrastructure.web;

import com.vetsoftware.app.bankreceipt.application.command.DiscardBankReceiptCommand;
import com.vetsoftware.app.bankreceipt.application.command.IdentifyBankReceiptCommand;
import com.vetsoftware.app.bankreceipt.application.command.RegisterBankReceiptCommand;
import com.vetsoftware.app.bankreceipt.application.port.in.DiscardBankReceiptUseCase;
import com.vetsoftware.app.bankreceipt.application.port.in.FindBankReceiptUseCase;
import com.vetsoftware.app.bankreceipt.application.port.in.IdentifyBankReceiptUseCase;
import com.vetsoftware.app.bankreceipt.application.port.in.ListBankReceiptsUseCase;
import com.vetsoftware.app.bankreceipt.application.port.in.ListUnidentifiedBankReceiptsUseCase;
import com.vetsoftware.app.bankreceipt.application.port.in.RegisterBankReceiptUseCase;
import com.vetsoftware.app.bankreceipt.infrastructure.web.request.RegisterBankReceiptRequest;
import com.vetsoftware.app.bankreceipt.infrastructure.web.response.BankReceiptResponse;
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
 * El extracto bancario, entero y solo desde la consola de plataforma.
 *
 * <h2>No hay controller de tenant, y eso es la decision, no un olvido</h2>
 *
 * <p>
 * Este es el <strong>unico</strong> controller de la feature. No existe un
 * {@code BankReceiptController} bajo la ruta del tenant, y no falta por
 * escribir: el extracto bancario es el cuadre interno de VetSoftware contra su
 * propio banco, y <strong>una entrada sin identificar no tiene todavia
 * dueño</strong>. Esa es literalmente su definicion — una consignacion que
 * llega al banco es un importe, una fecha y una referencia, y averiguar quien
 * la mando es el trabajo que esta bandeja organiza.
 *
 * <p>
 * <strong>Este parrafo existe para el dia que llegue la peticion.</strong>
 * Alguien pedira que la clinica vea «sus» consignaciones para reclamar un abono
 * que no aparece. Quien atienda esa peticion no leera el changelog: leera este
 * javadoc. Abrir el camino de tenant no es añadir un controller con
 * {@code hasAuthority} — es tener que responder antes a una pregunta que hoy no
 * tiene respuesta: <em>que subconjunto de una bandeja donde por definicion no
 * se sabe de quien es cada linea le corresponde a una empresa concreta</em>.
 * Enseñarle la bandeja completa es enseñarle los ingresos de sus competidoras,
 * con importe, fecha y referencia. Lo que si puede abrirse el dia que exista la
 * liquidacion de la pasarela es lo <em>ya conciliado y atribuido</em>, que es
 * otro recurso y no este.
 *
 * <p>
 * Por eso los seis puertos de entrada llevan {@code hasRole('SYSTEM')} a secas
 * —incluida la lectura por id—: sin {@code companyId} en la tabla no hay nada
 * que acotar, y una alternativa por {@code hasAuthority} daria a cualquier
 * empleado autenticado el extracto completo de la plataforma.
 *
 * <p>
 * <strong>Sin borrado.</strong> No hay {@code DELETE} porque una entrada de
 * extracto no se borra: se marca {@code DISCARDED} con
 * {@code PATCH .../discard} y queda a la vista.
 */
@RestController
@RequestMapping("/system/bank-receipts")
public class SystemBankReceiptController {

    private final RegisterBankReceiptUseCase registerUseCase;
    private final IdentifyBankReceiptUseCase identifyUseCase;
    private final DiscardBankReceiptUseCase discardUseCase;
    private final FindBankReceiptUseCase findUseCase;
    private final ListBankReceiptsUseCase listUseCase;
    private final ListUnidentifiedBankReceiptsUseCase listUnidentifiedUseCase;

    public SystemBankReceiptController(RegisterBankReceiptUseCase registerUseCase,
            IdentifyBankReceiptUseCase identifyUseCase, DiscardBankReceiptUseCase discardUseCase,
            FindBankReceiptUseCase findUseCase, ListBankReceiptsUseCase listUseCase,
            ListUnidentifiedBankReceiptsUseCase listUnidentifiedUseCase) {
        this.registerUseCase = registerUseCase;
        this.identifyUseCase = identifyUseCase;
        this.discardUseCase = discardUseCase;
        this.findUseCase = findUseCase;
        this.listUseCase = listUseCase;
        this.listUnidentifiedUseCase = listUnidentifiedUseCase;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BankReceiptResponse register(@Valid @RequestBody RegisterBankReceiptRequest request) {
        return BankReceiptResponse.from(registerUseCase.execute(
                new RegisterBankReceiptCommand(request.bankAccountRef(), request.bankReference(),
                        request.receivedOn(), request.amount(), request.description())));
    }

    /**
     * <strong>{@code PATCH} y sin cuerpo</strong>: la operacion no recibe ningun
     * dato: el esquema no tiene columna para el cliente identificado, asi que
     * identificar es exclusivamente marcar que esto ya no es trabajo pendiente. Un
     * cuerpo vacio obligatorio seria un campo que el front tendria que inventarse.
     */
    @PatchMapping("/{id}/identify")
    public BankReceiptResponse identify(@PathVariable Long id) {
        return BankReceiptResponse
                .from(identifyUseCase.execute(new IdentifyBankReceiptCommand(id)));
    }

    /**
     * <strong>No es un {@code DELETE} aunque saque la entrada de la
     * bandeja.</strong> La fila se queda: el extracto es el espejo de lo que hizo
     * el banco y una linea que desaparece deja el cuadre sin la mitad de su
     * explicacion.
     */
    @PatchMapping("/{id}/discard")
    public BankReceiptResponse discard(@PathVariable Long id) {
        return BankReceiptResponse.from(discardUseCase.execute(new DiscardBankReceiptCommand(id)));
    }

    /**
     * La bandeja de lo no identificado: el trabajo pendiente del mes, de lo mas
     * antiguo a lo mas reciente. Va <em>antes</em> del mapeo por {@code {id}}
     * porque {@code PathPatternParser} da preferencia al literal frente a la
     * variable, pero dejarlo escrito arriba evita que alguien tenga que
     * comprobarlo.
     */
    @GetMapping("/unidentified")
    public PageResponse<BankReceiptResponse> listUnidentified(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return PageResponse.from(listUnidentifiedUseCase.listUnidentified(page, pageSize),
                BankReceiptResponse::from);
    }

    @GetMapping("/{id}")
    public BankReceiptResponse findById(@PathVariable Long id) {
        return BankReceiptResponse.from(findUseCase.findById(id));
    }

    @GetMapping
    public PageResponse<BankReceiptResponse> listAll(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return PageResponse.from(listUseCase.listAll(page, pageSize), BankReceiptResponse::from);
    }
}
