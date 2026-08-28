package com.vetsoftware.app.revenuerecognitionline.infrastructure.persistence;

import com.vetsoftware.app.revenuerecognitionline.application.port.out.RevenueRecognitionLineRepository;
import com.vetsoftware.app.revenuerecognitionline.domain.RevenueRecognitionLine;
import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.shared.pagination.Pages;
import java.util.Optional;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

@Repository
public class JpaRevenueRecognitionLineRepository implements RevenueRecognitionLineRepository {

    private final RevenueRecognitionLineJpaRepository jpaRepository;
    private final RevenueRecognitionLineJpaMapper mapper;

    public JpaRevenueRecognitionLineRepository(RevenueRecognitionLineJpaRepository jpaRepository,
            RevenueRecognitionLineJpaMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    /**
     * <strong>{@code saveAndFlush} y no {@code save}, y aqui el flush es lo que
     * hace util a la llave antiduplicados.</strong> {@code uq_rrl_recognition} es
     * lo que atrapa el reintento del proceso nocturno —el mismo cargo, el mismo mes
     * y el mismo periodo contable—; sin flush esa violacion saldria al cerrar la
     * transaccion, fuera del caso de uso, y el proceso creeria haber escrito.
     * Ademas el disparador {@code trg_rrl_bi_period_open} solo dispara en el
     * {@code INSERT} real: sin flush, «has escrito en un periodo cerrado» tampoco
     * llegaria a tiempo.
     */
    @Override
    public RevenueRecognitionLine save(RevenueRecognitionLine line) {
        return mapper.toDomain(jpaRepository.saveAndFlush(mapper.toJpa(line)));
    }

    @Override
    public Optional<RevenueRecognitionLine> findById(Long id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<RevenueRecognitionLine> findByIdAndCompanyId(Long id, Long companyId) {
        return jpaRepository.findByIdAndCompanyId(id, companyId).map(mapper::toDomain);
    }

    @Override
    public PageResult<RevenueRecognitionLine> findAllByCompanyId(Long companyId, int page,
            int pageSize) {
        return Pages.result(jpaRepository.findAllByCompanyId(companyId,
                Pages.request(page, pageSize, ledgerOrder())), mapper::toDomain);
    }

    @Override
    public PageResult<RevenueRecognitionLine> findAllByPostingPeriod(String postingPeriod, int page,
            int pageSize) {
        return Pages.result(jpaRepository.findAllByPostingPeriod(postingPeriod,
                Pages.request(page, pageSize, closingOrder())), mapper::toDomain);
    }

    /**
     * El libro de una clinica se lee del periodo mas reciente hacia atras, y dentro
     * de cada periodo por el mes imputado. El {@code id} desempata: dos renglones
     * del mismo cargo, mes y periodo son imposibles por {@code uq_rrl_recognition},
     * pero dos renglones de <em>cargos distintos</em> comparten los tres criterios
     * sin problema, y sin desempate estable dos paginas consecutivas pueden repetir
     * u omitir filas.
     */
    private static Sort ledgerOrder() {
        return Sort.by(Sort.Order.desc("postingPeriod"), Sort.Order.desc("periodKey"),
                Sort.Order.asc("id"));
    }

    /**
     * El barrido del cierre se lee agrupado por clinica: es como se arma el asiento
     * resumen del mes.
     */
    private static Sort closingOrder() {
        return Sort.by(Sort.Order.asc("companyId"), Sort.Order.asc("periodKey"),
                Sort.Order.asc("id"));
    }
}
