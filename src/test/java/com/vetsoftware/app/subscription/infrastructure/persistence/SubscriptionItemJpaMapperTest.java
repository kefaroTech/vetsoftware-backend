package com.vetsoftware.app.subscription.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.subscription.domain.EffectivePeriod;
import com.vetsoftware.app.subscription.domain.ItemOrigin;
import com.vetsoftware.app.subscription.domain.SubscriptionItem;
import com.vetsoftware.app.subscription.domain.SubscriptionItemType;
import com.vetsoftware.app.subscription.domain.TaxTreatment;
import com.vetsoftware.app.subscription.testsupport.SubscriptionMother;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Ida y vuelta dominio ↔ entidad de la línea de contrato.
 *
 * <p>
 * El caso que justifica la clase es el del {@code Clock}: el mapper es el único
 * sitio del slice que pone una fecha de creación cuando no viene, y lo hace con
 * un reloj inyectado. Con {@code LocalDateTime.now()} pelado no habría forma de
 * afirmar ese campo sin escribir un test que se cae al cruzar medianoche.
 */
@DisplayName("SubscriptionItemJpaMapper — ida y vuelta de la línea")
class SubscriptionItemJpaMapperTest {

    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 1, 15, 10, 15, 30);

    private final SubscriptionItemJpaMapper mapper = new SubscriptionItemJpaMapper(
            Clock.fixed(Instant.parse("2026-01-15T10:15:30Z"), ZoneOffset.UTC));

    /**
     * Subclase anonima porque el constructor sin argumentos de la entidad es
     * {@code protected} —solo lo usa Hibernate—; es el mismo recurso que ya emplea
     * {@code EntitlementJpaMappersTest}.
     */
    private static CompanyJpaEntity empresa() {
        CompanyJpaEntity company = new CompanyJpaEntity() {
        };
        company.setId(SubscriptionMother.EMPRESA);
        return company;
    }

    private static SubscriptionJpaEntity contrato() {
        SubscriptionJpaEntity subscription = new SubscriptionJpaEntity();
        subscription.setId(SubscriptionMother.CONTRATO);
        return subscription;
    }

    @Nested
    @DisplayName("Hacia la entidad")
    class HaciaLaEntidad {

        @Test
        @DisplayName("copia los datos congelados sin tocarlos")
        void copiaLosDatosCongelados() {
            SubscriptionItemJpaEntity entity = mapper.toJpa(SubscriptionMother.lineaAbierta(),
                    empresa(), contrato());

            assertThat(entity.getId()).isNull();
            assertThat(entity.getCompany().getId()).isEqualTo(SubscriptionMother.EMPRESA);
            assertThat(entity.getSubscription().getId()).isEqualTo(SubscriptionMother.CONTRATO);
            assertThat(entity.getCatalogItemId()).isEqualTo(SubscriptionMother.ARTICULO);
            assertThat(entity.getItemCode()).isEqualTo("EXTRA_USER");
            assertThat(entity.getItemType()).isEqualTo(SubscriptionItemType.CAPACITY);
            assertThat(entity.getCapacityUnit()).isEqualTo("USER");
            assertThat(entity.getIncludedQuantity()).isEqualTo(2);
            assertThat(entity.getTaxTreatment()).isEqualTo(TaxTreatment.TAXED);
            assertThat(entity.getQuantity()).isEqualTo(5);
            assertThat(entity.getUnitAmount()).isEqualByComparingTo(SubscriptionMother.PRECIO);
            assertThat(entity.getTaxRate()).isEqualByComparingTo(SubscriptionMother.IVA);
            assertThat(entity.getOrigin()).isEqualTo(ItemOrigin.ADDON);
            assertThat(entity.getCreatedAmendmentId()).isEqualTo(11L);
            assertThat(entity.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("la vigencia se parte en las dos columnas, y la abierta deja el fin nulo")
        void laVigenciaSeParteEnDosColumnas() {
            SubscriptionItemJpaEntity abierta = mapper.toJpa(SubscriptionMother.lineaAbierta(),
                    empresa(), contrato());
            SubscriptionItemJpaEntity cerrada = mapper.toJpa(SubscriptionMother
                    .lineaEntre(SubscriptionMother.ENERO_1, SubscriptionMother.JUNIO_30), empresa(),
                    contrato());

            assertThat(abierta.getEffectiveFrom()).isEqualTo(SubscriptionMother.ENERO_1);
            // effective_to nulo es lo que llena current_item_marker y activa el indice
            // unico: escribir aqui una fecha «infinita» lo desactivaria en silencio.
            assertThat(abierta.getEffectiveTo()).isNull();
            assertThat(cerrada.getEffectiveTo()).isEqualTo(SubscriptionMother.JUNIO_30);
        }

        @Test
        @DisplayName("una linea nueva se fecha con el reloj inyectado, no con el del sistema")
        void unaLineaNuevaSeFechaConElReloj() {
            SubscriptionItemJpaEntity entity = mapper.toJpa(SubscriptionMother.lineaAbierta(),
                    empresa(), contrato());

            assertThat(entity.getCreatedDate()).isEqualTo(AHORA);
        }

        @Test
        @DisplayName("una linea que ya tiene fecha conserva la suya: el reloj no la pisa")
        void unaLineaConFechaConservaLaSuya() {
            // Reguardar una linea existente no puede reescribirle la fecha de alta: es
            // parte de la prueba de desde cuando ese cliente tuvo ese modulo.
            LocalDateTime original = LocalDateTime.of(2025, 3, 1, 8, 0);
            SubscriptionItem existente = new SubscriptionItem(7L, SubscriptionMother.EMPRESA,
                    SubscriptionMother.CONTRATO, SubscriptionMother.ARTICULO, "CORE", "Nucleo",
                    SubscriptionItemType.MODULE, null, 0, TaxTreatment.TAXED, 1,
                    SubscriptionMother.PRECIO, BigDecimal.ZERO,
                    EffectivePeriod.openFrom(SubscriptionMother.ENERO_1), ItemOrigin.INITIAL, null,
                    null, original, 4L, true);

            SubscriptionItemJpaEntity entity = mapper.toJpa(existente, empresa(), contrato());

            assertThat(entity.getCreatedDate()).isEqualTo(original);
            assertThat(entity.getVersion()).isEqualTo(4L);
            assertThat(entity.getId()).isEqualTo(7L);
        }
    }

    @Nested
    @DisplayName("Hacia el dominio")
    class HaciaElDominio {

        @Test
        @DisplayName("la vuelta reconstruye la linea entera, incluida la vigencia")
        void laVueltaReconstruyeLaLinea() {
            SubscriptionItemJpaEntity entity = mapper.toJpa(SubscriptionMother.lineaAbierta(),
                    empresa(), contrato());
            entity.setId(7L);
            entity.setVersion(0L);

            SubscriptionItem linea = mapper.toDomain(entity);

            assertThat(linea.getId()).isEqualTo(7L);
            assertThat(linea.getCompanyId()).isEqualTo(SubscriptionMother.EMPRESA);
            assertThat(linea.getSubscriptionId()).isEqualTo(SubscriptionMother.CONTRATO);
            assertThat(linea.getPeriod().from()).isEqualTo(SubscriptionMother.ENERO_1);
            assertThat(linea.getPeriod().isOpen()).isTrue();
            assertThat(linea.getIncludedQuantity()).isEqualTo(2);
            assertThat(linea.getQuantity()).isEqualTo(5);
            assertThat(linea.billableQuantity()).isEqualTo(3);
            assertThat(linea.getCapacityUnit()).isEqualTo("USER");
            assertThat(linea.getCreatedDate()).isEqualTo(AHORA);
        }

        @Test
        @DisplayName("una linea cerrada vuelve con su fecha de fin y su otrosi de cierre")
        void unaLineaCerradaVuelveConSuCierre() {
            SubscriptionItem cerrada = SubscriptionMother.lineaAbierta();
            cerrada.endOn(SubscriptionMother.JUNIO_30, 99L);
            SubscriptionItemJpaEntity entity = mapper.toJpa(cerrada, empresa(), contrato());
            entity.setId(7L);
            entity.setVersion(1L);

            SubscriptionItem vuelta = mapper.toDomain(entity);

            assertThat(vuelta.getPeriod().to()).isEqualTo(SubscriptionMother.JUNIO_30);
            assertThat(vuelta.getEndedAmendmentId()).isEqualTo(99L);
            // Dar de baja no borra ni desactiva: la fila vuelve habilitada.
            assertThat(vuelta.isEnabled()).isTrue();
            assertThat(vuelta.isCurrentOn(LocalDate.of(2026, 3, 15))).isTrue();
            assertThat(vuelta.isCurrentOn(SubscriptionMother.JUNIO_30)).isFalse();
        }
    }
}
