package com.vetsoftware.app.submodule.infrastructure.persistence;

import com.vetsoftware.app.module.infrastructure.persistence.ModuleJpaEntity;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "sub_modules")
public class SubModuleJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 50)
    private String code;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "module_id", nullable = false)
    private ModuleJpaEntity module;

    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate;

    protected SubModuleJpaEntity() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public ModuleJpaEntity getModule() { return module; }
    public void setModule(ModuleJpaEntity module) { this.module = module; }
    public LocalDateTime getCreatedDate() { return createdDate; }
    public void setCreatedDate(LocalDateTime createdDate) { this.createdDate = createdDate; }
}
