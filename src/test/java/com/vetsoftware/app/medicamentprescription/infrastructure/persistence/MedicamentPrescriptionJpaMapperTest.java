package com.vetsoftware.app.medicamentprescription.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.medicament.infrastructure.persistence.MedicamentJpaEntity;
import com.vetsoftware.app.medicamentprescription.domain.MedicamentPrescription;
import com.vetsoftware.app.medicamentprescription.domain.MedicamentRef;
import com.vetsoftware.app.medicamentprescription.domain.PrescriptionRef;
import com.vetsoftware.app.medicamentprescription.testsupport.MedicamentPrescriptionMother;
import com.vetsoftware.app.prescription.infrastructure.persistence.PrescriptionJpaEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * El mapper es el unico punto que conoce dominio y entidad JPA a la vez, asi
 * que un campo cruzado aqui no lo detecta ninguna otra capa: compila, persiste
 * y solo se ve en pantalla.
 *
 * <p>
 * Las entidades JPA de otras features ({@code PrescriptionJpaEntity},
 * {@code MedicamentJpaEntity}) se mockean porque su constructor sin argumentos
 * es {@code protected} y no son instanciables desde este paquete. No tienen
 * logica: son portadores de datos, y mockearlas no oculta comportamiento.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MedicamentPrescriptionJpaMapper")
class MedicamentPrescriptionJpaMapperTest {

    private final MedicamentPrescriptionJpaMapper mapper = new MedicamentPrescriptionJpaMapper();

    @Mock
    private PrescriptionJpaEntity prescriptionEntity;
    @Mock
    private MedicamentJpaEntity medicamentEntity;

    private MedicamentPrescriptionJpaEntity entidadCompleta() {
        MedicamentPrescriptionJpaEntity entity = new MedicamentPrescriptionJpaEntity();
        entity.setId(MedicamentPrescriptionMother.ID);
        entity.setName(MedicamentPrescriptionMother.MEDICAMENTO.name());
        entity.setPresentation("Tableta");
        entity.setQuantity(2.0);
        entity.setPosology("Cada 12 horas por 7 dias");
        entity.setObservation("Con alimento");
        entity.setCreatedDate(MedicamentPrescriptionMother.CREADO);
        entity.setEnabled(true);
        return entity;
    }

    @Nested
    @DisplayName("toJpa — dominio a entidad")
    class ToJpa {

