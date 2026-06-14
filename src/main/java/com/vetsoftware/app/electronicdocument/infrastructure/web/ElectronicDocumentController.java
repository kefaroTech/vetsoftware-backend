package com.vetsoftware.app.electronicdocument.infrastructure.web;

import com.vetsoftware.app.auth.infrastructure.security.Authz;
import com.vetsoftware.app.electronicdocument.application.command.BuildElectronicDocumentCommand;
import com.vetsoftware.app.electronicdocument.application.command.TransmitElectronicDocumentCommand;
import com.vetsoftware.app.electronicdocument.application.dto.ElectronicDocumentDto;
import com.vetsoftware.app.electronicdocument.application.port.in.BuildElectronicDocumentFromAccountUseCase;
import com.vetsoftware.app.electronicdocument.application.port.in.FindElectronicDocumentUseCase;
import com.vetsoftware.app.electronicdocument.application.port.in.ListElectronicDocumentsUseCase;
import com.vetsoftware.app.electronicdocument.application.port.in.TransmitElectronicDocumentUseCase;
import com.vetsoftware.app.electronicdocument.infrastructure.web.request.BuildElectronicDocumentRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/**
 * F2 - lectura del documento electronico + construccion PROVISIONAL desde una cuenta cerrada.
 * Sin update/delete (inmutabilidad fiscal). El POST de construccion es el punto de entrada temporal
 * para probar el modelo; F4 lo reemplazara por la emision automatica al cerrar la cuenta.
 */
@RestController
@RequestMapping("/electronic-documents")
public class ElectronicDocumentController {
    private final BuildElectronicDocumentFromAccountUseCase buildUseCase;
    private final TransmitElectronicDocumentUseCase transmitUseCase;
    private final FindElectronicDocumentUseCase findUseCase;
    private final ListElectronicDocumentsUseCase listUseCase;
    private final Authz authz;

    public ElectronicDocumentController(BuildElectronicDocumentFromAccountUseCase buildUseCase,
                                        TransmitElectronicDocumentUseCase transmitUseCase,
                                        FindElectronicDocumentUseCase findUseCase,
                                        ListElectronicDocumentsUseCase listUseCase,
                                        Authz authz) {
        this.buildUseCase = buildUseCase;
        this.transmitUseCase = transmitUseCase;
        this.findUseCase = findUseCase;
        this.listUseCase = listUseCase;
        this.authz = authz;
    }

    @PostMapping("/from-account")
    @ResponseStatus(HttpStatus.CREATED)
    public ElectronicDocumentDto buildFromAccount(@Valid @RequestBody BuildElectronicDocumentRequest request) {
        return buildUseCase.execute(new BuildElectronicDocumentCommand(
                request.openAccountId(), request.documentType(), authz.currentCompanyId()));
    }

    @PostMapping("/{id}/transmit")
    public ElectronicDocumentDto transmit(@PathVariable Long id) {
        return transmitUseCase.execute(
                new TransmitElectronicDocumentCommand(id, authz.currentCompanyId()));
    }

    @GetMapping
    public List<ElectronicDocumentDto> listAll() {
        return listUseCase.listByCompany(authz.currentCompanyId());
    }

    @GetMapping("/{id}")
    public ElectronicDocumentDto findById(@PathVariable Long id) {
        return findUseCase.findById(id);
    }
}
