package com.vetsoftware.app.limitdimension.infrastructure.web;

import com.vetsoftware.app.limitdimension.application.command.CreateLimitDimensionCommand;
import com.vetsoftware.app.limitdimension.application.command.UpdateLimitDimensionCommand;
import com.vetsoftware.app.limitdimension.application.port.in.CreateLimitDimensionUseCase;
import com.vetsoftware.app.limitdimension.application.port.in.FindLimitDimensionUseCase;
import com.vetsoftware.app.limitdimension.application.port.in.ListLimitDimensionsUseCase;
import com.vetsoftware.app.limitdimension.application.port.in.UpdateLimitDimensionUseCase;
import com.vetsoftware.app.limitdimension.infrastructure.web.request.CreateLimitDimensionRequest;
import com.vetsoftware.app.limitdimension.infrastructure.web.request.UpdateLimitDimensionRequest;
import com.vetsoftware.app.limitdimension.infrastructure.web.response.LimitDimensionResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * El catálogo de ejes limitables: qué cosas se pueden contar.
 *
 * <p>
 * <strong>Ningún endpoint recibe ni deriva empresa</strong>, ni en la ruta ni
 * en el cuerpo, porque la tabla no la tiene: es catálogo global de plataforma,
 * igual que {@code catalog_items}. La autorización vive entera en el
 * {@code @PreAuthorize} de cada puerto y es {@code hasRole('SYSTEM')} a secas
 * en los tres —lo exige {@code LISTADOS_SIN_EMPRESA_SOLO_SYSTEM} para el
 * listado y la familia «por id» de BE-COV para la consulta por identificador—.
 *
 * <p>
 * Es lo que reparte la «ficha de construcción» del modelo de suscripciones al
 * bloque <em>Catálogo de límites</em>: escribe plataforma, lee plataforma.
 *
 * <p>
 * <strong>Los cuatro endpoints son {@code hasRole('SYSTEM')} a secas</strong>,
 * incluido el {@code PUT}. La edición no es la excepción sino el mismo caso: el
 * {@code id} lo escribe el cliente en la URL y la fila no pertenece a nadie a
 * quien revalidar.
 */
@RestController
@RequestMapping("/limit-dimensions")
public class LimitDimensionController {

    private final CreateLimitDimensionUseCase createUseCase;
    private final UpdateLimitDimensionUseCase updateUseCase;
    private final FindLimitDimensionUseCase findUseCase;
    private final ListLimitDimensionsUseCase listUseCase;

    public LimitDimensionController(CreateLimitDimensionUseCase createUseCase,
            UpdateLimitDimensionUseCase updateUseCase, FindLimitDimensionUseCase findUseCase,
            ListLimitDimensionsUseCase listUseCase) {
        this.createUseCase = createUseCase;
        this.updateUseCase = updateUseCase;
        this.findUseCase = findUseCase;
        this.listUseCase = listUseCase;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public LimitDimensionResponse create(@Valid @RequestBody CreateLimitDimensionRequest request) {
        return LimitDimensionResponse
                .from(createUseCase.execute(new CreateLimitDimensionCommand(request.code(),
                        request.name(), request.measureKind(), request.subModuleId(),
                        request.releaseDelayDays(), request.availableFrom())));
    }

    /**
     * El catálogo entero. <strong>No está paginado</strong>, y no es un olvido: el
     * puerto devuelve {@code List} porque son los ejes que la plataforma sabe
     * contar —decenas, no miles—, y la pantalla que declara un techo los necesita
     * todos a la vez para ofrecerlos como opciones.
     */
    @GetMapping
    public List<LimitDimensionResponse> listAll() {
        return listUseCase.listAll().stream().map(LimitDimensionResponse::from).toList();
    }

    @GetMapping("/{id}")
    public LimitDimensionResponse findById(@PathVariable Long id) {
        return LimitDimensionResponse.from(findUseCase.findById(id));
    }

    /**
     * Cambia lo editable del eje: nombre, submódulo y días de enfriamiento.
     *
     * <p>
     * <strong>El cuerpo no admite ni el código ni el tipo de medida ni la fecha de
     * disponibilidad</strong>, y esa ausencia es la regla: los tres viven copiados
     * o atados aguas abajo —en la línea del contrato, en la clave foránea compuesta
     * de los techos vendidos y en la decisión de D-74— y moverlos aquí no sería una
     * edición sino una migración disfrazada de {@code PUT}.
     */
    @PutMapping("/{id}")
    public LimitDimensionResponse update(@PathVariable Long id,
            @Valid @RequestBody UpdateLimitDimensionRequest request) {
        return LimitDimensionResponse.from(updateUseCase.execute(new UpdateLimitDimensionCommand(id,
                request.name(), request.subModuleId(), request.releaseDelayDays())));
    }
}
