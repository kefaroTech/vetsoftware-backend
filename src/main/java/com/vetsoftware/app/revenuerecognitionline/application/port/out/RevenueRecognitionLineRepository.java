package com.vetsoftware.app.revenuerecognitionline.application.port.out;

import com.vetsoftware.app.revenuerecognitionline.domain.RevenueRecognitionLine;
import com.vetsoftware.app.shared.pagination.PageResult;
import java.util.Optional;

/**
 * <strong>Este puerto declara siempre la variante acotada por empresa, y por
 * eso las cuatro reglas de BE-COV lo dejan en paz.</strong>
 * {@code revenue_recognition_lines} es la unica tabla del bloque contable con
 * {@code company_id}: sus filas <b>son de alguien</b>, asi que
 * {@code perteneceAUnaEmpresa} devuelve cierto para su entidad y las reglas de
 * aislamiento se activan sobre la feature entera.
 *
 * <p>
 * {@link #findAllByPostingPeriod(String, int, int)} es la excepcion deliberada:
 * es el <b>barrido de plataforma</b> —el cierre mensual de todas las clinicas—
 * al que sirve {@code ix_rrl_period}, que tampoco lleva la empresa delante a
 * proposito. Su caso de uso va cerrado a {@code hasRole('SYSTEM')} a secas, que
 * es lo unico que {@code LISTADOS_SIN_EMPRESA_SOLO_SYSTEM} admite.
 *
 * <p>
 * <strong>Sin {@code delete} y sin {@code update}.</strong> El libro solo se
 * agrega: un reconocimiento mal calculado se compensa con otra fila de signo
 * contrario. Un {@code update} aqui reescribiria en silencio el ingreso de un
 * periodo ya declarado.
 */
public interface RevenueRecognitionLineRepository {

    RevenueRecognitionLine save(RevenueRecognitionLine line);

    Optional<RevenueRecognitionLine> findById(Long id);

    /**
     * La variante acotada. Es la que usa todo camino que sepa de que clinica habla,
     * y la que {@code CARGA_POR_ID_ACOTADA_POR_EMPRESA} exige que exista y se
     * llame.
     */
    Optional<RevenueRecognitionLine> findByIdAndCompanyId(Long id, Long companyId);

    /**
     * El libro de una clinica, ordenado por periodo. Sirve a
     * {@code ix_rrl_posting}.
     */
    PageResult<RevenueRecognitionLine> findAllByCompanyId(Long companyId, int page, int pageSize);

    /**
     * <strong>Barrido de plataforma</strong>: todo lo registrado en un periodo
     * contable, de todas las clinicas. Es la consulta del cierre mensual y la razon
     * de ser de {@code ix_rrl_period}, que no lleva la empresa delante porque
     * ponersela lo haria inutil para este caso.
     */
    PageResult<RevenueRecognitionLine> findAllByPostingPeriod(String postingPeriod, int page,
            int pageSize);
}
