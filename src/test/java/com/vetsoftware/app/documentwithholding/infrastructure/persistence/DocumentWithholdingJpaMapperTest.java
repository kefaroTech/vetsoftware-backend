package com.vetsoftware.app.documentwithholding.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.documentwithholding.domain.DocumentWithholding;
import com.vetsoftware.app.documentwithholding.domain.WithholdingType;
import com.vetsoftware.app.documentwithholding.testsupport.DocumentWithholdingMother;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * El unico sitio que conoce a la vez el dominio y la entidad JPA, y por tanto
 * el unico donde catorce componentes se pueden cruzar en silencio.
 *
 * <p>
 * <b>Los dos fallos que esta clase existe para cazar</b> no los ve ningun test
 * de servicio:
 *
 * <ul>
 * <li><b>El {@code version} que no viaja.</b> Si {@code toJpa} lo dejara sin
 * poner, Hibernate veria una entidad con version nula, la trataria como nueva e
 * <i>insertaria una fila duplicada</i> en vez de actualizar la existente. La
 * retencion original quedaria sin certificado y nadie lo notaria hasta cuadrar
 * el ano.</li>
 * <li><b>El estrechamiento del ano a {@code short}.</b> La columna es
 * {@code SMALLINT}; si alguien cambiara el tipo del campo, el ano podria
 * truncarse y la retencion acabaria imputada a otro ejercicio.</li>
 * </ul>
 */
@DisplayName("DocumentWithholdingJpaMapper — catorce campos que se pueden cruzar")
class DocumentWithholdingJpaMapperTest {

    private final DocumentWithholdingJpaMapper mapper = new DocumentWithholdingJpaMapper();

    @Nested
    @DisplayName("Creacion")
    class Creacion {

        @Test
        @DisplayName("lleva cada campo a su columna sin cruzar importes ni fechas")
        void lleva_cada_campo_a_su_columna() {
            DocumentWithholdingJpaEntity entidad = mapper.toJpa(DocumentWithholdingMother.ica());

            assertThat(entidad.getCompanyId()).isEqualTo(DocumentWithholdingMother.EMPRESA);
            assertThat(entidad.getBillingDocumentId()).isEqualTo(DocumentWithholdingMother.FACTURA);
            assertThat(entidad.getType()).isEqualTo(WithholdingType.ICA);
            // Los tres decimales van a columnas distintas y valen cosas distintas: si
            // se cruzaran, la retencion diria haberse llevado la base entera.
            assertThat(entidad.getTaxableBase()).isEqualByComparingTo("1234567.89");
            assertThat(entidad.getRatePercent()).isEqualByComparingTo("0.690000");
            assertThat(entidad.getAmount()).isEqualByComparingTo("8518.52");
            assertThat(entidad.getMunicipalityCode()).isEqualTo("05001");
            assertThat(entidad.getFiscalYear()).isEqualTo((short) 2026);
            assertThat(entidad.getFiscalPeriodKey()).isEqualTo("2026-B02");
            // Las dos fechas son distintas a proposito: practicedOn es cuando ocurrio
            // el hecho, createdDate cuando se registro.
            assertThat(entidad.getPracticedOn()).isEqualTo(LocalDate.of(2026, 3, 5));
            assertThat(entidad.getCreatedDate()).isEqualTo(LocalDateTime.of(2026, 3, 7, 8, 45, 0));
            assertThat(entidad.getCertificateId()).isNull();
        }

        @Test
        @DisplayName("la ida y vuelta devuelve una retencion identica a la de partida")
        void la_ida_y_vuelta_devuelve_una_retencion_identica() {
            DocumentWithholding original = DocumentWithholdingMother.yaCertificada(41L, 8410L);

            DocumentWithholding recuperada = mapper.toDomain(mapper.toJpa(original));

            assertThat(recuperada).usingRecursiveComparison().isEqualTo(original);
        }
    }

    @Nested
    @DisplayName("Validaciones")
    class Validaciones {

        @Test
        @DisplayName("el version viaja en los dos sentidos, que es lo que evita el duplicado")
        void el_version_viaja_en_los_dos_sentidos() {
            DocumentWithholding yaEscrita = DocumentWithholdingMother.yaCertificada(41L, 8410L);

            DocumentWithholdingJpaEntity entidad = mapper.toJpa(yaEscrita);

            assertThat(entidad.getId()).isEqualTo(41L);
            // Sin esta linea, Hibernate trataria la entidad como nueva y el UPDATE se
            // convertiria en un INSERT.
            assertThat(entidad.getVersion()).isEqualTo(3L);
            assertThat(mapper.toDomain(entidad).getVersion()).isEqualTo(3L);
        }

        @Test
        @DisplayName("una retencion nueva viaja sin id y sin version, para que se inserte")
        void una_retencion_nueva_viaja_sin_id_y_sin_version() {
            DocumentWithholdingJpaEntity entidad = mapper.toJpa(DocumentWithholdingMother.renta());

            assertThat(entidad.getId()).isNull();
            assertThat(entidad.getVersion()).isNull();
        }

        @Test
        @DisplayName("el ano gravable sobrevive al estrechamiento a short en los dos extremos")
        void el_ano_gravable_sobrevive_al_estrechamiento() {
            // 2020 y 2100 son los extremos que el CHECK admite. Los dos caben de sobra
            // en un short, y este caso lo congela por si alguien ampliara el rango sin
            // mirar el tipo de la columna.
            assertThat(mapper.toJpa(retencionDelAno(2020)).getFiscalYear()).isEqualTo((short) 2020);
            assertThat(mapper.toJpa(retencionDelAno(2100)).getFiscalYear()).isEqualTo((short) 2100);
            assertThat(mapper.toDomain(mapper.toJpa(retencionDelAno(2100))).getFiscalYear())
                    .isEqualTo(2100);
        }

        @Test
        @DisplayName("un municipio ausente viaja como nulo y no como cadena vacia")
        void un_municipio_ausente_viaja_como_nulo() {
            // El centinela '-' lo pone la columna generada de la base, no el mapper.
            // Escribir aqui una cadena vacia romperia la FK contra cities.dane_code.
            assertThat(mapper.toJpa(DocumentWithholdingMother.renta()).getMunicipalityCode())
                    .isNull();
        }
    }

    // --- andamio ------------------------------------------------------------

    private static DocumentWithholding retencionDelAno(int ano) {
        return DocumentWithholding.register(DocumentWithholdingMother.EMPRESA,
                DocumentWithholdingMother.FACTURA, WithholdingType.INCOME_TAX,
                DocumentWithholdingMother.BASE_GRAVABLE, DocumentWithholdingMother.TARIFA_RENTA,
                DocumentWithholdingMother.RETENIDO, null, ano, ano + "-A",
                DocumentWithholdingMother.PRACTICADA_EL, DocumentWithholdingMother.CREADA_EL);
    }
}
