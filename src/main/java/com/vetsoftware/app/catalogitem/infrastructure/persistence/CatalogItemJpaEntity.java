package com.vetsoftware.app.catalogitem.infrastructure.persistence;

import com.vetsoftware.app.catalogitem.domain.CatalogItemStatus;
import com.vetsoftware.app.catalogitem.domain.ItemType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.LocalDateTime;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

/**
 * {@code catalog_items}, ficha 1 de {@code suscripciones-tablas.md}.
 *
 * <p>
 * <strong>Lleva {@code version}</strong>, y por eso su {@code @SQLDelete}
 * termina en {@code AND version = ?}: en cuanto una entidad declara
 * {@code @Version}, Hibernate liga <em>dos</em> parámetros al SQL del borrado
 * lógico —primero el id, después la versión— y el {@code UPDATE} de un solo
 * parámetro queda roto en tiempo de ejecución sin que el compilador diga nada
 * ({@code BORRADO_LOGICO_RESPETA_LA_VERSION}, regla dura de BE-26).
 *
 * <p>
 * Sin {@code company_id} a propósito: catálogo global de plataforma. Ninguna de
 * sus asociaciones alcanza {@code CompanyJpaEntity}, que es lo que mantiene
 * dormidas las cuatro reglas duras de la familia BE-COV sobre este slice.
 */
@Entity
@Table(name = "catalog_items")
@SQLDelete(sql = "UPDATE catalog_items SET enabled = false WHERE id = ? AND version = ?")
@SQLRestriction("enabled = true")
public class CatalogItemJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", nullable = false, length = 50, unique = true)
    private String code;

    @Column(name = "name", nullable = false, length = 120)
    private String name;

    @Column(name = "short_description", length = 255)
    private String shortDescription;

    @Column(name = "long_description", columnDefinition = "TEXT")
    private String longDescription;

    @Enumerated(EnumType.STRING)
    @Column(name = "item_type", nullable = false, length = 20)
    private ItemType itemType;

    /**
     * El <strong>codigo del eje</strong> ({@code limit_dimensions.code}), no un
     * enumerado. Desde el changeset 333 la columna lleva
     * {@code fk_catalog_items_capacity_unit} contra {@code limit_dimensions(code)},
     * que es lo que sustituyo a la lista literal de cuatro valores: vender un eje
     * nuevo es sembrar una fila alli, no tocar este archivo (#655).
     *
     * <p>
     * <strong>Sin {@code @ManyToOne} a proposito</strong>, por lo mismo que
     * {@code LimitDimensionJpaEntity.sub_module_id}: colgar una asociacion metaria
     * el grafo del catalogo de ejes en cada carga de articulo, y aqui no hace falta
     * ni un solo campo del eje. Lo que hidrata el companion VO es
     * {@code JpaLimitDimensionQueryPort}, con una consulta aparte y solo cuando hay
     * que validar.
     */
    @Column(name = "capacity_unit", length = 50)
    private String capacityUnit;

    @Column(name = "is_core", nullable = false)
    private boolean core;

    @Column(name = "min_quantity", nullable = false)
    private int minQuantity;

    @Column(name = "max_quantity")
    private Integer maxQuantity;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private CatalogItemStatus status;

    /**
     * Política de prueba del artículo (capa I). Las cuatro columnas nacen en el
     * changeset 229 y todavía NO llegan al agregado de dominio ni al mapeador: el
     * caso de uso que las edita es trabajo de la capa I y no de esta migración. Se
     * mapean aquí con valor inicial conservador porque son {@code NOT NULL} en la
     * base y sin ellas cualquier alta de artículo fallaría con
     * {@code Field doesn't have a default value}.
     *
     * <p>
     * El valor inicial es el LADO SEGURO a propósito: {@code NEVER_FREE} significa
     * "o se paga o no se tiene". Un artículo nuevo creado desde la consola mientras
     * la capa I no esté cableada no se regala por accidente, que es el error caro;
     * lo contrario -nacer {@code ELIGIBLE} por defecto- regalaría justo lo que se
     * decidió no regalar (D-04).
     */
    @Column(name = "trial_eligibility", nullable = false, length = 20)
    private String trialEligibility = "NEVER_FREE";

    @Column(name = "default_trial_days")
    private Integer defaultTrialDays;

    @Column(name = "trial_outcome", length = 20)
    private String trialOutcome;

    /**
     * Naturaleza del servicio a ojos de la retención (capa M). Vive en el artículo
     * y no en la configuración de plataforma porque un contrato mixto lleva dos
     * clases a la vez. Comparte lista cerrada con la tarifa de retención: si
     * divergen en un valor, la búsqueda devuelve vacío, la retención esperada sale
     * cero y no hay error.
     */
    @Column(name = "service_nature", nullable = false, length = 30)
    private String serviceNature = "SOFTWARE_LICENSING";

    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    protected CatalogItemJpaEntity() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getShortDescription() {
        return shortDescription;
    }

    public void setShortDescription(String shortDescription) {
        this.shortDescription = shortDescription;
    }

    public String getLongDescription() {
        return longDescription;
    }

    public void setLongDescription(String longDescription) {
        this.longDescription = longDescription;
    }

    public ItemType getItemType() {
        return itemType;
    }

    public void setItemType(ItemType itemType) {
        this.itemType = itemType;
    }

    public String getCapacityUnit() {
        return capacityUnit;
    }

    public void setCapacityUnit(String capacityUnit) {
        this.capacityUnit = capacityUnit;
    }

    public boolean isCore() {
        return core;
    }

    public void setCore(boolean core) {
        this.core = core;
    }

    public int getMinQuantity() {
        return minQuantity;
    }

    public void setMinQuantity(int minQuantity) {
        this.minQuantity = minQuantity;
    }

    public Integer getMaxQuantity() {
        return maxQuantity;
    }

    public void setMaxQuantity(Integer maxQuantity) {
        this.maxQuantity = maxQuantity;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }

    public CatalogItemStatus getStatus() {
        return status;
    }

    public void setStatus(CatalogItemStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getTrialEligibility() {
        return trialEligibility;
    }

    public void setTrialEligibility(String trialEligibility) {
        this.trialEligibility = trialEligibility;
    }

    public Integer getDefaultTrialDays() {
        return defaultTrialDays;
    }

    public void setDefaultTrialDays(Integer defaultTrialDays) {
        this.defaultTrialDays = defaultTrialDays;
    }

    public String getTrialOutcome() {
        return trialOutcome;
    }

    public void setTrialOutcome(String trialOutcome) {
        this.trialOutcome = trialOutcome;
    }

    public String getServiceNature() {
        return serviceNature;
    }

    public void setServiceNature(String serviceNature) {
        this.serviceNature = serviceNature;
    }
}
