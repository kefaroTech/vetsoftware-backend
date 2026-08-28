package com.vetsoftware.app.companylimitevent.application.port.in;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * La trampa verificada que este caso de uso existe para no pisar.
 *
 * <p>
 * La corrección de un contador <strong>no puede colgar de un gate que admita al
 * tenant</strong>: si lo hiciera, la administradora de la clínica recuperaría
 * su propio cupo cada vez que topa y el cupo dejaría de existir, sin que
 * ninguna fila del modelo estuviera mal. Es una regla que no se ve en una
 * revisión —el {@code @PreAuthorize} «se lee bien» en las dos formas—, así que
 * se comprueba aquí y no se confía a la memoria de nadie.
 */
@DisplayName("Autorización de la bitácora de cupo — quién puede corregir un contador")
class LimitEventAuthorizationTest {

    private static String gateDe(Class<?> puerto, String metodo) throws NoSuchMethodException {
        Method encontrado = null;
        for (Method candidato : puerto.getMethods()) {
            if (candidato.getName().equals(metodo))
                encontrado = candidato;
        }
        if (encontrado == null)
            throw new NoSuchMethodException(metodo);
        PreAuthorize gate = encontrado.getAnnotation(PreAuthorize.class);
        return gate == null ? null : gate.value();
    }

    @Test
    @DisplayName("un empleado de la clínica no puede invocar la corrección de consumo: el gate es"
            + " hasRole('SYSTEM') a secas")
    void un_empleado_de_la_clinica_no_puede_invocar_la_correccion_de_consumo() throws Exception {
        String gate = gateDe(AdjustCompanyUsageUseCase.class, "execute");

        assertThat(gate).isEqualTo("hasRole('SYSTEM')");
        assertThat(gate).doesNotContain("isMyCompany");
    }

    @Test
    @DisplayName("escribir el hecho del portazo sí admite al tenant: si no, el registro quedaría"
            + " vacío justo en el caso que importa")
    void escribir_el_hecho_del_portazo_admite_al_tenant() throws Exception {
        String gate = gateDe(RecordLimitEventUseCase.class, "execute");

        assertThat(gate).contains("hasRole('SYSTEM')");
        assertThat(gate).contains("@authz.isMyCompany(#command.companyId)");
    }

    @Test
    @DisplayName("el listado de hechos de una empresa revalida el tenant contra el principal")
    void el_listado_de_hechos_revalida_el_tenant() throws Exception {
        String gate = gateDe(ListCompanyLimitEventsUseCase.class, "listByCompanyId");

        assertThat(gate).contains("@authz.isMyCompany(#companyId)");
    }
}
