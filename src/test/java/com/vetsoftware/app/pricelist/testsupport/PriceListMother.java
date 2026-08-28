package com.vetsoftware.app.pricelist.testsupport;

import com.vetsoftware.app.pricelist.domain.PriceList;
import com.vetsoftware.app.pricelist.domain.PriceListStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Object mother de {@code pricelist}. No se comparte con otras features. */
public final class PriceListMother {

    public static final LocalDateTime CREADA_EL = LocalDateTime.of(2026, 1, 15, 10, 30);
    public static final LocalDateTime PUBLICADA_EL = LocalDateTime.of(2026, 1, 20, 9, 0);
    public static final LocalDate DESDE = LocalDate.of(2026, 2, 1);
    public static final Long FIRMANTE = 7L;

    private PriceListMother() {
    }

    /**
     * Borrador recien creado, sin id.
     *
     * <p>
     * <b>Su codigo NO es {@code LISTA-2026-01}, y no puede serlo.</b> Ese lo
     * siembra el changeset {@code 310_seed_price_list_2026} y
     * {@code uq_price_lists_code} es un unico global sin empresa, asi que la unica
     * rodaja que persiste este mother —{@code PriceListPersistenceIT}— moria con
     * «Duplicate entry 'LISTA-2026-01' for key 'price_lists.uq_price_lists_code'»
     * antes de llegar a su primera asercion. La semilla de produccion manda: lo que
     * se mueve es la fila del test.
     *
     * <p>
     * Las tres factorias de abajo si conservan el codigo comercial porque no tocan
     * la base —llevan {@code id = 1L} puesto a mano y sirven a tests de dominio, de
     * mapper y de controller—, y ahi el codigo real es parte de lo que describen.
     */
    public static PriceList nuevoBorrador() {
        return PriceList.create("LISTA-TEST-2026", "Tarifa 2026", "COP", DESDE, null, CREADA_EL);
    }

    /** Borrador ya persistido: tiene id y version. */
    public static PriceList borrador() {
        return new PriceList(1L, "LISTA-2026-01", "Tarifa 2026", "COP", DESDE, null,
                PriceListStatus.DRAFT, null, null, CREADA_EL, 0L, true);
    }

    /** Publicada: congelada, ella y sus precios. */
    public static PriceList publicada() {
        return new PriceList(1L, "LISTA-2026-01", "Tarifa 2026", "COP", DESDE, null,
                PriceListStatus.PUBLISHED, PUBLICADA_EL, FIRMANTE, CREADA_EL, 1L, true);
    }

    public static PriceList archivada() {
        return new PriceList(1L, "LISTA-2026-01", "Tarifa 2026", "COP", DESDE,
                LocalDate.of(2026, 12, 31), PriceListStatus.ARCHIVED, PUBLICADA_EL, FIRMANTE,
                CREADA_EL, 2L, true);
    }

    public static PriceList enEstado(PriceListStatus status) {
        return switch (status) {
            case DRAFT -> borrador();
            case PUBLISHED -> publicada();
            case ARCHIVED -> archivada();
        };
    }
}
