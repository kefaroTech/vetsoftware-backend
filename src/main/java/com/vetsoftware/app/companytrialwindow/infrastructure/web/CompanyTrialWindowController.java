package com.vetsoftware.app.companytrialwindow.infrastructure.web;

import com.vetsoftware.app.auth.infrastructure.security.Authz;
import com.vetsoftware.app.companytrialwindow.application.port.in.FindCurrentTrialWindowUseCase;
import com.vetsoftware.app.companytrialwindow.infrastructure.web.response.CompanyTrialWindowResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Lo que la clínica ve de su propio reloj de prueba: <strong>solo
 * lectura</strong>.
 *
 * <p>
 * La ficha de construcción reparte el bloque <em>Prueba gratuita</em> como
 * mixto —escribe plataforma, <strong>leen ambos</strong>— y esta es la mitad
 * del cliente. Sin ella, «hasta cuándo dura mi prueba» es una pregunta que solo
 * sabe responder el comercial, y el aviso de vencimiento que el producto
 * necesita pintar no tiene de dónde salir.
 *
 * <p>
 * <strong>La empresa la pone el backend</strong> con
 * {@code authz.currentCompanyId()}: no viaja en ninguna ruta ni en ningún
 * cuerpo de este controller. El puerto la revalida con
 * {@code @authz.isMyCompany(#companyId)}, que es la defensa en profundidad
 * contra un caller futuro que pase otra.
 *
 * <p>
 * <strong>Abrir y cerrar no viven aquí, y no es una funcionalidad
 * pendiente.</strong> Conceder días de prueba es una decisión comercial de
 * plataforma; si el gate admitiera al empleado de la clínica, la administradora
 * se abriría ventanas y el abuso que toda la capa existe para cerrar entraría
 * por la puerta principal. Están en {@link SystemCompanyTrialWindowController}.
 */
@RestController
@RequestMapping("/company-trial-windows")
public class CompanyTrialWindowController {

    private final FindCurrentTrialWindowUseCase findUseCase;
    private final Authz authz;

    public CompanyTrialWindowController(FindCurrentTrialWindowUseCase findUseCase, Authz authz) {
        this.findUseCase = findUseCase;
        this.authz = authz;
    }

    /**
     * La ventana viva de mi clínica. Un 404 aquí es una respuesta correcta y
     * frecuente: significa que la empresa no tiene ninguna abierta, no que algo
     * haya fallado.
     */
    @GetMapping("/current")
    public CompanyTrialWindowResponse current() {
        return CompanyTrialWindowResponse
                .from(findUseCase.findOpenByCompanyId(authz.currentCompanyId()));
    }
}
