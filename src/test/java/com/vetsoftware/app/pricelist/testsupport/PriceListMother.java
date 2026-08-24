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

    /** Borrador recien creado, sin id. */
    public static PriceList nuevoBorrador() {
        return PriceList.create("LISTA-2026-01", "Tarifa 2026", "COP", DESDE, null, CREADA_EL);
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
