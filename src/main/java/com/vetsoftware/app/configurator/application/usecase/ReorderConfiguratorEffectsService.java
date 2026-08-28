package com.vetsoftware.app.configurator.application.usecase;

import com.vetsoftware.app.configurator.application.command.ReorderConfiguratorEffectsCommand;
import com.vetsoftware.app.configurator.application.command.ReorderConfiguratorEffectsCommand.EffectPriority;
import com.vetsoftware.app.configurator.application.dto.ConfiguratorEffectDto;
import com.vetsoftware.app.configurator.application.port.in.ReorderConfiguratorEffectsUseCase;
import com.vetsoftware.app.configurator.application.port.out.ConfiguratorEffectRepository;
import com.vetsoftware.app.configurator.domain.ConfiguratorEffect;
import com.vetsoftware.app.configurator.domain.ConfiguratorEffectNotFoundException;
import io.micrometer.observation.annotation.Observed;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reparte prioridades sobre varios efectos de una vez.
 *
 * <p>
 * <strong>Todo o nada, y por eso es {@code @Transactional}.</strong> Un reparto
 * a medias es peor que no haber reordenado: deja el {@code REMOVE} movido y el
 * {@code ADD} en su sitio viejo, que es la combinación que produce el carrito
 * equivocado. Si un id no existe, no se mueve ninguno.
 *
 * <p>
 * <strong>Se escribe por el ciclo leer-modificar-guardar, no por un
 * {@code UPDATE} masivo</strong>, y no es pereza: {@code configurator_effects}
 * va versionada, y una {@code @Query} de {@code UPDATE} no pasa por el chequeo
 * de {@code @Version} —ni lo comprueba ni lo incrementa—, así que un
 * {@code save} concurrente que venga de una lectura anterior pisaría el reparto
 * sin excepción, sin log y sin 409. Es exactamente el defecto que persigue
 * {@code UPDATE_MASIVO_MUEVE_LA_VERSION} (#53). Aquí cada efecto se lee, se
 * mueve y se guarda, que es el único camino que {@code @Version} protege.
 *
 * <p>
 * <strong>La carga es una sola consulta</strong> ({@code findAllByIds}) y no un
 * {@code findById} por efecto: además del coste, cargarlos uno a uno dejaría el
 * conjunto sin una foto coherente.
 */
@Observed(name = "configurator.effect.reorder")
@Service
public class ReorderConfiguratorEffectsService implements ReorderConfiguratorEffectsUseCase {

    private final ConfiguratorEffectRepository repository;

    public ReorderConfiguratorEffectsService(ConfiguratorEffectRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public List<ConfiguratorEffectDto> execute(ReorderConfiguratorEffectsCommand command) {
        List<EffectPriority> reparto = command.priorities();
        List<Long> ids = reparto.stream().map(EffectPriority::effectId).toList();
        assertSinRepetidos(ids);

        Map<Long, ConfiguratorEffect> porId = repository.findAllByIds(ids).stream()
                .collect(Collectors.toMap(ConfiguratorEffect::getId, Function.identity()));
        assertTodosExisten(ids, porId);

        return reparto.stream().map(par -> mover(porId.get(par.effectId()), par.priority()))
                .sorted(Comparator.comparingInt(ConfiguratorEffectDto::priority)
                        .thenComparing(ConfiguratorEffectDto::id))
                .toList();
    }

    /** El rango 0..9999 lo comprueba la entidad; aquí solo se mueve y se guarda. */
    private ConfiguratorEffectDto mover(ConfiguratorEffect effect, int priority) {
        effect.reprioritize(priority);
        return ConfiguratorEffectDto.from(repository.save(effect));
    }

    /**
     * Dos entradas para el mismo efecto no son un reparto: son dos órdenes
     * contradictorias, y la que ganaría sería la última del array — es decir, el
     * resultado dependería de en qué orden serializó el JSON quien lo mandó.
     * Rechazarlo es lo que convierte un reordenado silenciosamente equivocado en un
     * 400 que nombra el efecto duplicado.
     */
    private static void assertSinRepetidos(List<Long> ids) {
        Set<Long> vistos = new HashSet<>();
        List<Long> repetidos = ids.stream().filter(id -> !vistos.add(id)).distinct().toList();
        if (!repetidos.isEmpty())
            throw new IllegalArgumentException(
                    "Duplicated effect in reorder: " + repetidos.getFirst());
    }

    /**
     * Un id que no vuelve de la consulta o no existe o está dado de baja —
     * {@code findAllByIds} respeta el borrado lógico—. Las dos cosas son un 404
     * para quien reordena, y las dos tienen que parar el reparto entero antes de
     * escribir nada.
     */
    private static void assertTodosExisten(List<Long> ids, Map<Long, ConfiguratorEffect> porId) {
        ids.stream().filter(id -> !porId.containsKey(id)).findFirst().ifPresent(ausente -> {
            throw new ConfiguratorEffectNotFoundException(ausente);
        });
    }
}
