package com.vetsoftware.app.deworming.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.animal.infrastructure.persistence.AnimalJpaEntity;
import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.consultation.infrastructure.persistence.ConsultationJpaEntity;
import com.vetsoftware.app.deworming.domain.Deworming;
import com.vetsoftware.app.deworming.domain.DewormingType;
import com.vetsoftware.app.deworming.testsupport.DewormingMother;
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
 * Las entidades JPA de otras features (animal, consultation, company) se
 * mockean porque su constructor sin argumentos es {@code protected} y no son
 * instanciables desde este paquete.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DewormingJpaMapper")
class DewormingJpaMapperTest {

    private final DewormingJpaMapper mapper = new DewormingJpaMapper();

    @Mock
    private AnimalJpaEntity animalEntity;
    @Mock
    private ConsultationJpaEntity consultationEntity;
    @Mock
    private CompanyJpaEntity companyEntity;

    private static DewormingJpaEntity entidadCompleta() {
        DewormingJpaEntity entity = new DewormingJpaEntity();
        entity.setId(DewormingMother.DEWORMING_ID);
        entity.setDate(DewormingMother.FECHA);
        entity.setLastDeworming(DewormingMother.FECHA.minusMonths(3));
        entity.setType(DewormingType.INTERNAL);
        entity.setProduct("Drontal Plus");
        entity.setDosage("1 tableta / 10kg");
        entity.setNextControl(DewormingMother.FECHA.plusMonths(3));
        entity.setObservations("Sin reacciones adversas");
        entity.setCreatedDate(DewormingMother.CREADO);
        entity.setEnabled(true);
        return entity;
    }

    @Nested
    @DisplayName("toJpa — dominio a entidad")
    class ToJpa {

