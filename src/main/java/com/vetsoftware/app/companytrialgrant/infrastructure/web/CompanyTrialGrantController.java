package com.vetsoftware.app.companytrialgrant.infrastructure.web;

import com.vetsoftware.app.auth.infrastructure.security.Authz;
import com.vetsoftware.app.companytrialgrant.application.port.in.ListCompanyTrialGrantsUseCase;
import com.vetsoftware.app.companytrialgrant.infrastructure.web.response.CompanyTrialGrantResponse;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Lo que la clínica ve de sus propias pruebas: <strong>solo lectura</strong>.
 *
 * <p>
 * La ficha reparte el bloque <em>Prueba gratuita</em> como mixto —escribe
 * plataforma, <strong>leen ambos</strong>—, y esta es la mitad del cliente: qué
 * ha probado ya y hasta cuándo. Sin ella, el aviso de «te quedan tres días de
 * la agenda» no tiene de dónde salir y el vencimiento le llega al cliente como
 * una sorpresa.
 *
 * <p>
 * <strong>La empresa la pone el backend</strong> con
 * {@code authz.currentCompanyId()}; el puerto la revalida con
 * {@code @authz.isMyCompany(#companyId)}. El hermano sin empresa —el barrido de
 * vencimientos— es otro caso de uso y vive en
 * {@link SystemCompanyTrialGrantController}, cerrado a plataforma como exige
 * {@code LISTADOS_SIN_EMPRESA_SOLO_SYSTEM}.
 *
 * <p>
 * <strong>No hay ninguna operación de borrado, aquí ni en el controlador de
 * plataforma</strong> (R-TRIAL-22, R-TRIAL-30). Una concesión no se desconcede:
 * lo más parecido es resolverla con su desenlace, y eso escribe un hecho en vez
 * de quitarlo.
 */
@RestController
@RequestMapping("/company-trial-grants")
public class CompanyTrialGrantController {

    private final ListCompanyTrialGrantsUseCase listUseCase;
    private final Authz authz;

    public CompanyTrialGrantController(ListCompanyTrialGrantsUseCase listUseCase, Authz authz) {
        this.listUseCase = listUseCase;
        this.authz = authz;
    }

    @GetMapping
    public List<CompanyTrialGrantResponse> listMine() {
        return listUseCase.listByCompanyId(authz.currentCompanyId()).stream()
                .map(CompanyTrialGrantResponse::from).toList();
    }
}
