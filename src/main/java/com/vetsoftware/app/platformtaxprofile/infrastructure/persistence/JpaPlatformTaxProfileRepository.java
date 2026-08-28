package com.vetsoftware.app.platformtaxprofile.infrastructure.persistence;

import com.vetsoftware.app.economicactivity.infrastructure.persistence.EconomicActivityJpaEntity;
import com.vetsoftware.app.economicactivity.infrastructure.persistence.EconomicActivityJpaRepository;
import com.vetsoftware.app.platformtaxprofile.application.port.out.PlatformTaxProfileRepository;
import com.vetsoftware.app.platformtaxprofile.domain.PlatformTaxProfile;
import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.shared.pagination.Pages;
import java.util.Optional;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

@Repository
public class JpaPlatformTaxProfileRepository implements PlatformTaxProfileRepository {

    private final PlatformTaxProfileJpaRepository jpaRepository;
    private final EconomicActivityJpaRepository economicActivityJpaRepository;
    private final PlatformTaxProfileJpaMapper mapper;

    public JpaPlatformTaxProfileRepository(PlatformTaxProfileJpaRepository jpaRepository,
            EconomicActivityJpaRepository economicActivityJpaRepository,
            PlatformTaxProfileJpaMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.economicActivityJpaRepository = economicActivityJpaRepository;
        this.mapper = mapper;
    }

    /**
     * <strong>{@code saveAndFlush} y no {@code save}: es el contrato del puerto, no
     * una preferencia.</strong>
     *
     * <p>
     * La sucesion cierra la identidad vigente y abre la siguiente en la misma
     * transaccion. Hibernate no manda esas dos sentencias en el orden en que se
     * llamo aqui: su cola de acciones ejecuta <em>todos</em> los {@code INSERT}
     * antes que los {@code UPDATE}. Con un {@code save} normal, la sucesora
     * entraria mientras la anterior sigue con {@code valid_to} nulo, las dos
     * calcularian el mismo {@code current_profile_marker} —la constante {@code 1}—
     * y {@code uq_platform_tax_profiles_current} pararia la operacion con un
     * {@code Duplicate entry} sobre una columna generada que nadie escribio.
     *
     * <p>
     * El caso de uso se lee perfecto —cierra primero, abre despues— y el defecto
     * estaria debajo, en el orden que decide el framework. Por eso el flush va
     * escrito en el puerto y se cumple aqui.
     *
     * <p>
     * <strong>{@code getReferenceById} y no {@code findById}</strong>: devuelve un
     * proxy sin {@code SELECT}, porque la actividad economica ya la valido el caso
     * de uso a traves de {@code EconomicActivityQueryPort}. Y el {@code toDomain}
     * de vuelta reusa el ref que traia la ficha, para no disparar la hidratacion de
     * ese mismo proxy.
     *
     * <p>
     * La actividad es <strong>opcional</strong>: sin ella no se pide ninguna
     * referencia y la columna queda nula, que es lo que 367 permite.
     */
    @Override
    public PlatformTaxProfile save(PlatformTaxProfile profile) {
        EconomicActivityJpaEntity economicActivity = profile.getEconomicActivity() == null
                ? null
                : economicActivityJpaRepository
                        .getReferenceById(profile.getEconomicActivity().id());
        PlatformTaxProfileJpaEntity saved = jpaRepository
                .saveAndFlush(mapper.toJpa(profile, economicActivity));
        return mapper.toDomain(saved, profile.getEconomicActivity());
    }

    @Override
    public Optional<PlatformTaxProfile> findById(Long id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<PlatformTaxProfile> findCurrent() {
        return jpaRepository.findCurrent().map(mapper::toDomain);
    }

    @Override
    public PageResult<PlatformTaxProfile> findAll(int page, int pageSize) {
        return Pages.result(jpaRepository.findAll(Pages.request(page, pageSize, masReciente())),
                mapper::toDomain);
    }

    /**
     * Historico: la vigente primero y hacia atras, que es como se lee una linea de
     * tiempo. Desempate por {@code id} descendente para que el orden sea total —
     * {@code uq_platform_tax_profiles_validity} impide dos identidades con la misma
     * {@code valid_from}, asi que hoy el empate no puede darse, pero un orden que
     * depende de una restriccion es un orden que se rompe el dia que esa
     * restriccion cambie, y sin desempate dos paginas consecutivas repiten u omiten
     * filas.
     */
    private static Sort masReciente() {
        return Sort.by(Sort.Direction.DESC, "validFrom").and(Sort.by(Sort.Direction.DESC, "id"));
    }
}
