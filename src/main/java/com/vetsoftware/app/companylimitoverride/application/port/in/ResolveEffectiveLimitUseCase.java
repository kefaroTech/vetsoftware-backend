package com.vetsoftware.app.companylimitoverride.application.port.in;

import com.vetsoftware.app.companylimitoverride.application.dto.EffectiveLimitDto;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * <strong>Qué techo rige de verdad sobre un eje, y de dónde sale.</strong>
 *
 * <p>
 * Es la mitad que faltaba del bloque. Hasta aquí el cliente podía leer sus
 * excepciones negociadas por un lado y sus techos congelados por otro, pero
 * nada le decía cuál de los dos manda: leer 300 en una pantalla y 200 en la de
 * al lado no es informar, es obligar al usuario a adivinar. Este puerto aplica
 * la precedencia
 * {@code COMPANY_OVERRIDE > SUBSCRIPTION > CATALOG_DEFAULT > NONE} en un solo
 * sitio y devuelve el número <em>con su procedencia dentro</em>, que es lo que
 * la pantalla de cupos necesita para pintar la línea que explica el número.
 *
 * <p>
 * <strong>Autorización mixta: plataforma o la propia empresa.</strong> Es lo
 * que reparte la ficha de construcción al bloque <em>Techo del contrato</em>
 * —escribe plataforma, leen ambos— y su frase es literalmente «el cliente lee
 * su techo y de dónde sale». Recibe el {@code companyId} y lo revalida contra
 * el principal, que es lo que impide leer el techo de la clínica vecina
 * escribiendo otro número.
 *
 * <p>
 * <strong>Resuelve, nunca escribe.</strong> El resultado no se guarda: quien
 * guarda el techo ya resuelto junto al contador es el recálculo de permisos, y
 * duplicar aquí esa escritura daría dos caminos que pueden divergir sobre la
 * misma fila.
 */
public interface ResolveEffectiveLimitUseCase {

    @PreAuthorize("hasRole('SYSTEM') or (hasAuthority('companyLimitOverride.read')"
            + " and @authz.isMyCompany(#companyId))")
    EffectiveLimitDto resolve(Long companyId, Long limitDimensionId);
}
