package com.vetsoftware.app.catalogitemaihint.infrastructure.persistence;

import com.vetsoftware.app.catalogitemaihint.application.port.out.CatalogItemAiHintRepository;
import com.vetsoftware.app.catalogitemaihint.domain.CatalogItemAiHint;
import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.shared.pagination.Pages;
import java.util.Optional;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

@Repository
public class JpaCatalogItemAiHintRepository implements CatalogItemAiHintRepository {

    private final CatalogItemAiHintJpaRepository jpaRepository;
    private final CatalogItemAiHintJpaMapper mapper;

    public JpaCatalogItemAiHintRepository(CatalogItemAiHintJpaRepository jpaRepository,
            CatalogItemAiHintJpaMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public CatalogItemAiHint save(CatalogItemAiHint hint) {
        return mapper.toDomain(jpaRepository.save(mapper.toJpa(hint)));
    }

    /**
     * {@code saveAndFlush} y no {@code save}: el UPDATE que cierra la vigencia
     * tiene que llegar a la base antes de que se inserte la revision que la sucede,
     * o {@code uq_catalog_item_ai_hints_current} ve dos vigentes a la vez. Ver el
     * contrato del puerto.
     */
    @Override
    public CatalogItemAiHint supersede(CatalogItemAiHint hint) {
        return mapper.toDomain(jpaRepository.saveAndFlush(mapper.toJpa(hint)));
    }

    @Override
    public Optional<CatalogItemAiHint> findById(Long id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<CatalogItemAiHint> findCurrentByCatalogItemId(Long catalogItemId) {
        return jpaRepository.findByCatalogItemIdAndSupersededAtIsNull(catalogItemId)
                .map(mapper::toDomain);
    }

    @Override
    public Optional<Integer> findLastRevision(Long catalogItemId) {
        return jpaRepository.findLastRevision(catalogItemId);
    }

    /**
     * La huella se calcula aqui, en el adaptador, y no viaja por el puerto: el
     * dominio no guarda {@code hint_hash} porque no es un dato de la pista sino la
     * clave con la que el indice la identifica. {@link CatalogItemAiHint#hashOf}
     * reproduce el {@code SHA2(hint_text, 256)} de la columna generada; que los dos
     * coincidan lo comprueba la rodaja contra MySQL real, que es donde se puede
     * afirmar.
     */
    @Override
    public boolean existsPublishedText(Long catalogItemId, String hintText) {
        return jpaRepository.existsByCatalogItemIdAndHintHash(catalogItemId,
                CatalogItemAiHint.hashOf(hintText));
    }

    /**
     * Orden estable con desempate: sin el {@code id} detras, dos articulos con la
     * misma revision vigente pueden repetirse u omitirse entre paginas.
     */
    @Override
    public PageResult<CatalogItemAiHint> findAllCurrent(int page, int pageSize) {
        Sort orden = Sort.by(Sort.Direction.ASC, "catalogItemId")
                .and(Sort.by(Sort.Direction.ASC, "id"));
        return Pages.result(
                jpaRepository.findBySupersededAtIsNull(Pages.request(page, pageSize, orden)),
                mapper::toDomain);
    }

    @Override
    public PageResult<CatalogItemAiHint> findAllByCatalogItemId(Long catalogItemId, int page,
            int pageSize) {
        Sort orden = Sort.by(Sort.Direction.DESC, "hintRevision")
                .and(Sort.by(Sort.Direction.DESC, "id"));
        return Pages.result(jpaRepository.findByCatalogItemId(catalogItemId,
                Pages.request(page, pageSize, orden)), mapper::toDomain);
    }
}
