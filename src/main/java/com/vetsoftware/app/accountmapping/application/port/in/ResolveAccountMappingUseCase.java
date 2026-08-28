package com.vetsoftware.app.accountmapping.application.port.in;

import com.vetsoftware.app.accountmapping.application.dto.AccountMappingDto;
import com.vetsoftware.app.accountmapping.domain.MappingKind;
import java.time.LocalDate;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ResolveAccountMappingUseCase {

    /**
     * <strong>LA consulta del negocio</strong>: que cuentas mueve este supuesto en
     * esta fecha. De ella sale el asiento entero, y por ella existe toda la
     * feature.
     *
     * <p>
     * Devuelve el mapeo con {@code validFrom <= on} y {@code validTo} nulo o
     * posterior a {@code on}. <b>El limite superior es estricto</b>, de modo que el
     * mapeo que se cierra y el que lo releva ese mismo dia no se pisan.
     *
     * <p>
     * <strong>Si no hay mapeo, lanza en vez de devolver vacio.</strong> El fallo
     * caro de un puente concepto → cuenta no es un error: es un asiento que no se
     * genera. Ver {@code NoEffectiveAccountMappingException}.
     *
     * @param on
     *            la fecha del hecho economico, no la de hoy. Recalcular una factura
     *            de diciembre con el mapeo de enero es como se descuadra un periodo
     *            ya declarado. <b>Admite {@code null}</b>, y entonces el dia lo
     *            pone la implementacion con su {@code Clock} inyectado: un
     *            {@code LocalDate.now()} en la capa web es una fecha que ningun
     *            test puede fijar y {@code RELOJ_INYECTADO_EN_VEZ_DE_NOW} rompe el
     *            build por ello
     */
    @PreAuthorize("hasRole('SYSTEM')")
    AccountMappingDto resolve(MappingKind mappingKind, String mappingKey, Long catalogItemId,
            String chargeType, String taxTreatment, LocalDate on);
}
