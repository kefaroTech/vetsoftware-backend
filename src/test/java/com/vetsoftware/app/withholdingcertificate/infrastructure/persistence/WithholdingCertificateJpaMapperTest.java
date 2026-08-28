package com.vetsoftware.app.withholdingcertificate.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.withholdingcertificate.domain.SubstituteEvidenceKind;
import com.vetsoftware.app.withholdingcertificate.domain.WithholdingCertificate;
import com.vetsoftware.app.withholdingcertificate.domain.WithholdingType;
import com.vetsoftware.app.withholdingcertificate.testsupport.WithholdingCertificateMother;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Ida y vuelta dominio a entidad, sin base y sin mocks.
 *
 * <p>
 * <b>Lo que esta clase vigila y ningun otro test ve</b> es la conversion de
 * {@code fiscalYear}: el dominio lo lleva como {@code Integer} y la columna es
 * {@code SMALLINT}, asi que la entidad lo guarda como {@code short}. Ese
 * estrechamiento es silencioso en Java -2025 cabe, 40000 no- y el unico sitio
 * donde ocurre es aqui.
 */
@DisplayName("WithholdingCertificateJpaMapper — dominio y entidad JPA")
class WithholdingCertificateJpaMapperTest {

    private final WithholdingCertificateJpaMapper mapper = new WithholdingCertificateJpaMapper();

    @Nested
    @DisplayName("Creacion")
    class Creacion {

        @Test
        @DisplayName("el viaje de ida y vuelta devuelve el mismo certificado campo a campo")
        void el_viaje_de_ida_y_vuelta_devuelve_el_mismo_certificado() {
            WithholdingCertificate original = WithholdingCertificateMother.conId(41L);

            WithholdingCertificate vuelta = mapper.toDomain(mapper.toJpa(original));

            assertThat(vuelta.getId()).isEqualTo(41L);
            assertThat(vuelta.getCompanyId()).isEqualTo(WithholdingCertificateMother.COMPANY_ID);
            assertThat(vuelta.getIssuedByTaxId())
                    .isEqualTo(WithholdingCertificateMother.NIT_DEL_CLIENTE);
            assertThat(vuelta.getCertificateNumber()).isEqualTo("CERT-2025-0001");
            assertThat(vuelta.getWithholdingType()).isEqualTo(WithholdingType.INCOME_TAX);
            assertThat(vuelta.getFiscalYear()).isEqualTo(WithholdingCertificateMother.ANO_GRAVABLE);
            assertThat(vuelta.getFiscalPeriodKey()).isEqualTo("2025-A");
            assertThat(vuelta.getRatePercent())
                    .isEqualByComparingTo(WithholdingCertificateMother.TARIFA_RENTA);
            assertThat(vuelta.getCertifiedAmount())
                    .isEqualByComparingTo(WithholdingCertificateMother.IMPORTE_CERTIFICADO);
            assertThat(vuelta.getIssuedOn()).isEqualTo(WithholdingCertificateMother.EXPEDIDO_EL);
            assertThat(vuelta.getLegalDeadlineOn())
                    .isEqualTo(WithholdingCertificateMother.VENCE_EL);
            assertThat(vuelta.getCreatedDate()).isEqualTo(WithholdingCertificateMother.CREADO_EL);
            assertThat(vuelta.getReceivedOn()).isNull();
            assertThat(vuelta.getFileRef()).isNull();
        }

        @Test
        @DisplayName("la tarifa por mil conserva sus seis decimales al cruzar la frontera")
        void la_tarifa_por_mil_conserva_sus_seis_decimales() {
            WithholdingCertificateJpaEntity entidad = mapper
                    .toJpa(WithholdingCertificateMother.deIca());

            assertThat(entidad.getRatePercent())
                    .isEqualByComparingTo(WithholdingCertificateMother.TARIFA_ICA_POR_MIL);
            assertThat(entidad.getRatePercent().scale()).isEqualTo(6);
        }

        @Test
        @DisplayName("el ano gravable viaja al short de la columna y vuelve intacto")
        void el_ano_gravable_viaja_al_short_y_vuelve_intacto() {
            WithholdingCertificateJpaEntity entidad = mapper
                    .toJpa(WithholdingCertificateMother.conId(41L));

            assertThat(entidad.getFiscalYear()).isEqualTo((short) 2025);
            assertThat(mapper.toDomain(entidad).getFiscalYear()).isEqualTo(2025);
        }

        @Test
        @DisplayName("el sustituto y la recepcion viajan en sus propios cuatro campos")
        void el_sustituto_y_la_recepcion_viajan_en_sus_campos() {
            WithholdingCertificateJpaEntity conSustituto = mapper
                    .toJpa(WithholdingCertificateMother.conSustituto(41L));
            WithholdingCertificateJpaEntity recibido = mapper
                    .toJpa(WithholdingCertificateMother.recibido(42L));

            assertThat(conSustituto.getSubstituteEvidenceKind())
                    .isEqualTo(SubstituteEvidenceKind.PAYMENT_RECEIPT);
            assertThat(conSustituto.getSubstituteEvidenceRef())
                    .isEqualTo("s3://pagos/2025/REC-77120.pdf");
            assertThat(conSustituto.getReceivedOn()).isNull();
            assertThat(recibido.getReceivedOn())
                    .isEqualTo(WithholdingCertificateMother.RECIBIDO_EL);
            assertThat(recibido.getFileRef())
                    .isEqualTo("s3://certificados/2025/CERT-2025-0001.pdf");
            assertThat(recibido.getSubstituteEvidenceKind()).isNull();
        }
    }

    @Nested
    @DisplayName("Validaciones")
    class Validaciones {

        @Test
        @DisplayName("apply vuelca sobre la entidad recibida sin tocar su version")
        void apply_vuelca_sobre_la_entidad_recibida_sin_tocar_su_version() {
            // Es lo que permite que el bloqueo optimista siga funcionando en la
            // segunda escritura: si el mapper construyera una entidad nueva, llegaria
            // con version a nulo y Hibernate la tomaria por fila nueva.
            WithholdingCertificateJpaEntity gestionada = mapper
                    .toJpa(WithholdingCertificateMother.conId(41L));
            gestionada.setVersion(3L);

            mapper.apply(WithholdingCertificateMother.recibido(41L), gestionada);

            assertThat(gestionada.getVersion()).isEqualTo(3L);
            assertThat(gestionada.getReceivedOn())
                    .isEqualTo(WithholdingCertificateMother.RECIBIDO_EL);
            assertThat(gestionada.getFileRef())
                    .isEqualTo("s3://certificados/2025/CERT-2025-0001.pdf");
        }

        @Test
        @DisplayName("un certificado sin id se mapea a una entidad sin id, para que la BD lo genere")
        void un_certificado_sin_id_se_mapea_a_una_entidad_sin_id() {
            assertThat(mapper.toJpa(WithholdingCertificateMother.deRenta()).getId()).isNull();
        }
    }
}
