package com.vetsoftware.app.owner.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.city.infrastructure.persistence.CityJpaEntity;
import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.owner.domain.FiscalResponsibility;
import com.vetsoftware.app.owner.domain.Owner;
import com.vetsoftware.app.owner.domain.OwnerDocumentType;
import com.vetsoftware.app.owner.domain.PersonType;
import com.vetsoftware.app.owner.domain.TaxRegime;
import com.vetsoftware.app.owner.testsupport.OwnerMother;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * El mapper es el unico punto que conoce dominio y entidad JPA a la vez, asi
 * que un campo cruzado aqui no lo detecta ninguna otra capa. Las entidades JPA
 * de otras features se mockean porque su constructor sin argumentos es
 * {@code protected} y no son instanciables desde este paquete.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("OwnerJpaMapper")
class OwnerJpaMapperTest {

    private final OwnerJpaMapper mapper = new OwnerJpaMapper();

    @Mock
    private CityJpaEntity cityEntity;
    @Mock
    private CompanyJpaEntity companyEntity;

    private OwnerJpaEntity entidadCompleta() {
        OwnerJpaEntity entity = new OwnerJpaEntity();
        entity.setId(OwnerMother.JURIDICA_ID);
        entity.setName("Veterinaria Sur");
        entity.setEmail("contacto@sur.com");
        entity.setDocument("900123456");
        entity.setDocumentType(OwnerDocumentType.NIT);
        entity.setPersonType(PersonType.JURIDICA);
        entity.setVerificationDigit("7");
        entity.setLegalName("Veterinaria Sur S.A.S.");
        entity.setAddress("Avenida 3 # 40-50");
        entity.setPhone("6041234567");
        entity.setWithholdingAgent(true);
        entity.setTaxRegime(TaxRegime.RESPONSABLE_IVA);
        entity.setFiscalResponsibility(FiscalResponsibility.GRAN_CONTRIBUYENTE);
        entity.setCreatedDate(OwnerMother.CREADO);
        entity.setEnabled(true);
        return entity;
    }

    @Nested
    @DisplayName("toJpa — dominio a entidad")
    class ToJpa {

        @Test
        @DisplayName("copia cada campo escalar en su columna")
        void copia_cada_campo_escalar_en_su_columna() {
            Owner owner = OwnerMother.personaJuridica();

            OwnerJpaEntity entity = mapper.toJpa(owner, cityEntity, companyEntity);

            assertThat(entity.getId()).isEqualTo(OwnerMother.JURIDICA_ID);
            assertThat(entity.getName()).isEqualTo("Veterinaria Sur");
            assertThat(entity.getEmail()).isEqualTo("contacto@sur.com");
            assertThat(entity.getDocument()).isEqualTo("900123456");
            assertThat(entity.getDocumentType()).isEqualTo(OwnerDocumentType.NIT);
            assertThat(entity.getPersonType()).isEqualTo(PersonType.JURIDICA);
            assertThat(entity.getVerificationDigit()).isEqualTo("7");
            assertThat(entity.getLegalName()).isEqualTo("Veterinaria Sur S.A.S.");
            assertThat(entity.getAddress()).isEqualTo("Avenida 3 # 40-50");
            assertThat(entity.getPhone()).isEqualTo("6041234567");
            assertThat(entity.isWithholdingAgent()).isTrue();
            assertThat(entity.getTaxRegime()).isEqualTo(TaxRegime.RESPONSABLE_IVA);
            assertThat(entity.getFiscalResponsibility())
                    .isEqualTo(FiscalResponsibility.GRAN_CONTRIBUYENTE);
            assertThat(entity.getCreatedDate()).isEqualTo(OwnerMother.CREADO);
            assertThat(entity.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("engancha ciudad y compania en su slot")
        void engancha_ciudad_y_compania_en_su_slot() {
            OwnerJpaEntity entity = mapper.toJpa(OwnerMother.personaNatural(), cityEntity,
                    companyEntity);

            assertThat(entity.getCity()).isSameAs(cityEntity);
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
            // si leyera entity.getCity(), Hibernate lanzaria un SELECT extra por save.
            Owner owner = mapper.toDomain(entidadCompleta(), OwnerMother.BOGOTA,
                    OwnerMother.CLINICA);

            assertThat(owner.getId()).isEqualTo(OwnerMother.JURIDICA_ID);
            assertThat(owner.getName()).isEqualTo("Veterinaria Sur");
            assertThat(owner.getDocumentType()).isEqualTo(OwnerDocumentType.NIT);
            assertThat(owner.getPersonType()).isEqualTo(PersonType.JURIDICA);
            assertThat(owner.getVerificationDigit()).isEqualTo("7");
            assertThat(owner.getLegalName()).isEqualTo("Veterinaria Sur S.A.S.");
            assertThat(owner.getCity()).isEqualTo(OwnerMother.BOGOTA);
            assertThat(owner.getCompany()).isEqualTo(OwnerMother.CLINICA);
            assertThat(owner.getCreatedDate()).isEqualTo(OwnerMother.CREADO);
            assertThat(owner.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("la ida y vuelta dominio -> entidad -> dominio no pierde nada")
        void la_ida_y_vuelta_no_pierde_nada() {
            Owner original = OwnerMother.personaJuridica();

            OwnerJpaEntity entity = mapper.toJpa(original, cityEntity, companyEntity);
            Owner vuelta = mapper.toDomain(entity, original.getCity(), original.getCompany());

            assertThat(vuelta).usingRecursiveComparison().isEqualTo(original);
        }
    }

    @Nested
    @DisplayName("toDomain desde las asociaciones — camino de lectura")
    class ToDomainDesdeAsociaciones {

        @Test
        @DisplayName("construye cada companion VO desde su propia asociacion")
        void construye_cada_companion_vo_desde_su_asociacion() {
            when(cityEntity.getId()).thenReturn(OwnerMother.BOGOTA.id());
            when(cityEntity.getName()).thenReturn(OwnerMother.BOGOTA.name());
            when(companyEntity.getId()).thenReturn(OwnerMother.CLINICA.id());
            when(companyEntity.getName()).thenReturn(OwnerMother.CLINICA.name());
            when(companyEntity.getIdentifier()).thenReturn(OwnerMother.CLINICA.identifier());

            OwnerJpaEntity entity = entidadCompleta();
            entity.setCity(cityEntity);
            entity.setCompany(companyEntity);

            Owner owner = mapper.toDomain(entity);

            assertThat(owner.getCity()).isEqualTo(OwnerMother.BOGOTA);
            assertThat(owner.getCompany()).isEqualTo(OwnerMother.CLINICA);
        }
    }
}
