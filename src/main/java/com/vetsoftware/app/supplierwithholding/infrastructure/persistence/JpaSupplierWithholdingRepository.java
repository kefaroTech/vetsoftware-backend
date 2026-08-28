package com.vetsoftware.app.supplierwithholding.infrastructure.persistence;

import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.shared.pagination.Pages;
import com.vetsoftware.app.supplierwithholding.application.port.out.SupplierWithholdingRepository;
import com.vetsoftware.app.supplierwithholding.domain.SupplierWithholding;
import java.util.Optional;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

@Repository
public class JpaSupplierWithholdingRepository implements SupplierWithholdingRepository {

    private final SupplierWithholdingJpaRepository jpaRepository;
    private final SupplierWithholdingJpaMapper mapper;

    public JpaSupplierWithholdingRepository(SupplierWithholdingJpaRepository jpaRepository,
            SupplierWithholdingJpaMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    /**
     * <strong>{@code saveAndFlush} y no {@code save}.</strong> Spring Data no
     * incrementa {@code @Version} hasta el flush —lo que ya mordio en
     * {@code JpaDocumentWithholdingRepository}—, y ademas es lo que hace que la
     * violacion de {@code uq_supplier_withholdings_case} —la misma retencion al
     * mismo proveedor por el mismo soporte— salga dentro del caso de uso y no al
     * cerrar la transaccion.
     */
    @Override
    public SupplierWithholding save(SupplierWithholding withholding) {
        return mapper.toDomain(jpaRepository.saveAndFlush(mapper.toJpa(withholding)));
    }

    @Override
    public Optional<SupplierWithholding> findById(Long id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public PageResult<SupplierWithholding> findAll(int page, int pageSize) {
        return Pages.result(jpaRepository.findAll(Pages.request(page, pageSize, ledgerOrder())),
                mapper::toDomain);
    }

    @Override
    public PageResult<SupplierWithholding> findAllByFiscalPeriodKey(String fiscalPeriodKey,
            int page, int pageSize) {
        return Pages.result(jpaRepository.findAllByFiscalPeriodKey(fiscalPeriodKey,
                Pages.request(page, pageSize, declarationOrder())), mapper::toDomain);
    }

    @Override
    public PageResult<SupplierWithholding> findAllBySupplierTaxIdAndFiscalYear(String supplierTaxId,
            int fiscalYear, int page, int pageSize) {
        return Pages.result(jpaRepository.findAllBySupplierTaxIdAndFiscalYear(supplierTaxId,
                (short) fiscalYear, Pages.request(page, pageSize, certificateOrder())),
                mapper::toDomain);
    }

    /**
     * El archivo se lee de lo mas reciente hacia atras. El {@code id} desempata:
     * varias retenciones del mismo dia son el caso normal, y sin un criterio
     * estable dos paginas consecutivas pueden repetir u omitir filas.
     */
    private static Sort ledgerOrder() {
        return Sort.by(Sort.Order.desc("practicedOn"), Sort.Order.desc("id"));
    }

    /**
     * La declaracion del mes se arma por tipo de retencion y, dentro de cada uno,
     * por proveedor: es el orden en que se totaliza el formulario.
     */
    private static Sort declarationOrder() {
        return Sort.by(Sort.Order.asc("withholdingType"), Sort.Order.asc("supplierTaxId"),
                Sort.Order.asc("practicedOn"), Sort.Order.asc("id"));
    }

    /**
     * El certificado anual se lee en orden cronologico: es como se listan los
     * soportes que lo componen.
     */
    private static Sort certificateOrder() {
        return Sort.by(Sort.Order.asc("practicedOn"), Sort.Order.asc("id"));
    }
}
