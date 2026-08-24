package com.vetsoftware.app.submodule.domain;

import java.time.LocalDateTime;

/**
 * Un submodulo del arbol de la aplicacion.
 *
 * <p>
 * Lleva dos banderas que el modelo de suscripciones necesita, y las dos nacen
 * en <b>{@code false}</b> a proposito, en contra del instinto:
 *
 * <ul>
 * <li><b>{@code isSellable}</b> — distingue lo vendible de la infraestructura
 * interna. El default seguro es «no se vende»: si un submodulo nuevo entra al
 * arbol y nadie decide si es comercial, el fallo debe ser <em>no aparece en el
 * catalogo</em>, no <em>aparece un articulo al que nadie ha puesto precio</em>.
 * Es lo que evita que «Configuracion del sistema» se ofrezca como modulo
 * comprable.</li>
 * <li><b>{@code readOnlyCapable}</b> — dice si el submodulo sabe funcionar en
 * modo solo lectura. El default seguro es «no lo sabe»: al darse de baja, la
 * pantalla se <em>oculta</em> en vez de mostrarse rota. La alternativa seria
 * ensenar una pantalla que nadie probo en ese modo, con botones que fallan al
 * pulsarlos.</li>
 * </ul>
 */
public class SubModule {
    private Long id;
    private String name;
    private String code;
    private ModuleRef module;
    private boolean sellable;
    private boolean readOnlyCapable;
    private final LocalDateTime createdDate;
    private Long version;
    private boolean enabled;

    public SubModule(Long id, String name, String code, ModuleRef module, boolean sellable,
            boolean readOnlyCapable, LocalDateTime createdDate, Long version, boolean enabled) {
        if (name == null || name.isBlank())
            throw new IllegalArgumentException("name is required");
        if (name.length() > 100)
            throw new IllegalArgumentException("name must be 100 chars or less");
        if (code == null || code.isBlank())
            throw new IllegalArgumentException("code is required");
        if (code.length() > 50)
            throw new IllegalArgumentException("code must be 50 chars or less");
        if (module == null)
            throw new IllegalArgumentException("module is required");
        this.id = id;
        this.name = name;
        this.code = code;
        this.module = module;
        this.sellable = sellable;
        this.readOnlyCapable = readOnlyCapable;
        this.createdDate = createdDate;
        this.version = version;
        this.enabled = enabled;
    }

    public static SubModule create(String name, String code, ModuleRef module, boolean sellable,
            boolean readOnlyCapable, LocalDateTime createdDate) {
        return new SubModule(null, name, code, module, sellable, readOnlyCapable, createdDate, null,
                true);
    }

    public void update(String name, String code, ModuleRef module, boolean sellable,
            boolean readOnlyCapable) {
        if (name == null || name.isBlank())
            throw new IllegalArgumentException("name is required");
        if (name.length() > 100)
            throw new IllegalArgumentException("name must be 100 chars or less");
        if (code == null || code.isBlank())
            throw new IllegalArgumentException("code is required");
        if (code.length() > 50)
            throw new IllegalArgumentException("code must be 50 chars or less");
        if (module == null)
            throw new IllegalArgumentException("module is required");
        this.name = name;
        this.code = code;
        this.module = module;
        this.sellable = sellable;
        this.readOnlyCapable = readOnlyCapable;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void enable() {
        this.enabled = true;
    }

    public void disable() {
        this.enabled = false;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getCode() {
        return code;
    }

    public ModuleRef getModule() {
        return module;
    }

    public boolean isSellable() {
        return sellable;
    }

    public boolean isReadOnlyCapable() {
        return readOnlyCapable;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public Long getVersion() {
        return version;
    }
}
