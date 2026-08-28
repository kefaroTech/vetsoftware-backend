package com.vetsoftware.app.subscription.domain;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("SubscriptionItemOverlapGuard - la regla que el esquema no puede imponer")
class SubscriptionItemOverlapGuardTest {

    private static final Long ARTICULO = 100L;
    private static final LocalDate ENERO_1 = LocalDate.of(2026, 1, 1);
    private static final LocalDate MAYO_1 = LocalDate.of(2026, 5, 1);
    private static final LocalDate JUNIO_30 = LocalDate.of(2026, 6, 30);
    private static final LocalDate DICIEMBRE_31 = LocalDate.of(2026, 12, 31);

    private static SubscriptionItem tramo(LocalDate from, LocalDate to) {
        return SubscriptionItem.open(42L, 7L, ARTICULO, "VET", "Veterinaria",
                SubscriptionItemType.MODULE, null, 0, TaxTreatment.TAXED, 1,
                new BigDecimal("100.00"), BigDecimal.ZERO, new EffectivePeriod(from, to),
                ItemOrigin.ADDON, null);
    }

    @Test
    @DisplayName("dos tramos con fechas de fin futuras que se pisan se rechazan")
    void tramosConFinFuturoQueSePisan() {
        // El caso exacto que el indice unico sobre current_item_marker NO ve: la linea
        // A del 1-ene al 30-jun y la B del 1-may al 31-dic dan las dos marcador nulo.
        // Sin esta comprobacion, en mayo y junio ese modulo se factura dos veces.
        List<SubscriptionItem> existentes = List.of(tramo(ENERO_1, JUNIO_30));

        assertThatThrownBy(() -> SubscriptionItemOverlapGuard.ensureNoOverlap(ARTICULO, 1,
                new EffectivePeriod(MAYO_1, DICIEMBRE_31), existentes))
                .isInstanceOf(SubscriptionItemOverlapException.class)
                .hasMessageContaining(ARTICULO.toString());
    }

    @Test
    @DisplayName("abrir sobre una linea todavia abierta se rechaza")
    void abrirSobreUnaLineaAbierta() {
        List<SubscriptionItem> existentes = List.of(tramo(ENERO_1, null));

        assertThatThrownBy(() -> SubscriptionItemOverlapGuard.ensureNoOverlap(ARTICULO, 1,
                new EffectivePeriod(MAYO_1, DICIEMBRE_31), existentes))
                .isInstanceOf(SubscriptionItemOverlapException.class);
    }

    @Test
    @DisplayName("un tramo que empieza justo donde acaba el anterior se acepta")
    void tramoConsecutivo() {
        List<SubscriptionItem> existentes = List.of(tramo(ENERO_1, JUNIO_30));

        assertThatCode(() -> SubscriptionItemOverlapGuard.ensureNoOverlap(ARTICULO, 1,
                EffectivePeriod.openFrom(JUNIO_30), existentes)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("sin lineas previas no hay nada que comprobar")
    void sinLineasPrevias() {
        assertThatCode(() -> SubscriptionItemOverlapGuard.ensureNoOverlap(ARTICULO, 1,
                EffectivePeriod.openFrom(ENERO_1), List.of())).doesNotThrowAnyException();
        assertThatCode(() -> SubscriptionItemOverlapGuard.ensureNoOverlap(ARTICULO, 1,
                EffectivePeriod.openFrom(ENERO_1), null)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("basta con que UNA de las lineas se pise para rechazar")
    void bastaUnaLinea() {
        List<SubscriptionItem> existentes = List.of(tramo(ENERO_1, LocalDate.of(2026, 2, 1)),
                tramo(MAYO_1, DICIEMBRE_31));

        assertThatThrownBy(() -> SubscriptionItemOverlapGuard.ensureNoOverlap(ARTICULO, 1,
                new EffectivePeriod(JUNIO_30, null), existentes))
                .isInstanceOf(SubscriptionItemOverlapException.class);
    }

    @Test
    @DisplayName("sin tramo candidato falla: no hay nada que comparar")
    void sinTramoCandidato() {
        assertThatThrownBy(
                () -> SubscriptionItemOverlapGuard.ensureNoOverlap(ARTICULO, 1, null, List.of()))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("candidate");
    }

    @Test
    @DisplayName("una lista nula de lineas previas se trata igual que ninguna linea")
    void listaNulaDeLineasPrevias() {
        assertThatCode(() -> SubscriptionItemOverlapGuard.ensureNoOverlap(ARTICULO, 1,
                EffectivePeriod.openFrom(ENERO_1), null)).doesNotThrowAnyException();
    }
}
