package com.vetsoftware.app.uvtvalue.infrastructure.web;

import com.vetsoftware.app.auth.infrastructure.security.Authz;
import com.vetsoftware.app.infrastructure.web.PageResponse;
import com.vetsoftware.app.uvtvalue.application.command.CreateUvtValueCommand;
import com.vetsoftware.app.uvtvalue.application.port.in.CreateUvtValueUseCase;
import com.vetsoftware.app.uvtvalue.application.port.in.FindUvtValueForYearUseCase;
import com.vetsoftware.app.uvtvalue.application.port.in.ListUvtValuesUseCase;
import com.vetsoftware.app.uvtvalue.infrastructure.web.request.CreateUvtValueRequest;
import com.vetsoftware.app.uvtvalue.infrastructure.web.response.UvtValueResponse;
import jakarta.validation.Valid;
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
 * La UVT por ano. <strong>No hay endpoint de «la UVT vigente»</strong>: la ruta
 * de lectura es {@code /uvt-values/years/{year}} con el ano obligatorio, que es
 * la forma de que nadie liquide un ano viejo con la cifra de este.
 *
 * <p>
 * La empresa la pone el controller con {@code currentCompanyIdOrNull()} —para
 * un principal de plataforma no hay empresa y {@code currentCompanyId()}
 * lanzaria antes de que el gate pudiera aplicar su rama SYSTEM— y el puerto la
 * revalida.
 */
@RestController
@RequestMapping("/uvt-values")
public class UvtValueController {

    private final CreateUvtValueUseCase createUseCase;
    private final FindUvtValueForYearUseCase findUseCase;
    private final ListUvtValuesUseCase listUseCase;
    private final Authz authz;

    public UvtValueController(CreateUvtValueUseCase createUseCase,
            FindUvtValueForYearUseCase findUseCase, ListUvtValuesUseCase listUseCase, Authz authz) {
        this.createUseCase = createUseCase;
        this.findUseCase = findUseCase;
        this.listUseCase = listUseCase;
        this.authz = authz;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UvtValueResponse create(@Valid @RequestBody CreateUvtValueRequest request) {
        return UvtValueResponse
                .from(createUseCase.execute(new CreateUvtValueCommand(request.fiscalYear(),
                        request.valueAmount(), request.legalReference())));
    }

    @GetMapping("/years/{year}")
    public UvtValueResponse findByYear(@PathVariable int year) {
        return UvtValueResponse.from(findUseCase.findByYear(year, authz.currentCompanyIdOrNull()));
    }

    @GetMapping
    public PageResponse<UvtValueResponse> listAll(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return PageResponse.from(
                listUseCase.listAll(authz.currentCompanyIdOrNull(), page, pageSize),
                UvtValueResponse::from);
    }
}
