package com.vetsoftware.app.configurator.application.port.in;

import com.vetsoftware.app.configurator.application.command.ReorderConfiguratorEffectsCommand;
import com.vetsoftware.app.configurator.application.dto.ConfiguratorEffectDto;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Cambia el orden en que se aplican los efectos del configurador.
 *
 * <p>
 * <strong>Lo que desbloquea.</strong> Hasta hoy la columna {@code priority}
 * existía en el esquema y no había forma de escribirla: corregir el orden
 * obligaba a borrar el efecto y volver a crearlo, lo que le cambia el
 * {@code id} —y con él el desempate— y reordena de paso todo lo demás. Es
 * decir, la única herramienta disponible para arreglar el orden era la que
 * volvía a romperlo.
 *
 * <p>
 * <strong>{@code hasRole('SYSTEM')} a secas, como los otros catorce puertos de
 * la feature.</strong> El configurador es un catálogo global: sus tres tablas
 * no tienen {@code company_id} y el cuestionario que ve un prospecto es el
 * mismo para todos. Abrirlo por una {@code hasAuthority} suelta sería un
 * endpoint que se activa sembrando un permiso, sobre un catálogo que comparten
 * todos los tenants — y es justo lo que rompe la regla dura
 * {@code GATE_COHERENTE_EN_FEATURE_DE_SYSTEM}.
 */
public interface ReorderConfiguratorEffectsUseCase {

    /**
     * Aplica el reparto y devuelve los efectos afectados <strong>ya en el orden
     * nuevo</strong>, para que la pantalla que reordena pueda repintar con lo que
     * de verdad quedó guardado en vez de con lo que creía haber mandado.
     */
    @PreAuthorize("hasRole('SYSTEM')")
    List<ConfiguratorEffectDto> execute(ReorderConfiguratorEffectsCommand command);
}
