package com.vetsoftware.app.pricelist.domain;

import static com.vetsoftware.app.pricelist.testsupport.PriceListMother.CREADA_EL;
import static com.vetsoftware.app.pricelist.testsupport.PriceListMother.DESDE;
import static com.vetsoftware.app.pricelist.testsupport.PriceListMother.FIRMANTE;
import static com.vetsoftware.app.pricelist.testsupport.PriceListMother.PUBLICADA_EL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vetsoftware.app.pricelist.testsupport.PriceListMother;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("PriceList — la tarifa versionada")
class PriceListTest {

    @Nested
    @DisplayName("Creación")
    class Creacion {

        @Test
        @DisplayName("nace en DRAFT, habilitada y sin firma")
        void nace_en_draft_sin_firma() {
            PriceList lista = PriceListMother.nuevoBorrador();

            assertThat(lista.getStatus()).isEqualTo(PriceListStatus.DRAFT);
            assertThat(lista.isDraft()).isTrue();
            assertThat(lista.isEnabled()).isTrue();
            assertThat(lista.getPublishedAt()).isNull();
            assertThat(lista.getPublishedBySystemUserId()).isNull();
            assertThat(lista.getId()).isNull();
        }

        @Test
        @DisplayName("conserva el código, el nombre, la moneda y la vigencia tal como se dieron")
        void conserva_los_datos_comerciales() {
            PriceList lista = PriceListMother.nuevoBorrador();

            assertThat(lista.getCode()).isEqualTo("LISTA-2026-01");
            assertThat(lista.getName()).isEqualTo("Tarifa 2026");
            assertThat(lista.getCurrency()).isEqualTo("COP");
            assertThat(lista.getValidFrom()).isEqualTo(DESDE);
            assertThat(lista.getValidTo()).isNull();
            assertThat(lista.getCreatedDate()).isEqualTo(CREADA_EL);
        }

