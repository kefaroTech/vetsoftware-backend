package com.vetsoftware.app.surgery.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.animal.infrastructure.persistence.AnimalJpaEntity;
import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.consultation.infrastructure.persistence.ConsultationJpaEntity;
import com.vetsoftware.app.surgery.domain.AnimalRef;
import com.vetsoftware.app.surgery.domain.CompanyRef;
import com.vetsoftware.app.surgery.domain.ConsultationRef;
import com.vetsoftware.app.surgery.domain.Surgery;
import com.vetsoftware.app.surgery.domain.SurgeryStatus;
import com.vetsoftware.app.surgery.domain.SurgeryTypeRef;
import com.vetsoftware.app.surgery.testsupport.SurgeryMother;
import com.vetsoftware.app.surgerytype.infrastructure.persistence.SurgeryTypeJpaEntity;
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
 * Las entidades JPA se mockean porque su constructor sin argumentos es
 * {@code protected} y no son instanciables desde este paquete. No tienen
 * logica: son portadores de datos.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SurgeryJpaMapper")
class SurgeryJpaMapperTest {

    private final SurgeryJpaMapper mapper = new SurgeryJpaMapper();

    @Mock
    private SurgeryTypeJpaEntity surgeryTypeEntity;
    @Mock
    private AnimalJpaEntity animalEntity;
    @Mock
    private ConsultationJpaEntity consultationEntity;
    @Mock
    private CompanyJpaEntity companyEntity;

    private SurgeryJpaEntity entidadCompleta() {
        SurgeryJpaEntity entity = new SurgeryJpaEntity();
        entity.setId(SurgeryMother.SURGERY_ID);
        entity.setDate(SurgeryMother.FECHA);
        entity.setDescription("Ovariohisterectomia electiva");
        entity.setMedicament("Ketamina 10mg");
        entity.setObservations("Recuperacion normal");
        entity.setComplications(null);
        entity.setStatus("PROGRAMADA");
        entity.setCreatedDate(SurgeryMother.CREADO);
        entity.setEnabled(true);
        return entity;
    }

    @Nested
    @DisplayName("toJpa — dominio a entidad")
    class ToJpa {

