package com.vetsoftware.app.submodule.infrastructure.persistence;

import com.vetsoftware.app.module.infrastructure.persistence.ModuleJpaEntity;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "sub_modules")
@SQLDelete(sql = "UPDATE sub_modules SET enabled = false WHERE id = ? AND version = ?")
@SQLRestriction("enabled = true")
public class SubModuleJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 50, unique = true)
    private String code;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "module_id", nullable = false)
    private ModuleJpaEntity module;

    /**
     * Sin {@code columnDefinition}: el proyecto fija
     * {@code preferred_boolean_jdbc_type: TINYINT}, y un {@code TINYINT(1)} el
     * driver lo reporta como {@code BIT} y rompe {@code ddl-auto: validate}.
     */
    @Column(name = "is_sellable", nullable = false)
    private boolean sellable = false;

    @Column(name = "read_only_capable", nullable = false)
    private boolean readOnlyCapable = false;

    /**
     * R-ENT-05: un submódulo con esta bandera en {@code true} no se degrada jamás
     * -ni por mora, ni por cupo, ni por baja-. Lo lee el mismo recálculo de
     * entitlements que decide READ_ONLY/deshabilitado, no el dominio
     * {@code SubModule} (la bandera se siembra por migración y se consulta por SQL
     * nativo; no forma parte del contrato de API).
     */
    @Column(name = "degradation_immune", nullable = false)
    private boolean degradationImmune = false;

    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    protected SubModuleJpaEntity() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public ModuleJpaEntity getModule() {
        return module;
    }

    public void setModule(ModuleJpaEntity module) {
        this.module = module;
    }

    public boolean isSellable() {
        return sellable;
    }

    public void setSellable(boolean sellable) {
        this.sellable = sellable;
    }

    public boolean isReadOnlyCapable() {
        return readOnlyCapable;
    }

    public void setReadOnlyCapable(boolean readOnlyCapable) {
        this.readOnlyCapable = readOnlyCapable;
    }

    public boolean isDegradationImmune() {
        return degradationImmune;
    }

    public void setDegradationImmune(boolean degradationImmune) {
        this.degradationImmune = degradationImmune;
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
}
