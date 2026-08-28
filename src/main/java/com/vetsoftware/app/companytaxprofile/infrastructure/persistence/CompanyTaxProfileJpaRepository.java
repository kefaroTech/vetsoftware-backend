package com.vetsoftware.app.companytaxprofile.infrastructure.persistence;

import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

/**
 * <strong>Desde el changeset 364 la tabla guarda histórico</strong>, así que
 * una consulta por empresa sin filtro de vigencia devuelve también las fichas
 * cerradas — y con ellas la identidad fiscal con la que la clínica emitía hace
 * un año. Las dos consultas de aquí filtran por {@code validTo IS NULL}, que es
 * lo que las hace hablar del presente.
 *
 * <p>
 * <strong>La única {@code @Query} de {@code UPDATE} es el cierre de la
 * vigencia, y tiene motivo.</strong> Ver {@link #closeCurrent}.
 */
public interface CompanyTaxProfileJpaRepository
        extends
            JpaRepository<CompanyTaxProfileJpaEntity, Long> {

    /**
     * El perfil que rige hoy: el único de la empresa con {@code valid_to} nulo.
     * Recorre {@code ix_company_tax_profiles_current (company_id, valid_to)} en el
     * mismo orden en que el índice está escrito.
     *
     * <p>
     * Devuelve {@code Optional} y no {@code List} porque la unicidad no la sostiene
     * esta consulta: la impone {@code uq_company_tax_profiles_current} sobre la
     * columna generada. Si alguna vez llegaran dos, lo correcto es que Spring Data
     * lance en vez de que el servicio elija una en silencio.
     */
    @EntityGraph(attributePaths = {"company", "economicActivity", "responsibilities"})
    @Query("""
            SELECT p
            FROM CompanyTaxProfileJpaEntity p
            WHERE p.company.id = :companyId
              AND p.validTo IS NULL
            """)
    Optional<CompanyTaxProfileJpaEntity> findCurrentByCompanyId(@Param("companyId") Long companyId);

    /**
     * Si la empresa ya tiene ficha <strong>vigente</strong>. Preguntar por
     * «cualquier ficha» sería erróneo desde que hay histórico: una empresa con
     * fichas cerradas y ninguna abierta sí puede volver a abrir una.
     */
    @Query("""
            SELECT COUNT(p) > 0
            FROM CompanyTaxProfileJpaEntity p
            WHERE p.company.id = :companyId
              AND p.validTo IS NULL
            """)
    boolean existsCurrentByCompanyId(@Param("companyId") Long companyId);

    /**
     * Cierra la ficha vigente escribiendo <strong>solo su
     * {@code valid_to}</strong>.
     *
     * <p>
     * <strong>Por qué no es un {@code save} del agregado.</strong> El mapper
     * reconstruye las responsabilidades desde el dominio, y el dominio solo guarda
     * el código: las filas hijas vuelven a nacer sin {@code id}. Guardar una ficha
     * <em>sin cambiarle las responsabilidades</em> —que es exactamente lo que hace
     * el cierre— las reinserta y revienta contra
     * {@code uq_ctp_responsibilities_profile_code} con un
     * {@code Duplicate entry '<perfil>-O-13'}. El camino de edición antiguo no lo
     * veía porque siempre cambiaba los códigos; la sucesión lo pisa siempre. El
     * cierre mueve una columna, así que escribe una columna.
     *
     * <p>
     * <strong>Cumple las dos reglas duras que vigilan un {@code UPDATE}
     * masivo.</strong> {@code version = version + 1} va en el {@code SET} y nunca
     * en el {@code WHERE} ({@code UPDATE_MASIVO_MUEVE_LA_VERSION}, #53): esta tabla
     * va versionada, y sin ese incremento un {@code save} concurrente que venga de
     * una lectura anterior pisaría el cierre sin ruido. Y el {@code WHERE} nombra
     * la empresa ({@code MUTACIONES_SQL_ACOTADAS_POR_EMPRESA}, BE-COV): aquí la
     * lectura previa la hace el caso de uso, pero el filtro es lo que impide cerrar
     * la ficha fiscal de otra clínica.
     *
     * <p>
     * {@code valid_to IS NULL} en el {@code WHERE} <strong>no</strong> es un
     * candado optimista disfrazado: es la definición de «la vigente». Dos
     * sucesiones simultáneas dejan a la segunda con cero filas afectadas, y esa es
     * la señal que el caso de uso convierte en error —la misma barandilla que
     * {@code CustomerCreditBalanceJpaRepository.applyDelta}—.
     *
     * @return filas afectadas: {@code 1} si se cerró, {@code 0} si la ficha ya no
     *         era la vigente o no era de esa empresa
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Transactional
    @Query(value = """
            UPDATE company_tax_profiles
            SET valid_to = :validTo, version = version + 1
            WHERE id = :id
              AND company_id = :companyId
              AND valid_to IS NULL
            """, nativeQuery = true)
    int closeCurrent(@Param("id") Long id, @Param("companyId") Long companyId,
            @Param("validTo") LocalDate validTo);
}
