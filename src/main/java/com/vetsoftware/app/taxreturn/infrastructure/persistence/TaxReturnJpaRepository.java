package com.vetsoftware.app.taxreturn.infrastructure.persistence;

import java.time.LocalDate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * <strong>Sin una sola {@code @Query}.</strong> Las dos consultas propias las
 * expresa el derivador de nombres de Spring Data, asi que aqui no hay SQL que
 * pueda olvidarse de mover la {@code version} en su {@code SET}
 * ({@code UPDATE_MASIVO_MUEVE_LA_VERSION}) ni proyectar un literal booleano
 * ({@code PROYECCION_SIN_LITERAL_BOOLEANO}). Las cinco escrituras de la feature
 * —crear, editar, presentar, corregir y anular— pasan por el ciclo
 * leer-modificar-guardar, que es el unico camino que {@code @Version} protege.
 *
 * <p>
 * Ningun metodo recibe {@code companyId} porque la tabla no tiene esa columna.
 */
public interface TaxReturnJpaRepository extends JpaRepository<TaxReturnJpaEntity, Long> {

    Page<TaxReturnJpaEntity> findAllByFiscalPeriodKey(String fiscalPeriodKey, Pageable pageable);

    /**
     * Las que quedan en firme antes de esa fecha. Sirve a
     * {@code ix_tax_returns_firmeza} y es la consulta de la que sale la ventana de
     * conservacion de soportes.
     *
     * <p>
     * Las que tienen {@code firmeza_until} nulo —borradores y anuladas— quedan
     * fuera por construccion: en SQL una comparacion contra {@code NULL} no es
     * cierta, y eso es exactamente lo que se quiere. Una declaracion sin presentar
     * no sostiene ninguna ventana.
     */
    Page<TaxReturnJpaEntity> findAllByFirmezaUntilBefore(LocalDate limit, Pageable pageable);
}
