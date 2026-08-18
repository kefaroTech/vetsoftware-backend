package com.vetsoftware.app.supplier.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.supplier.domain.CompanyRef;
import com.vetsoftware.app.supplier.domain.Supplier;
import com.vetsoftware.app.supplier.testsupport.SupplierMother;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * {@link CompanyJpaEntity} pertenece a otra feature y su constructor es
 * {@code protected}: desde aqui se mockea como fila, no como entidad de
 * dominio.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SupplierJpaMapper — ida y vuelta dominio <-> entidad")
class SupplierJpaMapperTest {

    private final SupplierJpaMapper mapper = new SupplierJpaMapper();

    @Mock
    private CompanyJpaEntity companyEntity;

    private static final CompanyRef EMPRESA = new CompanyRef(9L, "Clinica Sur", "900654321");

    @Nested
    @DisplayName("toJpa")
    class ADominioPersistente {

        @Test
        @DisplayName("copia cada campo de un proveedor con todos los opcionales informados")
        void copia_cada_campo_de_un_proveedor_completo() {
            Supplier supplier = SupplierMother.completo("Distribuidora Vet", EMPRESA);

            SupplierJpaEntity entity = mapper.toJpa(supplier, companyEntity);

            assertThat(entity.getId()).isEqualTo(supplier.getId());
            assertThat(entity.getName()).isEqualTo(supplier.getName());
            assertThat(entity.getTaxId()).isEqualTo(supplier.getTaxId());
            assertThat(entity.getContactName()).isEqualTo(supplier.getContactName());
            assertThat(entity.getPhone()).isEqualTo(supplier.getPhone());
            assertThat(entity.getEmail()).isEqualTo(supplier.getEmail());
            assertThat(entity.getAddress()).isEqualTo(supplier.getAddress());
            assertThat(entity.getPaymentTermsDays()).isEqualTo(supplier.getPaymentTermsDays());
            assertThat(entity.getNotes()).isEqualTo(supplier.getNotes());
            assertThat(entity.getCompany()).isSameAs(companyEntity);
            assertThat(entity.getCreatedDate()).isEqualTo(supplier.getCreatedDate());
            assertThat(entity.getUpdatedDate()).isEqualTo(supplier.getUpdatedDate());
            assertThat(entity.getUpdatedBy()).isEqualTo(supplier.getUpdatedBy());
            assertThat(entity.getVersion()).isEqualTo(supplier.getVersion());
            assertThat(entity.isEnabled()).isEqualTo(supplier.isEnabled());
        }

        @Test
        @DisplayName("un proveedor minimo viaja con los campos opcionales en null")
        void un_proveedor_minimo_viaja_con_los_opcionales_en_null() {
            Supplier minimo = SupplierMother.minimo("Insumos Andinos", EMPRESA);

            SupplierJpaEntity entity = mapper.toJpa(minimo, companyEntity);

            assertThat(entity.getTaxId()).isNull();
            assertThat(entity.getContactName()).isNull();
            assertThat(entity.getPhone()).isNull();
            assertThat(entity.getEmail()).isNull();
            assertThat(entity.getAddress()).isNull();
            assertThat(entity.getPaymentTermsDays()).isNull();
            assertThat(entity.getNotes()).isNull();
        }

        @Test
        @DisplayName("un proveedor nuevo viaja sin id para que lo genere la base")
        void un_proveedor_nuevo_viaja_sin_id() {
            SupplierJpaEntity entity = mapper
                    .toJpa(SupplierMother.completo("Distribuidora Vet", EMPRESA), companyEntity);

            assertThat(entity.getId()).isNull();
        }
    }

    @Nested
    @DisplayName("toDomain(entity) — read path, resuelve la company desde la asociacion")
    class ADominioDesdeLaEntidad {

        @Test
        @DisplayName("mapea un proveedor completo con la company resuelta")
        void mapea_un_proveedor_completo_con_la_company_resuelta() {
            when(companyEntity.getId()).thenReturn(EMPRESA.id());
            when(companyEntity.getName()).thenReturn(EMPRESA.name());
            when(companyEntity.getIdentifier()).thenReturn(EMPRESA.identifier());
            SupplierJpaEntity entity = mapper
                    .toJpa(SupplierMother.completo("Distribuidora Vet", EMPRESA), companyEntity);
            entity.setId(501L);

            Supplier domain = mapper.toDomain(entity);

            assertThat(domain.getId()).isEqualTo(501L);
            assertThat(domain.getName()).isEqualTo("Distribuidora Vet");
            assertThat(domain.getCompany()).isEqualTo(EMPRESA);
        }
    }

    @Nested
    @DisplayName("toDomain(entity, ref) — write path, reusa la company ya cargada")
    class ADominioReusandoElRef {

        @Test
        @DisplayName("reusa el CompanyRef recibido sin tocar la asociacion de la entidad")
        void reusa_el_company_ref_recibido() {
            SupplierJpaEntity entity = mapper
                    .toJpa(SupplierMother.completo("Distribuidora Vet", EMPRESA), companyEntity);
            entity.setId(501L);

            Supplier domain = mapper.toDomain(entity, EMPRESA);

            assertThat(domain.getCompany()).isSameAs(EMPRESA);
        }
    }
}
