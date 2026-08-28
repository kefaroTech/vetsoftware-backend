package com.vetsoftware.app.billingdocumentstatushistory.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.billingdocumentstatushistory.domain.BillingDocumentStatus;
import com.vetsoftware.app.billingdocumentstatushistory.domain.BillingDocumentStatusHistory;
import com.vetsoftware.app.billingdocumentstatushistory.testsupport.BillingDocumentStatusHistoryMother;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("BillingDocumentStatusHistoryJpaMapper — ida y vuelta dominio/entidad")
class BillingDocumentStatusHistoryJpaMapperTest {

    private final BillingDocumentStatusHistoryJpaMapper mapper = new BillingDocumentStatusHistoryJpaMapper();

    @Nested
    @DisplayName("Creacion")
    class Creacion {

        @Test
        @DisplayName("lleva a la entidad JPA los nueve campos sin cruzar ninguno")
        void lleva_a_la_entidad_los_nueve_campos() {
            BillingDocumentStatusHistoryJpaEntity entidad = mapper
                    .toJpa(BillingDocumentStatusHistoryMother.yaRegistrado(41L));

            assertThat(entidad.getId()).isEqualTo(41L);
            assertThat(entidad.getCompanyId())
                    .isEqualTo(BillingDocumentStatusHistoryMother.EMPRESA);
            assertThat(entidad.getBillingDocumentId())
                    .isEqualTo(BillingDocumentStatusHistoryMother.DOCUMENTO);
            assertThat(entidad.getFromStatus()).isEqualTo(BillingDocumentStatus.DRAFT);
            assertThat(entidad.getToStatus()).isEqualTo(BillingDocumentStatus.AWAITING_EXTERNAL);
            assertThat(entidad.getActor())
                    .isEqualTo(BillingDocumentStatusHistoryMother.ACTOR_PERSONA);
            assertThat(entidad.getReason()).isEqualTo(BillingDocumentStatusHistoryMother.MOTIVO);
            assertThat(entidad.getOccurredAt())
                    .isEqualTo(BillingDocumentStatusHistoryMother.OCURRIO_EL);
            assertThat(entidad.getCreatedDate())
                    .isEqualTo(BillingDocumentStatusHistoryMother.CREADO_EL);
        }

        @Test
        @DisplayName("no cruza el momento del cambio con el de escritura")
        void no_cruza_el_momento_del_cambio_con_el_de_escritura() {
            // Los dos son LocalDateTime y van seguidos en el constructor del dominio:
            // cruzarlos compila. Con los dos instantes distintos, el cruce se ve.
            BillingDocumentStatusHistoryJpaEntity entidad = mapper
                    .toJpa(BillingDocumentStatusHistoryMother.yaRegistrado(41L));

            assertThat(entidad.getOccurredAt()).isBefore(entidad.getCreatedDate());
        }

        @Test
        @DisplayName("un fotograma sin id viaja con el id nulo, que es lo que hace el INSERT")
        void un_fotograma_sin_id_viaja_con_el_id_nulo() {
            // Si el mapper inventara un id, Hibernate creeria que la fila ya existe y
            // lanzaria un UPDATE contra una fila que no esta.
            BillingDocumentStatusHistoryJpaEntity entidad = mapper
                    .toJpa(BillingDocumentStatusHistoryMother.haciaEsperaExterna());

            assertThat(entidad.getId()).isNull();
        }
    }

    @Nested
    @DisplayName("Validaciones")
    class Validaciones {

        @Test
        @DisplayName("la vuelta reconstruye el fotograma identico al que salio")
        void la_vuelta_reconstruye_el_fotograma_identico() {
            BillingDocumentStatusHistory original = BillingDocumentStatusHistoryMother
                    .yaRegistrado(41L);

            BillingDocumentStatusHistory vuelta = mapper.toDomain(mapper.toJpa(original));

            assertThat(vuelta.getId()).isEqualTo(original.getId());
            assertThat(vuelta.getCompanyId()).isEqualTo(original.getCompanyId());
            assertThat(vuelta.getBillingDocumentId()).isEqualTo(original.getBillingDocumentId());
            assertThat(vuelta.getFromStatus()).isEqualTo(original.getFromStatus());
            assertThat(vuelta.getToStatus()).isEqualTo(original.getToStatus());
            assertThat(vuelta.getOccurredAt()).isEqualTo(original.getOccurredAt());
            assertThat(vuelta.getActor()).isEqualTo(original.getActor());
            assertThat(vuelta.getReason()).isEqualTo(original.getReason());
            assertThat(vuelta.getCreatedDate()).isEqualTo(original.getCreatedDate());
        }

        @Test
        @DisplayName("los dos estados sobreviven al viaje como enum y no se confunden entre si")
        void los_dos_estados_sobreviven_al_viaje() {
            // Son del mismo tipo y van seguidos: si el mapper los intercambiara, la
            // pelicula quedaria contada al reves y el informe de vigilancia con ella.
            BillingDocumentStatusHistory vuelta = mapper.toDomain(
                    mapper.toJpa(BillingDocumentStatusHistoryMother.haciaRegistroExterno()));

            assertThat(vuelta.getFromStatus()).isEqualTo(BillingDocumentStatus.AWAITING_EXTERNAL);
            assertThat(vuelta.getToStatus()).isEqualTo(BillingDocumentStatus.EXTERNAL_REGISTERED);
        }
    }

    @Nested
    @DisplayName("Tenancy")
    class Tenancy {

        @Test
        @DisplayName("la empresa viaja en los dos sentidos y no se deriva del documento")
        void la_empresa_viaja_en_los_dos_sentidos() {
            // company_id no es solo una columna mas: es mitad de la FK compuesta contra
            // subscription_billing_documents y el filtro de todas las consultas. Un
            // mapper que la perdiera dejaria la fila sin empresa y la escritura moriria
            // en el motor, o peor, la colgaria de la empresa equivocada.
            BillingDocumentStatusHistory ajeno = new BillingDocumentStatusHistory(7L,
                    BillingDocumentStatusHistoryMother.OTRA_EMPRESA,
                    BillingDocumentStatusHistoryMother.DOCUMENTO, BillingDocumentStatus.DRAFT,
                    BillingDocumentStatus.VOIDED, BillingDocumentStatusHistoryMother.OCURRIO_EL,
                    BillingDocumentStatusHistoryMother.ACTOR_PROCESO, "anulado",
                    BillingDocumentStatusHistoryMother.CREADO_EL);

            BillingDocumentStatusHistoryJpaEntity entidad = mapper.toJpa(ajeno);

            assertThat(entidad.getCompanyId())
                    .isEqualTo(BillingDocumentStatusHistoryMother.OTRA_EMPRESA);
            assertThat(mapper.toDomain(entidad).getCompanyId())
                    .isEqualTo(BillingDocumentStatusHistoryMother.OTRA_EMPRESA);
        }
    }
}
