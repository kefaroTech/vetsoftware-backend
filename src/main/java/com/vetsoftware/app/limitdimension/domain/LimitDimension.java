package com.vetsoftware.app.limitdimension.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Un eje limitable: qué cosa se puede contar y cómo se mide.
 *
 * <p>
 * <strong>Por qué existe la tabla.</strong> Hasta ahora la lista de cosas
 * contables vivía escrita a mano dentro de tres {@code CHECK} y de dos ficheros
 * de código, así que vender «hasta 100 mascotas» era literalmente una migración
 * de esquema. Con esta fila es insertar un registro.
 *
 * <p>
 * <strong>No lleva empresa, y eso es la mitad del diseño.</strong> Es catálogo
 * global de plataforma, igual que {@code catalog_items}. Nada de esta rodaja
 * puede alcanzar la entidad de empresas: en cuanto una entidad de un slice
 * global llega ahí, las cuatro reglas duras de la familia BE-COV se activan
 * sobre la feature entera y rompen el build. Lo que lleva empresa —contadores,
 * excepciones negociadas, bitácora— vive en otras tablas y apunta aquí por id.
 *
 * <p>
 * <strong>{@code availableFrom} no es metadato.</strong> Responde a D-74: «sin
 * fila porque no se vendió» y «sin fila porque el eje no existía cuando se
 * firmó» tienen respuestas <em>opuestas</em> —techo cero la primera, sin techo
 * la segunda— y sin esta fecha no se distinguen. Un eje de citas añadido hoy
 * dejaría bloqueadas mañana todas las agendas firmadas antes de que existiera.
 *
 * <p>
 * <strong>No es una bolsa de atributos sin tipo.</strong> En ese antipatrón el
 * valor puede ser cualquier cosa y acaba en un campo de texto. Aquí cada eje
 * tiene un solo atributo, de un solo tipo: un entero no negativo. Lo que se
 * cataloga es el eje, no el tipo del dato.
 */
public class LimitDimension {

    private static final int CODE_MAX = 50;
    private static final int NAME_MAX = 120;

    private final Long id;
    private final String code;
    private String name;
    private final MeasureKind measureKind;
    private SubModuleRef subModule;
    private Integer releaseDelayDays;
    private final LocalDate availableFrom;
    private final LocalDateTime createdDate;
    private final boolean enabled;
    private final Long version;

    public LimitDimension(Long id, String code, String name, MeasureKind measureKind,
            SubModuleRef subModule, Integer releaseDelayDays, LocalDate availableFrom,
            LocalDateTime createdDate, boolean enabled, Long version) {
        validate(code, name, measureKind, releaseDelayDays, availableFrom);
        this.id = id;
        this.code = code;
        this.name = name;
        this.measureKind = measureKind;
        this.subModule = subModule;
        this.releaseDelayDays = releaseDelayDays;
        this.availableFrom = availableFrom;
        this.createdDate = createdDate;
        this.enabled = enabled;
        this.version = version;
    }

    /** Eje recién declarado: sin id y activo. */
    public static LimitDimension create(String code, String name, MeasureKind measureKind,
            SubModuleRef subModule, Integer releaseDelayDays, LocalDate availableFrom,
            LocalDateTime createdDate) {
        return new LimitDimension(null, code, name, measureKind, subModule, releaseDelayDays,
                availableFrom, createdDate, true, null);
    }

    /**
     * Lo editable es lo que no rompe nada aguas abajo. <strong>El tipo de medida no
     * se puede cambiar desde aquí</strong>, y no por prudencia: la copia atada por
     * clave foránea contra {@code (id, measure_kind)} convierte ese cambio en un
     * error del motor en cuanto hay un artículo vendido, y una operación que muere
     * a mitad de transacción es peor que una que no existe. Cambiar el tipo de un
     * eje es retirarlo y declarar otro.
     */
    public void update(String name, SubModuleRef subModule, Integer releaseDelayDays) {
        validate(this.code, name, this.measureKind, releaseDelayDays, this.availableFrom);
        this.name = name;
        this.subModule = subModule;
        this.releaseDelayDays = releaseDelayDays;
    }

    /**
     * Espeja {@code chk_limit_dimensions_measure_kind} y
     * {@code chk_limit_dimensions_release_delay}. Cada regla de aquí tiene su
     * gemela en el esquema: si una de las dos cambia, cambian las dos.
     */
    private static void validate(String code, String name, MeasureKind measureKind,
            Integer releaseDelayDays, LocalDate availableFrom) {
        if (code == null || code.isBlank())
            throw new IllegalArgumentException("code is required");
        if (code.length() > CODE_MAX)
            throw new IllegalArgumentException("code must be " + CODE_MAX + " chars or less");
        if (name == null || name.isBlank())
            throw new IllegalArgumentException("name is required");
        if (name.length() > NAME_MAX)
            throw new IllegalArgumentException("name must be " + NAME_MAX + " chars or less");
        if (measureKind == null)
            throw new IllegalArgumentException("measure kind is required");
        if (availableFrom == null)
            throw new IllegalArgumentException("available from is required");
        if (measureKind.requiresReleaseDelay()) {
            if (releaseDelayDays == null)
                throw new IllegalArgumentException(
                        "release delay days is required for a CUMULATIVE dimension");
            if (releaseDelayDays < 0)
                throw new IllegalArgumentException("release delay days cannot be negative");
        } else if (releaseDelayDays != null) {
            throw new IllegalArgumentException(
                    "release delay days only applies to a CUMULATIVE dimension");
        }
    }

    /**
     * D-74, la mitad que el motor no puede decidir. Si el eje nació después de que
     * el contrato se firmara, la ausencia de contador <strong>no</strong> es techo
     * cero: es que a ese cliente todavía no se le vendió nada sobre este eje y
     * bloquearle sería castigarle por una decisión de producto posterior a su
     * firma.
     *
     * @param contractSignedOn
     *            la fecha de firma del contrato con la que se compara
     * @return {@code true} si el eje ya existía al firmar, y entonces la ausencia
     *         de fila sí significa techo cero
     */
    public boolean existedOn(LocalDate contractSignedOn) {
        if (contractSignedOn == null)
            throw new IllegalArgumentException("contract signed on is required");
        return !availableFrom.isAfter(contractSignedOn);
    }

    public Long getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public MeasureKind getMeasureKind() {
        return measureKind;
    }

    public SubModuleRef getSubModule() {
        return subModule;
    }

    public Integer getReleaseDelayDays() {
        return releaseDelayDays;
    }

    public LocalDate getAvailableFrom() {
        return availableFrom;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public Long getVersion() {
        return version;
    }
}
