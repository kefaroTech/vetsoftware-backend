package com.vetsoftware.app.smmlvvalue.infrastructure.web;

import com.vetsoftware.app.auth.infrastructure.security.Authz;
import com.vetsoftware.app.infrastructure.web.PageResponse;
import com.vetsoftware.app.smmlvvalue.application.command.ChangeSmmlvStatusCommand;
import com.vetsoftware.app.smmlvvalue.application.command.CreateSmmlvValueCommand;
import com.vetsoftware.app.smmlvvalue.application.port.in.ChangeSmmlvStatusUseCase;
import com.vetsoftware.app.smmlvvalue.application.port.in.CreateSmmlvValueUseCase;
import com.vetsoftware.app.smmlvvalue.application.port.in.FindSmmlvValueForYearUseCase;
import com.vetsoftware.app.smmlvvalue.application.port.in.ListSmmlvValuesUseCase;
import com.vetsoftware.app.smmlvvalue.infrastructure.web.request.ChangeSmmlvStatusRequest;
import com.vetsoftware.app.smmlvvalue.infrastructure.web.request.CreateSmmlvValueRequest;
import com.vetsoftware.app.smmlvvalue.infrastructure.web.response.SmmlvValueResponse;
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
 * El salario minimo por ano y el registro de su estado.
 *
 * <p>
 * El cambio de estado va por <strong>ano</strong> en la ruta y no por id: quien
 * registra un auto judicial conoce el ano al que afecta, no el identificador de
 * la fila.
 */
@RestController
@RequestMapping("/smmlv-values")
public class SmmlvValueController {

    private final CreateSmmlvValueUseCase createUseCase;
    private final ChangeSmmlvStatusUseCase changeStatusUseCase;
    private final FindSmmlvValueForYearUseCase findUseCase;
    private final ListSmmlvValuesUseCase listUseCase;
    private final Authz authz;

    public SmmlvValueController(CreateSmmlvValueUseCase createUseCase,
            ChangeSmmlvStatusUseCase changeStatusUseCase, FindSmmlvValueForYearUseCase findUseCase,
            ListSmmlvValuesUseCase listUseCase, Authz authz) {
        this.createUseCase = createUseCase;
        this.changeStatusUseCase = changeStatusUseCase;
        this.findUseCase = findUseCase;
        this.listUseCase = listUseCase;
        this.authz = authz;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SmmlvValueResponse create(@Valid @RequestBody CreateSmmlvValueRequest request) {
        return SmmlvValueResponse
                .from(createUseCase.execute(new CreateSmmlvValueCommand(request.fiscalYear(),
                        request.valueAmount(), request.legalReference())));
    }

    @PatchMapping("/years/{year}/status")
    public SmmlvValueResponse changeStatus(@PathVariable int year,
            @Valid @RequestBody ChangeSmmlvStatusRequest request) {
        return SmmlvValueResponse
                .from(changeStatusUseCase.execute(new ChangeSmmlvStatusCommand(year,
                        request.status(), request.statusReference(), request.statusChangedOn())));
    }

    @GetMapping("/years/{year}")
    public SmmlvValueResponse findByYear(@PathVariable int year) {
        return SmmlvValueResponse
                .from(findUseCase.findByYear(year, authz.currentCompanyIdOrNull()));
    }

    @GetMapping
    public PageResponse<SmmlvValueResponse> listAll(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return PageResponse.from(
                listUseCase.listAll(authz.currentCompanyIdOrNull(), page, pageSize),
                SmmlvValueResponse::from);
    }
}
