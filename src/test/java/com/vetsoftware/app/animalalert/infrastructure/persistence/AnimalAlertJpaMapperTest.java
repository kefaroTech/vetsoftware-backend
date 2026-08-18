package com.vetsoftware.app.animalalert.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.animal.infrastructure.persistence.AnimalJpaEntity;
import com.vetsoftware.app.animalalert.domain.AlertSeverity;
import com.vetsoftware.app.animalalert.domain.AlertType;
import com.vetsoftware.app.animalalert.domain.AnimalAlert;
import com.vetsoftware.app.animalalert.testsupport.AnimalAlertMother;
import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
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
 * Las entidades JPA de otras features ({@code AnimalJpaEntity},
 * {@code CompanyJpaEntity}) se mockean porque su constructor sin argumentos es
 * {@code protected} y no son instanciables desde este paquete.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AnimalAlertJpaMapper")
class AnimalAlertJpaMapperTest {

    private final AnimalAlertJpaMapper mapper = new AnimalAlertJpaMapper();

    @Mock
    private AnimalJpaEntity animalEntity;
    @Mock
    private CompanyJpaEntity companyEntity;

    private AnimalAlertJpaEntity entidadCompleta() {
        AnimalAlertJpaEntity entity = new AnimalAlertJpaEntity();
        entity.setId(AnimalAlertMother.ALERT_ID);
        entity.setType(AlertType.ALLERGY);
        entity.setDescription("Alergia a la penicilina");
        entity.setSeverity(AlertSeverity.HIGH);
        entity.setCreatedDate(AnimalAlertMother.CREADO);
        entity.setEnabled(true);
        return entity;
    }

    @Nested
    @DisplayName("toJpa — dominio a entidad")
    class ToJpa {

        @Test
        @DisplayName("copia cada campo escalar en su columna")
        void copia_cada_campo_escalar_en_su_columna() {
            AnimalAlert alert = AnimalAlertMother.alergia();

            AnimalAlertJpaEntity entity = mapper.toJpa(alert, animalEntity, companyEntity);

            assertThat(entity.getId()).isEqualTo(AnimalAlertMother.ALERT_ID);
            assertThat(entity.getType()).isEqualTo(AlertType.ALLERGY);
            assertThat(entity.getDescription()).isEqualTo("Alergia a la penicilina");
            assertThat(entity.getSeverity()).isEqualTo(AlertSeverity.HIGH);
            assertThat(entity.getCreatedDate()).isEqualTo(AnimalAlertMother.CREADO);
            assertThat(entity.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("engancha cada asociacion en su slot")
        void engancha_cada_asociacion_en_su_slot() {
            AnimalAlertJpaEntity entity = mapper.toJpa(AnimalAlertMother.alergia(), animalEntity,
                    companyEntity);

            assertThat(entity.getAnimal()).isSameAs(animalEntity);
            assertThat(entity.getCompany()).isSameAs(companyEntity);
        }
    }

    @Nested
    @DisplayName("toDomain con refs precargados — camino de escritura")
    class ToDomainConRefs {

        @Test
        @DisplayName("reconstruye el agregado sin tocar las asociaciones JPA")
        void reconstruye_el_agregado_sin_tocar_las_asociaciones() {
            // Este overload existe para no inicializar los proxies de getReferenceById:
            // si leyera entity.getAnimal(), Hibernate lanzaria un SELECT extra por save.
            AnimalAlert alert = mapper.toDomain(entidadCompleta(), AnimalAlertMother.FIRULAIS,
                    AnimalAlertMother.CLINICA);

            assertThat(alert.getId()).isEqualTo(AnimalAlertMother.ALERT_ID);
            assertThat(alert.getAnimal()).isEqualTo(AnimalAlertMother.FIRULAIS);
            assertThat(alert.getCompany()).isEqualTo(AnimalAlertMother.CLINICA);
            assertThat(alert.getType()).isEqualTo(AlertType.ALLERGY);
            assertThat(alert.getDescription()).isEqualTo("Alergia a la penicilina");
            assertThat(alert.getSeverity()).isEqualTo(AlertSeverity.HIGH);
            assertThat(alert.getCreatedDate()).isEqualTo(AnimalAlertMother.CREADO);
            assertThat(alert.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("la ida y vuelta dominio -> entidad -> dominio no pierde nada")
        void la_ida_y_vuelta_no_pierde_nada() {
            AnimalAlert original = AnimalAlertMother.alergia();

            AnimalAlertJpaEntity entity = mapper.toJpa(original, animalEntity, companyEntity);
            AnimalAlert vuelta = mapper.toDomain(entity, original.getAnimal(),
                    original.getCompany());

            assertThat(vuelta).usingRecursiveComparison().isEqualTo(original);
        }
    }

    @Nested
    @DisplayName("toDomain desde las asociaciones — camino de lectura")
    class ToDomainDesdeAsociaciones {

        @Test
        @DisplayName("construye cada companion VO desde su propia asociacion")
        void construye_cada_companion_vo_desde_su_asociacion() {
            when(animalEntity.getId()).thenReturn(AnimalAlertMother.FIRULAIS.id());
            when(animalEntity.getName()).thenReturn(AnimalAlertMother.FIRULAIS.name());
            when(animalEntity.getCode()).thenReturn(AnimalAlertMother.FIRULAIS.code());
            when(companyEntity.getId()).thenReturn(AnimalAlertMother.CLINICA.id());
            when(companyEntity.getName()).thenReturn(AnimalAlertMother.CLINICA.name());
            when(companyEntity.getIdentifier()).thenReturn(AnimalAlertMother.CLINICA.identifier());

            AnimalAlertJpaEntity entity = entidadCompleta();
            entity.setAnimal(animalEntity);
            entity.setCompany(companyEntity);

            AnimalAlert alert = mapper.toDomain(entity);

            assertThat(alert.getAnimal()).isEqualTo(AnimalAlertMother.FIRULAIS);
            assertThat(alert.getCompany()).isEqualTo(AnimalAlertMother.CLINICA);
        }
    }
}
