package com.vetsoftware.app.taxreturn.application.port.out;

import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.taxreturn.domain.TaxReturn;
import java.time.LocalDate;
import java.util.Optional;

/**
 * <strong>Ningun metodo recibe {@code companyId}</strong>: {@code tax_returns}
 * no tiene esa columna. Son declaraciones de Lumbre.
 */
public interface TaxReturnRepository {

    TaxReturn save(TaxReturn taxReturn);

    Optional<TaxReturn> findById(Long id);

    /** Todas, paginadas, de la mas reciente hacia atras. */
    PageResult<TaxReturn> findAll(int page, int pageSize);

    /** Las de un periodo fiscal. Sirve a {@code ix_tax_returns_period}. */
    PageResult<TaxReturn> findAllByFiscalPeriodKey(String fiscalPeriodKey, int page, int pageSize);

    /**
     * <strong>Barrido de plataforma</strong>: las declaraciones que quedan en firme
     * antes de una fecha.
     *
     * <p>
     * Es la consulta a la que sirve {@code ix_tax_returns_firmeza}, y no es un
     * informe decorativo: de ella sale <b>hasta cuando hay que conservar los
     * soportes</b>. El detalle de {@code company_usage_events} no se purga mientras
     * exista una declaracion de renta que cubra el año gravable del cargo al que
     * apunta y cuya firmeza no haya pasado (art. 632 ET + art. 46 de la Ley 962 de
     * 2005 + art. 714 ET). Purgar antes destruye la prueba que sostiene el cargo
     * por excedente.
     */
    PageResult<TaxReturn> findAllByFirmezaUntilBefore(LocalDate limit, int page, int pageSize);
}
