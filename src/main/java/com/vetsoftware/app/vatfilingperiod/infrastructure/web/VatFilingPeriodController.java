package com.vetsoftware.app.vatfilingperiod.infrastructure.web;

import com.vetsoftware.app.auth.infrastructure.security.Authz;
import com.vetsoftware.app.infrastructure.web.PageResponse;
import com.vetsoftware.app.vatfilingperiod.application.command.CreateVatFilingPeriodCommand;
import com.vetsoftware.app.vatfilingperiod.application.port.in.CreateVatFilingPeriodUseCase;
import com.vetsoftware.app.vatfilingperiod.application.port.in.FindVatFilingPeriodForYearUseCase;
import com.vetsoftware.app.vatfilingperiod.application.port.in.ListVatFilingPeriodsUseCase;
import com.vetsoftware.app.vatfilingperiod.infrastructure.web.request.CreateVatFilingPeriodRequest;
import com.vetsoftware.app.vatfilingperiod.infrastructure.web.response.VatFilingPeriodResponse;
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

/** La periodicidad de IVA por ano. El ano es obligatorio en la lectura. */
@RestController
@RequestMapping("/vat-filing-periods")
public class VatFilingPeriodController {

    private final CreateVatFilingPeriodUseCase createUseCase;
    private final FindVatFilingPeriodForYearUseCase findUseCase;
    private final ListVatFilingPeriodsUseCase listUseCase;
    private final Authz authz;

    public VatFilingPeriodController(CreateVatFilingPeriodUseCase createUseCase,
            FindVatFilingPeriodForYearUseCase findUseCase, ListVatFilingPeriodsUseCase listUseCase,
            Authz authz) {
        this.createUseCase = createUseCase;
        this.findUseCase = findUseCase;
        this.listUseCase = listUseCase;
        this.authz = authz;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public VatFilingPeriodResponse create(
            @Valid @RequestBody CreateVatFilingPeriodRequest request) {
        return VatFilingPeriodResponse
                .from(createUseCase.execute(new CreateVatFilingPeriodCommand(request.fiscalYear(),
                        request.frequency(), request.legalReference())));
    }

    @GetMapping("/years/{year}")
    public VatFilingPeriodResponse findByYear(@PathVariable int year) {
        return VatFilingPeriodResponse
                .from(findUseCase.findByYear(year, authz.currentCompanyIdOrNull()));
    }

    @GetMapping
    public PageResponse<VatFilingPeriodResponse> listAll(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return PageResponse.from(
                listUseCase.listAll(authz.currentCompanyIdOrNull(), page, pageSize),
                VatFilingPeriodResponse::from);
    }
}