        @Test
        @DisplayName("copia cada campo escalar y el estado como texto")
        void copia_cada_campo_escalar_y_el_estado_como_texto() {
            Surgery surgery = SurgeryMother.cirugiaValida();

            SurgeryJpaEntity entity = mapper.toJpa(surgery, surgeryTypeEntity, animalEntity,
                    consultationEntity, companyEntity);

            assertThat(entity.getId()).isEqualTo(SurgeryMother.SURGERY_ID);
            assertThat(entity.getDate()).isEqualTo(SurgeryMother.FECHA);
            assertThat(entity.getDescription()).isEqualTo("Ovariohisterectomia electiva");
            assertThat(entity.getMedicament()).isEqualTo("Ketamina 10mg");
            assertThat(entity.getObservations()).isEqualTo("Recuperacion normal");
            assertThat(entity.getComplications()).isNull();
            assertThat(entity.getStatus()).isEqualTo("PROGRAMADA");
            assertThat(entity.getCreatedDate()).isEqualTo(SurgeryMother.CREADO);
            assertThat(entity.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("engancha cada asociacion en su slot")
        void engancha_cada_asociacion_en_su_slot() {
            SurgeryJpaEntity entity = mapper.toJpa(SurgeryMother.cirugiaValida(), surgeryTypeEntity,
                    animalEntity, consultationEntity, companyEntity);

            assertThat(entity.getSurgeryType()).isSameAs(surgeryTypeEntity);
            assertThat(entity.getAnimal()).isSameAs(animalEntity);
            assertThat(entity.getConsultation()).isSameAs(consultationEntity);
            assertThat(entity.getCompany()).isSameAs(companyEntity);
        }

        @Test
        @DisplayName("sin consulta asociada la deja en null")
        void sin_consulta_asociada_la_deja_en_null() {
            SurgeryJpaEntity entity = mapper.toJpa(SurgeryMother.cirugiaSinConsulta(),
                    surgeryTypeEntity, animalEntity, null, companyEntity);

            assertThat(entity.getConsultation()).isNull();
        }
    }

    @Nested
    @DisplayName("toDomain con refs precargados — camino de escritura")
    class ToDomainConRefs {

        @Test
        @DisplayName("reconstruye el agregado sin tocar las asociaciones JPA")
        void reconstruye_el_agregado_sin_tocar_las_asociaciones() {
            // Este overload existe para no inicializar los proxies de getReferenceById: si
            // leyera
            // entity.getSurgeryType(), Hibernate lanzaria un SELECT extra en save.
            Surgery surgery = mapper.toDomain(entidadCompleta(), SurgeryMother.OVARIOHISTERECTOMIA,
                    SurgeryMother.FIRULAIS, SurgeryMother.CONSULTA_PREVIA, SurgeryMother.CLINICA);

            assertThat(surgery.getId()).isEqualTo(SurgeryMother.SURGERY_ID);
            assertThat(surgery.getSurgeryType()).isEqualTo(SurgeryMother.OVARIOHISTERECTOMIA);
            assertThat(surgery.getAnimal()).isEqualTo(SurgeryMother.FIRULAIS);
            assertThat(surgery.getConsultation()).isEqualTo(SurgeryMother.CONSULTA_PREVIA);
            assertThat(surgery.getCompany()).isEqualTo(SurgeryMother.CLINICA);
            assertThat(surgery.getStatus()).isEqualTo(SurgeryStatus.PROGRAMADA);
            assertThat(surgery.getCreatedDate()).isEqualTo(SurgeryMother.CREADO);
            assertThat(surgery.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("la ida y vuelta dominio -> entidad -> dominio no pierde nada")
        void la_ida_y_vuelta_no_pierde_nada() {
            Surgery original = SurgeryMother.cirugiaValida();

            SurgeryJpaEntity entity = mapper.toJpa(original, surgeryTypeEntity, animalEntity,
                    consultationEntity, companyEntity);
            Surgery vuelta = mapper.toDomain(entity, original.getSurgeryType(),
                    original.getAnimal(), original.getConsultation(), original.getCompany());

            assertThat(vuelta).usingRecursiveComparison().isEqualTo(original);
        }
    }

    @Nested
    @DisplayName("toDomain desde las asociaciones — camino de lectura")
    class ToDomainDesdeAsociaciones {

        @Test
        @DisplayName("construye cada companion VO desde su propia asociacion")
        void construye_cada_companion_vo_desde_su_asociacion() {
            when(surgeryTypeEntity.getId()).thenReturn(SurgeryMother.OVARIOHISTERECTOMIA.id());
            when(surgeryTypeEntity.getName()).thenReturn(SurgeryMother.OVARIOHISTERECTOMIA.name());
            when(animalEntity.getId()).thenReturn(SurgeryMother.FIRULAIS.id());
            when(animalEntity.getName()).thenReturn(SurgeryMother.FIRULAIS.name());
            when(animalEntity.getCode()).thenReturn(SurgeryMother.FIRULAIS.code());
            when(consultationEntity.getId()).thenReturn(SurgeryMother.CONSULTA_PREVIA.id());
            when(consultationEntity.getDate()).thenReturn(SurgeryMother.CONSULTA_PREVIA.date());
            when(companyEntity.getId()).thenReturn(SurgeryMother.CLINICA.id());
            when(companyEntity.getName()).thenReturn(SurgeryMother.CLINICA.name());
            when(companyEntity.getIdentifier()).thenReturn(SurgeryMother.CLINICA.identifier());

            SurgeryJpaEntity entity = entidadCompleta();
            entity.setSurgeryType(surgeryTypeEntity);
            entity.setAnimal(animalEntity);
            entity.setConsultation(consultationEntity);
            entity.setCompany(companyEntity);

            Surgery surgery = mapper.toDomain(entity);

            assertThat(surgery.getSurgeryType())
                    .isEqualTo(new SurgeryTypeRef(SurgeryMother.OVARIOHISTERECTOMIA.id(),
                            SurgeryMother.OVARIOHISTERECTOMIA.name()));
            assertThat(surgery.getAnimal()).isEqualTo(new AnimalRef(SurgeryMother.FIRULAIS.id(),
                    SurgeryMother.FIRULAIS.name(), SurgeryMother.FIRULAIS.code()));
            assertThat(surgery.getConsultation()).isEqualTo(new ConsultationRef(
                    SurgeryMother.CONSULTA_PREVIA.id(), SurgeryMother.CONSULTA_PREVIA.date()));
            assertThat(surgery.getCompany()).isEqualTo(new CompanyRef(SurgeryMother.CLINICA.id(),
                    SurgeryMother.CLINICA.name(), SurgeryMother.CLINICA.identifier()));
        }

        @Test
        @DisplayName("sin consulta asociada en la entidad, el companion viaja en null")
        void sin_consulta_asociada_en_la_entidad() {
            when(surgeryTypeEntity.getId()).thenReturn(SurgeryMother.OVARIOHISTERECTOMIA.id());
            when(surgeryTypeEntity.getName()).thenReturn(SurgeryMother.OVARIOHISTERECTOMIA.name());
            when(animalEntity.getId()).thenReturn(SurgeryMother.FIRULAIS.id());
            when(animalEntity.getName()).thenReturn(SurgeryMother.FIRULAIS.name());
            when(animalEntity.getCode()).thenReturn(SurgeryMother.FIRULAIS.code());
            when(companyEntity.getId()).thenReturn(SurgeryMother.CLINICA.id());
            when(companyEntity.getName()).thenReturn(SurgeryMother.CLINICA.name());
            when(companyEntity.getIdentifier()).thenReturn(SurgeryMother.CLINICA.identifier());

            SurgeryJpaEntity entity = entidadCompleta();
            entity.setSurgeryType(surgeryTypeEntity);
            entity.setAnimal(animalEntity);
            entity.setConsultation(null);
            entity.setCompany(companyEntity);

            Surgery surgery = mapper.toDomain(entity);

            assertThat(surgery.getConsultation()).isNull();
        }
    }
}
