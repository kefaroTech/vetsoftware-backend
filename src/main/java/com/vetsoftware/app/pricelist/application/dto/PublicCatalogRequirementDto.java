package com.vetsoftware.app.pricelist.application.dto;

/**
 * Un arco {@code REQUIRES} tal como sale por HTTP.
 *
 * <p>
 * Misma forma que {@link PublicCatalogRequirementRowDto} y record aparte por lo
 * mismo que {@link PublicCatalogItemDto} lo es de
 * {@link PublicCatalogItemRowDto}: el read model puede crecer con una columna
 * que la respuesta publica no quiera —{@code note}, el tipo de relacion, el
 * orden— y con un solo record esa columna aparece en la portada el dia que
 * alguien la anada, sin que nadie lo decida. Aqui la proyeccion es explicita.
 */
public record PublicCatalogRequirementDto(String itemCode, String requiredItemCode) {

    /** Proyecta la fila plana del read model a la forma que sale por HTTP. */
    public static PublicCatalogRequirementDto from(PublicCatalogRequirementRowDto row) {
        return new PublicCatalogRequirementDto(row.itemCode(), row.requiredItemCode());
    }
}