        @Test
        @DisplayName("copia cada campo escalar en su columna, incluido el snapshot del nombre")
        void copia_cada_campo_escalar_en_su_columna() {
            MedicamentPrescription line = MedicamentPrescriptionMother.persistida();

            MedicamentPrescriptionJpaEntity entity = mapper.toJpa(line, prescriptionEntity,
                    medicamentEntity);

            assertThat(entity.getId()).isEqualTo(MedicamentPrescriptionMother.ID);
            assertThat(entity.getName()).isEqualTo(MedicamentPrescriptionMother.MEDICAMENTO.name());
            assertThat(entity.getPresentation()).isEqualTo("Tableta");
            assertThat(entity.getQuantity()).isEqualTo(2.0);
            assertThat(entity.getPosology()).isEqualTo("Cada 12 horas por 7 dias");
            assertThat(entity.getObservation()).isEqualTo("Con alimento");
            assertThat(entity.getCreatedDate()).isEqualTo(MedicamentPrescriptionMother.CREADO);
            assertThat(entity.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("engancha las asociaciones de prescripcion y medicamento en su slot")
        void engancha_las_asociaciones_en_su_slot() {
            MedicamentPrescriptionJpaEntity entity = mapper.toJpa(
                    MedicamentPrescriptionMother.persistida(), prescriptionEntity,
                    medicamentEntity);

            assertThat(entity.getPrescription()).isSameAs(prescriptionEntity);
            assertThat(entity.getMedicament()).isSameAs(medicamentEntity);
        }

        @Test
        @DisplayName("una linea deshabilitada conserva enabled=false")
        void una_linea_deshabilitada_conserva_enabled_false() {
            MedicamentPrescriptionJpaEntity entity = mapper.toJpa(
                    MedicamentPrescriptionMother.deshabilitada(), prescriptionEntity,
                    medicamentEntity);

            assertThat(entity.isEnabled()).isFalse();
        }
    }

    @Nested
    @DisplayName("toDomain(entity) — camino de lectura, sin inicializar proxies LAZY")
    class ToDomainDesdeAsociaciones {

        @Test
        @DisplayName("el id del medicamento sale de la asociacion, el nombre del snapshot de la fila")
        void el_id_sale_de_la_asociacion_el_nombre_del_snapshot() {
            when(medicamentEntity.getId())
                    .thenReturn(MedicamentPrescriptionMother.OTRO_MEDICAMENT_ID);
            when(prescriptionEntity.getId())
                    .thenReturn(MedicamentPrescriptionMother.PRESCRIPTION_ID);
            when(prescriptionEntity.getDate()).thenReturn(MedicamentPrescriptionMother.FECHA);

            MedicamentPrescriptionJpaEntity entity = entidadCompleta();
            entity.setMedicament(medicamentEntity);
            entity.setPrescription(prescriptionEntity);

            MedicamentPrescription line = mapper.toDomain(entity);

            // El nombre viene de entity.getName() (el snapshot), no de
            // medicamentEntity.getName(): si el catalogo renombra el medicamento despues,
            // la receta ya emitida no debe cambiar retroactivamente.
            assertThat(line.getMedicament())
                    .isEqualTo(new MedicamentRef(MedicamentPrescriptionMother.OTRO_MEDICAMENT_ID,
                            MedicamentPrescriptionMother.MEDICAMENTO.name()));
            assertThat(line.getPrescription())
                    .isEqualTo(new PrescriptionRef(MedicamentPrescriptionMother.PRESCRIPTION_ID,
                            MedicamentPrescriptionMother.FECHA));
        }
    }

    @Nested
    @DisplayName("toDomain(entity, refs) — camino de escritura, no toca las asociaciones")
    class ToDomainConRefs {

        @Test
        @DisplayName("reconstruye el agregado sin leer las asociaciones JPA")
        void reconstruye_el_agregado_sin_leer_las_asociaciones() {
            MedicamentPrescriptionJpaEntity entity = entidadCompleta();

            MedicamentPrescription line = mapper.toDomain(entity,
                    MedicamentPrescriptionMother.RECETA, MedicamentPrescriptionMother.MEDICAMENTO);

            assertThat(line.getId()).isEqualTo(MedicamentPrescriptionMother.ID);
            assertThat(line.getMedicament()).isEqualTo(MedicamentPrescriptionMother.MEDICAMENTO);
            assertThat(line.getPrescription()).isEqualTo(MedicamentPrescriptionMother.RECETA);
            assertThat(line.getPresentation()).isEqualTo("Tableta");
            assertThat(line.getCreatedDate()).isEqualTo(MedicamentPrescriptionMother.CREADO);
            assertThat(line.isEnabled()).isTrue();
            // Este overload existe para no inicializar los proxies de getReferenceById:
            // si leyera entity.getMedicament()/getPrescription(), Hibernate lanzaria un
            // SELECT extra por save.
            verifyNoInteractions(prescriptionEntity, medicamentEntity);
        }

        @Test
        @DisplayName("la ida y vuelta dominio -> entidad -> dominio no pierde nada")
        void la_ida_y_vuelta_no_pierde_nada() {
            MedicamentPrescription original = MedicamentPrescriptionMother.persistida();

            MedicamentPrescriptionJpaEntity entity = mapper.toJpa(original, prescriptionEntity,
                    medicamentEntity);
            MedicamentPrescription vuelta = mapper.toDomain(entity, original.getPrescription(),
                    original.getMedicament());

            assertThat(vuelta).usingRecursiveComparison().isEqualTo(original);
        }
    }
}
