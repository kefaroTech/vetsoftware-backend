package com.vetsoftware.app.revenuerecognitionline.infrastructure.persistence;

import com.vetsoftware.app.revenuerecognitionline.domain.RecognitionMethod;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * {@code revenue_recognition_lines} (changeset 344) — cuanto se gano de verdad,
 * por clinica.
 *
 * <h2>Sin {@code @Version}, y esta escrito en la lista de exenciones</h2>
 *
 * <p>
 * La tabla no tiene columna {@code version} y esta entidad figura en
 * {@code HexagonalArchitectureTest.ENTIDADES_EXENTAS_DE_VERSION} con el codigo
 * {@code E1_APPEND_ONLY}: <b>solo se agrega</b>. Un reconocimiento mal
 * calculado se corrige con otra fila de signo contrario, nunca encima, y ningun
 * caso de uso reescribe el importe. Sin esa entrada,
 * {@code ENTIDADES_CON_BLOQUEO_OPTIMISTA} rompe el build — que es exactamente
 * lo que tiene que hacer con una entidad cuya ausencia de bloqueo nadie haya
 * justificado por escrito.
 *
 * <h2>Sin {@code enabled}</h2>
 *
 * <p>
 * Es un documento de dinero. Deshabilitar un renglon lo sacaria de los totales
 * sin dejar rastro contable de por que; lo que se hace es compensarlo.
 *
 * <h2>Las tres claves foraneas van como escalares</h2>
 *
 * <p>
 * {@code company_id}, {@code charge_id} y {@code posting_period} son ids y
 * claves planas, no asociaciones. Un {@code @ManyToOne} traeria a este slice el
 * grafo de la empresa, el del cargo y el del calendario contable, y obligaria a
 * un {@code @EntityGraph} en cada finder para evitar el N+1 sobre la tabla que
 * mas filas va a tener del bloque. Las claves —incluida la compuesta
 * {@code fk_rrl_charge (company_id, charge_id)}, que es lo que impide colgar el
 * ingreso de una clinica del cargo de otra— siguen existiendo y vigilando en la
 * base.
 *
 * <p>
 * <strong>El campo {@code companyId} es ademas la señal que activa las cuatro
 * reglas duras de BE-COV sobre esta feature</strong>
 * ({@code perteneceAUnaEmpresa} lo busca por nombre). Es correcto que se
 * activen: estas filas si son de alguien.
 */
@Entity
@Table(name = "revenue_recognition_lines")
public class RevenueRecognitionLineJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "company_id", nullable = false)
    private Long companyId;

    @Column(name = "charge_id", nullable = false)
    private Long chargeId;

    /** El mes al que se <b>imputa</b>. {@code CHAR(7)}, formato {@code AAAA-MM}. */
    @Column(name = "period_key", nullable = false, length = 7)
    private String periodKey;

    /**
     * El periodo contable en que se <b>registra</b>. FK a
     * {@code accounting_periods}.
     */
    @Column(name = "posting_period", nullable = false, length = 7)
    private String postingPeriod;

    /**
     * Puede ser negativo: la fila que compensa. {@code precision}/{@code scale}
     * tienen que coincidir con {@code DECIMAL(19,2)} o {@code ddl-auto: validate}
     * lo rechaza al arrancar.
     */
    @Column(name = "recognized_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal recognizedAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "method", nullable = false, length = 25)
    private RecognitionMethod method;

    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate;

    protected RevenueRecognitionLineJpaEntity() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getCompanyId() {
        return companyId;
    }

    public void setCompanyId(Long companyId) {
        this.companyId = companyId;
    }

    public Long getChargeId() {
        return chargeId;
    }

    public void setChargeId(Long chargeId) {
        this.chargeId = chargeId;
    }

    public String getPeriodKey() {
        return periodKey;
    }

    public void setPeriodKey(String periodKey) {
        this.periodKey = periodKey;
    }

    public String getPostingPeriod() {
        return postingPeriod;
    }

    public void setPostingPeriod(String postingPeriod) {
        this.postingPeriod = postingPeriod;
    }

    public BigDecimal getRecognizedAmount() {
        return recognizedAmount;
    }

    public void setRecognizedAmount(BigDecimal recognizedAmount) {
        this.recognizedAmount = recognizedAmount;
    }

    public RecognitionMethod getMethod() {
        return method;
    }

    public void setMethod(RecognitionMethod method) {
        this.method = method;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
    }
}
