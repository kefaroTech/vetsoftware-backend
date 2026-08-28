package com.vetsoftware.app.revenuerecognitionline.infrastructure.web;

import com.vetsoftware.app.infrastructure.web.PageResponse;
import com.vetsoftware.app.revenuerecognitionline.application.port.in.FindRevenueRecognitionLineUseCase;
import com.vetsoftware.app.revenuerecognitionline.application.port.in.ListRevenueRecognitionLinesUseCase;
import com.vetsoftware.app.revenuerecognitionline.infrastructure.web.response.RevenueRecognitionLineResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * El libro de reconocimiento de ingreso, y es <strong>solo de lectura</strong>.
 *
 * <h2>Por que no hay alta, y por que eso no es una feature a medias</h2>
 *
 * <p>
 * {@code revenue_recognition_lines} es un <em>libro derivado</em>: cada renglon
 * sale del prorrateo de un {@code subscription_charges}, no de que alguien
 * escriba un importe. Un {@code POST} aqui permitiria inventar ingreso que
 * ningun cargo respalda, y el libro dejaria de cuadrar contra la cartera <b>sin
 * que nada lo delate</b> — ninguna constraint puede comprobar que un
 * reconocimiento corresponde a lo realmente devengado. La escritura vive en
 * {@code RecordRevenueRecognitionUseCase}, que es un puerto de entrada sin
 * endpoint: solo lo alcanza el proceso que factura.
 *
 * <p>
 * Y por lo mismo no hay {@code PUT}, {@code PATCH} ni {@code DELETE}: un
 * reconocimiento mal calculado se compensa con otra fila de signo contrario,
 * nunca se reescribe. Editarlo cambiaria el ingreso de un periodo <em>ya
 * declarado</em>.
 *
 * <h2>Todo cerrado a plataforma, incluida la lectura por empresa</h2>
 *
 * <p>
 * <strong>El {@code companyId} viaja por la query string y no sale de
 * {@code authz.currentCompanyId()}</strong>, al reves que en los controllers de
 * tenant. Es el patron de las rutas de plataforma: un principal SYSTEM no tiene
 * empresa propia y elige de que clinica quiere el libro. Los tres puertos van
 * cerrados a {@code hasRole('SYSTEM')} a secas, que es lo que exigen
 * {@code OPERACIONES_POR_ID_SIN_EMPRESA_SOLO_SYSTEM} para el {@code findById} y
 * {@code LISTADOS_SIN_EMPRESA_SOLO_SYSTEM} para el barrido por periodo.
 */
@RestController
@RequestMapping("/system/revenue-recognition-lines")
public class SystemRevenueRecognitionLineController {

    private final FindRevenueRecognitionLineUseCase findUseCase;
    private final ListRevenueRecognitionLinesUseCase listUseCase;

    public SystemRevenueRecognitionLineController(FindRevenueRecognitionLineUseCase findUseCase,
            ListRevenueRecognitionLinesUseCase listUseCase) {
        this.findUseCase = findUseCase;
        this.listUseCase = listUseCase;
    }

    @GetMapping("/{id}")
    public RevenueRecognitionLineResponse findById(@PathVariable Long id) {
        return RevenueRecognitionLineResponse.from(findUseCase.findById(id));
    }

    /** El libro de una clinica. Aqui el {@code companyId} filtra de verdad. */
    @GetMapping
    public PageResponse<RevenueRecognitionLineResponse> listByCompany(@RequestParam Long companyId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return PageResponse.from(listUseCase.listByCompany(companyId, page, pageSize),
                RevenueRecognitionLineResponse::from);
    }

    /**
     * <strong>El barrido del cierre mensual</strong>: todo lo registrado en un
     * periodo contable, de todas las clinicas. Es la consulta a la que sirve
     * {@code ix_rrl_period}, que no lleva la empresa delante a proposito.
     */
    @GetMapping("/by-posting-period/{postingPeriod}")
    public PageResponse<RevenueRecognitionLineResponse> listByPostingPeriod(
            @PathVariable String postingPeriod, @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return PageResponse.from(listUseCase.listByPostingPeriod(postingPeriod, page, pageSize),
                RevenueRecognitionLineResponse::from);
    }
}