        @Test
        @DisplayName("una vigencia que termina el mismo día en que empieza es válida")
        void vigencia_de_un_solo_dia_es_valida() {
            assertThatCode(() -> PriceList.create("L", "Lista", "COP", DESDE, DESDE, CREADA_EL))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("Validaciones")
    class Validaciones {

        @ParameterizedTest
        @ValueSource(strings = {"", "   "})
        @DisplayName("rechaza un código en blanco")
        void rechaza_codigo_en_blanco(String code) {
            assertThatThrownBy(() -> PriceList.create(code, "Lista", "COP", DESDE, null, CREADA_EL))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("code is required");
        }

        @Test
        @DisplayName("rechaza un código nulo")
        void rechaza_codigo_nulo() {
            assertThatThrownBy(() -> PriceList.create(null, "Lista", "COP", DESDE, null, CREADA_EL))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("code is required");
        }

        @Test
        @DisplayName("rechaza un código de más de 50 caracteres")
        void rechaza_codigo_demasiado_largo() {
            assertThatThrownBy(
                    () -> PriceList.create("C".repeat(51), "Lista", "COP", DESDE, null, CREADA_EL))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("code must be 50");
        }

        @Test
        @DisplayName("rechaza un nombre de más de 120 caracteres")
        void rechaza_nombre_demasiado_largo() {
            assertThatThrownBy(
                    () -> PriceList.create("L", "N".repeat(121), "COP", DESDE, null, CREADA_EL))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("name must be 120");
        }

        @ParameterizedTest
        @ValueSource(strings = {"CO", "COPX"})
        @DisplayName("rechaza una moneda que no tiene tres caracteres")
        void rechaza_moneda_de_longitud_distinta_de_tres(String currency) {
            assertThatThrownBy(
                    () -> PriceList.create("L", "Lista", currency, DESDE, null, CREADA_EL))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("currency must be 3");
        }

        @Test
        @DisplayName("rechaza una moneda en minúsculas, igual que chk_price_lists_currency")
        void rechaza_moneda_en_minusculas() {
            assertThatThrownBy(() -> PriceList.create("L", "Lista", "cop", DESDE, null, CREADA_EL))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("currency must be uppercase");
        }

        @Test
        @DisplayName("rechaza una vigencia sin fecha de inicio")
        void rechaza_vigencia_sin_inicio() {
            assertThatThrownBy(() -> PriceList.create("L", "Lista", "COP", null, null, CREADA_EL))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("validFrom is required");
        }

        @Test
        @DisplayName("rechaza una vigencia que termina antes de empezar")
        void rechaza_vigencia_invertida() {
            assertThatThrownBy(() -> PriceList.create("L", "Lista", "COP", DESDE,
                    DESDE.minusDays(1), CREADA_EL)).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("validTo must not be before validFrom");
        }
    }

    @Nested
    @DisplayName("Firma de publicación")
    class Firma {

        @Test
        @DisplayName("un borrador con firma es un estado imposible y se rechaza al construirlo")
        void borrador_firmado_se_rechaza() {
            assertThatThrownBy(() -> new PriceList(1L, "L", "Lista", "COP", DESDE, null,
                    PriceListStatus.DRAFT, PUBLICADA_EL, FIRMANTE, CREADA_EL, 0L, true))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("must not be signed");
        }

        @Test
        @DisplayName("una lista publicada sin firma es un estado imposible y se rechaza")
        void publicada_sin_firma_se_rechaza() {
            assertThatThrownBy(() -> new PriceList(1L, "L", "Lista", "COP", DESDE, null,
                    PriceListStatus.PUBLISHED, null, null, CREADA_EL, 0L, true))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("requires publishedAt and publishedBySystemUserId");
        }

        @Test
        @DisplayName("una firma a medias —fecha sin usuario— también se rechaza")
        void firma_incompleta_se_rechaza() {
            assertThatThrownBy(() -> new PriceList(1L, "L", "Lista", "COP", DESDE, null,
                    PriceListStatus.PUBLISHED, PUBLICADA_EL, null, CREADA_EL, 0L, true))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("requires publishedAt and publishedBySystemUserId");
        }

        @Test
        @DisplayName("publicar exige el usuario que firma")
        void publicar_exige_firmante() {
            PriceList lista = PriceListMother.borrador();

            assertThatThrownBy(() -> lista.publish(null, PUBLICADA_EL))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("publishedBySystemUserId is required");
        }

        @Test
        @DisplayName("publicar exige el momento de la publicación")
        void publicar_exige_momento() {
            PriceList lista = PriceListMother.borrador();

            assertThatThrownBy(() -> lista.publish(FIRMANTE, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("publishedAt is required");
        }
    }

    @Nested
    @DisplayName("Ciclo de vida")
    class CicloDeVida {

        @Test
        @DisplayName("publicar mueve a PUBLISHED y deja la firma puesta")
        void publicar_deja_la_firma() {
            PriceList lista = PriceListMother.borrador();

            lista.publish(FIRMANTE, PUBLICADA_EL);

            assertThat(lista.getStatus()).isEqualTo(PriceListStatus.PUBLISHED);
            assertThat(lista.getPublishedBySystemUserId()).isEqualTo(FIRMANTE);
            assertThat(lista.getPublishedAt()).isEqualTo(PUBLICADA_EL);
            assertThat(lista.isDraft()).isFalse();
        }

        @ParameterizedTest
        @EnumSource(value = PriceListStatus.class, names = {"PUBLISHED", "ARCHIVED"})
        @DisplayName("publicar dos veces no es una transición legal")
        void publicar_fuera_de_draft_es_ilegal(PriceListStatus estado) {
            PriceList lista = PriceListMother.enEstado(estado);

            assertThatThrownBy(() -> lista.publish(FIRMANTE, PUBLICADA_EL))
                    .isInstanceOf(InvalidPriceListTransitionException.class)
                    .hasMessageContaining(estado + " -> PUBLISHED");
        }

        @Test
        @DisplayName("archivar mueve una publicada a ARCHIVED sin tocar su firma ni su contenido")
        void archivar_solo_mueve_el_estado() {
            PriceList lista = PriceListMother.publicada();

            lista.archive();

            assertThat(lista.getStatus()).isEqualTo(PriceListStatus.ARCHIVED);
            assertThat(lista.getPublishedBySystemUserId()).isEqualTo(FIRMANTE);
            assertThat(lista.getPublishedAt()).isEqualTo(PUBLICADA_EL);
            assertThat(lista.getName()).isEqualTo("Tarifa 2026");
            assertThat(lista.getValidFrom()).isEqualTo(DESDE);
        }

        @ParameterizedTest
        @EnumSource(value = PriceListStatus.class, names = {"DRAFT", "ARCHIVED"})
        @DisplayName("solo se archiva lo que está publicado")
        void archivar_fuera_de_published_es_ilegal(PriceListStatus estado) {
            PriceList lista = PriceListMother.enEstado(estado);

            assertThatThrownBy(lista::archive)
                    .isInstanceOf(InvalidPriceListTransitionException.class)
                    .hasMessageContaining(estado + " -> ARCHIVED");
        }
    }

    @Nested
    @DisplayName("Inmutabilidad de una lista publicada (R9)")
    class InmutabilidadR9 {

        @Test
        @DisplayName("un borrador sí se puede editar")
        void el_borrador_se_edita() {
            PriceList lista = PriceListMother.borrador();

            lista.update("Tarifa 2026 rev. B", "USD", DESDE.plusDays(1), LocalDate.of(2026, 6, 30));

            assertThat(lista.getName()).isEqualTo("Tarifa 2026 rev. B");
            assertThat(lista.getCurrency()).isEqualTo("USD");
            assertThat(lista.getValidFrom()).isEqualTo(DESDE.plusDays(1));
            assertThat(lista.getValidTo()).isEqualTo(LocalDate.of(2026, 6, 30));
        }

        @ParameterizedTest
        @EnumSource(value = PriceListStatus.class, names = {"PUBLISHED", "ARCHIVED"})
        @DisplayName("editar una lista fuera de DRAFT se rechaza: cambiaría lo que ya se ofreció")
        void editar_fuera_de_draft_se_rechaza(PriceListStatus estado) {
            PriceList lista = PriceListMother.enEstado(estado);

            assertThatThrownBy(() -> lista.update("Otro nombre", "USD", DESDE, null))
                    .isInstanceOf(PriceListNotEditableException.class)
                    .hasMessageContaining("cannot be modified");
        }

        @ParameterizedTest
        @EnumSource(value = PriceListStatus.class, names = {"PUBLISHED", "ARCHIVED"})
        @DisplayName("la edición rechazada no deja ni un campo cambiado")
        void la_edicion_rechazada_no_muta_nada(PriceListStatus estado) {
            PriceList lista = PriceListMother.enEstado(estado);
            String nombreOriginal = lista.getName();
            String monedaOriginal = lista.getCurrency();

            assertThatThrownBy(() -> lista.update("Otro nombre", "USD", DESDE, null))
                    .isInstanceOf(PriceListNotEditableException.class);

            assertThat(lista.getName()).isEqualTo(nombreOriginal);
            assertThat(lista.getCurrency()).isEqualTo(monedaOriginal);
        }

        @ParameterizedTest
        @EnumSource(value = PriceListStatus.class, names = {"PUBLISHED", "ARCHIVED"})
        @DisplayName("requireDraft es el guardián que heredan los precios de la lista")
        void require_draft_rechaza_fuera_de_draft(PriceListStatus estado) {
            PriceList lista = PriceListMother.enEstado(estado);

            assertThatThrownBy(lista::requireDraft)
                    .isInstanceOf(PriceListNotEditableException.class);
        }

        @Test
        @DisplayName("requireDraft deja pasar un borrador")
        void require_draft_deja_pasar_un_borrador() {
            assertThatCode(PriceListMother.borrador()::requireDraft).doesNotThrowAnyException();
        }

        @ParameterizedTest
        @EnumSource(value = PriceListStatus.class, names = {"PUBLISHED", "ARCHIVED"})
        @DisplayName("dar de baja una lista publicada se rechaza: se archiva, no se esconde")
        void dar_de_baja_fuera_de_draft_se_rechaza(PriceListStatus estado) {
            PriceList lista = PriceListMother.enEstado(estado);

            assertThatThrownBy(lista::disable).isInstanceOf(PriceListNotEditableException.class);
            assertThat(lista.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("un borrador sí se da de baja y se vuelve a habilitar")
        void el_borrador_se_da_de_baja() {
            PriceList lista = PriceListMother.borrador();

            lista.disable();
            assertThat(lista.isEnabled()).isFalse();

            lista.enable();
            assertThat(lista.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("la excepción lleva el id y el estado que bloquearon el cambio")
        void la_excepcion_lleva_el_contexto() {
            PriceList lista = PriceListMother.publicada();

            assertThatThrownBy(lista::requireDraft)
                    .isInstanceOfSatisfying(PriceListNotEditableException.class, ex -> {
                        assertThat(ex.getPriceListId()).isEqualTo(1L);
                        assertThat(ex.getStatus()).isEqualTo(PriceListStatus.PUBLISHED);
                    });
        }
    }
}
