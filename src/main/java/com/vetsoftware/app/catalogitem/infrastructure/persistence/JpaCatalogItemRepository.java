package com.vetsoftware.app.catalogitem.infrastructure.persistence;

import com.vetsoftware.app.catalogitem.application.port.out.CatalogItemRepository;
import com.vetsoftware.app.catalogitem.domain.CatalogItem;
import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.shared.pagination.Pages;
import java.util.Optional;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

@Repository
public class JpaCatalogItemRepository implements CatalogItemRepository {

    private final CatalogItemJpaRepository jpaRepository;
    private final CatalogItemJpaMapper mapper;

    public JpaCatalogItemRepository(CatalogItemJpaRepository jpaRepository,
            CatalogItemJpaMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public CatalogItem save(CatalogItem item) {
        return mapper.toDomain(jpaRepository.save(mapper.toJpa(item)));
    }

    @Override
    public Optional<CatalogItem> findById(Long id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    /**
     * El orden es {@code sort_order} y luego {@code id}, y el desempate por id no
     * es decorativo: {@code sort_order} admite repetidos —su valor por defecto es
     * {@code 0} para todos— y sin un orden total dos páginas consecutivas repiten u
     * omiten filas.
     *
     * <p>
     * El tope de tamaño lo pone {@code Pages.request} y ningún otro sitio
     * ({@code PAGINA_ACOTADA_EN_UN_SOLO_SITIO}).
     */
    @Override
    public PageResult<CatalogItem> findAll(int page, int pageSize) {
        Sort order = Sort.by(Sort.Direction.ASC, "sortOrder")
                .and(Sort.by(Sort.Direction.ASC, "id"));
        return Pages.result(jpaRepository.findAll(Pages.request(page, pageSize, order)),
                mapper::toDomain);
    }

    @Override
    public void delete(Long id) {
        jpaRepository.deleteById(id);
    }

    @Override
    public int reactivate(Long id) {
        return jpaRepository.reactivate(id);
    }

    @Override
    public boolean existsByCodeIgnoringEnabled(String code) {
        return jpaRepository.countAnyByCode(code) > 0;
    }
}