        @Test
        @DisplayName("copia cada campo escalar en su columna")
        void copia_cada_campo_escalar_en_su_columna() {
            Deworming deworming = DewormingMother.desparasitacionValida();

            DewormingJpaEntity entity = mapper.toJpa(deworming, animalEntity, consultationEntity,
                    companyEntity);

            assertThat(entity.getId()).isEqualTo(DewormingMother.DEWORMING_ID);
            assertThat(entity.getDate()).isEqualTo(deworming.getDate());
            assertThat(entity.getLastDeworming()).isEqualTo(deworming.getLastDeworming());
            assertThat(entity.getType()).isEqualTo(DewormingType.INTERNAL);
            assertThat(entity.getProduct()).isEqualTo("Drontal Plus");
            assertThat(entity.getDosage()).isEqualTo("1 tableta / 10kg");
            assertThat(entity.getNextControl()).isEqualTo(deworming.getNextControl());
            assertThat(entity.getObservations()).isEqualTo("Sin reacciones adversas");
            assertThat(entity.getCreatedDate()).isEqualTo(deworming.getCreatedDate());
            assertThat(entity.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("engancha cada asociacion en su slot")
        void engancha_cada_asociacion_en_su_slot() {
            DewormingJpaEntity entity = mapper.toJpa(DewormingMother.desparasitacionValida(),
                    animalEntity, consultationEntity, companyEntity);

            assertThat(entity.getAnimal()).isSameAs(animalEntity);
            assertThat(entity.getConsultation()).isSameAs(consultationEntity);
            assertThat(entity.getCompany()).isSameAs(companyEntity);
        }

        @Test
        @DisplayName("una desparasitacion sin consulta asociada guarda consultation null")
        void sin_consulta_asociada_guarda_consultation_null() {
            DewormingJpaEntity entity = mapper.toJpa(DewormingMother.sinConsulta(), animalEntity,
                    null, companyEntity);

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
            // leyera entity.getAnimal(), Hibernate lanzaria un SELECT extra por save.
            Deworming deworming = mapper.toDomain(entidadCompleta(), DewormingMother.FIRULAIS,
                    DewormingMother.CONSULTA, DewormingMother.CLINICA);

            assertThat(deworming.getId()).isEqualTo(DewormingMother.DEWORMING_ID);
            assertThat(deworming.getAnimal()).isEqualTo(DewormingMother.FIRULAIS);
            assertThat(deworming.getConsultation()).isEqualTo(DewormingMother.CONSULTA);
            assertThat(deworming.getCompany()).isEqualTo(DewormingMother.CLINICA);
            assertThat(deworming.getProduct()).isEqualTo("Drontal Plus");
            assertThat(deworming.getCreatedDate()).isEqualTo(DewormingMother.CREADO);
            assertThat(deworming.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("admite consultation null — la desparasitacion sin consulta asociada")
        void admite_consultation_null() {
            Deworming deworming = mapper.toDomain(entidadCompleta(), DewormingMother.FIRULAIS, null,
                    DewormingMother.CLINICA);

            assertThat(deworming.getConsultation()).isNull();
        }

        @Test
        @DisplayName("la ida y vuelta dominio -> entidad -> dominio no pierde nada")
        void la_ida_y_vuelta_no_pierde_nada() {
            Deworming original = DewormingMother.desparasitacionValida();

            DewormingJpaEntity entity = mapper.toJpa(original, animalEntity, consultationEntity,
                    companyEntity);
            Deworming vuelta = mapper.toDomain(entity, original.getAnimal(),
                    original.getConsultation(), original.getCompany());

            assertThat(vuelta).usingRecursiveComparison().isEqualTo(original);
        }
    }

    @Nested
    @DisplayName("toDomain desde las asociaciones — camino de lectura")
    class ToDomainDesdeAsociaciones {

        @Test
        @DisplayName("construye cada companion VO desde su propia asociacion")
        void construye_cada_companion_vo_desde_su_asociacion() {
            when(animalEntity.getId()).thenReturn(DewormingMother.FIRULAIS.id());
            when(animalEntity.getName()).thenReturn(DewormingMother.FIRULAIS.name());
            when(animalEntity.getCode()).thenReturn(DewormingMother.FIRULAIS.code());
            when(consultationEntity.getId()).thenReturn(DewormingMother.CONSULTA.id());
            when(consultationEntity.getDate()).thenReturn(DewormingMother.CONSULTA.date());
            when(companyEntity.getId()).thenReturn(DewormingMother.CLINICA.id());
            when(companyEntity.getName()).thenReturn(DewormingMother.CLINICA.name());
            when(companyEntity.getIdentifier()).thenReturn(DewormingMother.CLINICA.identifier());

            DewormingJpaEntity entity = entidadCompleta();
            entity.setAnimal(animalEntity);
            entity.setConsultation(consultationEntity);
            entity.setCompany(companyEntity);

            Deworming deworming = mapper.toDomain(entity);

            assertThat(deworming.getAnimal()).isEqualTo(DewormingMother.FIRULAIS);
            assertThat(deworming.getConsultation()).isEqualTo(DewormingMother.CONSULTA);
            assertThat(deworming.getCompany()).isEqualTo(DewormingMother.CLINICA);
        }

        @Test
        @DisplayName("sin consulta asociada no construye ConsultationRef")
        void sin_consulta_asociada_no_construye_consultation_ref() {
            when(animalEntity.getId()).thenReturn(DewormingMother.FIRULAIS.id());
            when(animalEntity.getName()).thenReturn(DewormingMother.FIRULAIS.name());
            when(animalEntity.getCode()).thenReturn(DewormingMother.FIRULAIS.code());
            when(companyEntity.getId()).thenReturn(DewormingMother.CLINICA.id());
            when(companyEntity.getName()).thenReturn(DewormingMother.CLINICA.name());
            when(companyEntity.getIdentifier()).thenReturn(DewormingMother.CLINICA.identifier());

            DewormingJpaEntity entity = entidadCompleta();
            entity.setAnimal(animalEntity);
            entity.setConsultation(null);
            entity.setCompany(companyEntity);

            Deworming deworming = mapper.toDomain(entity);

            assertThat(deworming.getConsultation()).isNull();
        }
    }
}
