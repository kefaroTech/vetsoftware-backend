package com.vetsoftware.app.numberingresolution.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.numberingresolution.domain.CompanyRef;
import com.vetsoftware.app.numberingresolution.domain.ElectronicDocumentType;
import com.vetsoftware.app.numberingresolution.domain.NumberingResolution;
import com.vetsoftware.app.numberingresolution.testsupport.NumberingResolutionMother;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * El mapper es el unico punto que conoce dominio y entidad JPA a la vez: un
 * campo cruzado aqui no lo detecta ninguna otra capa, ni el dominio (que no
 * conoce la entidad) ni la rodaja de persistencia (que solo mira lo que
 * sobrevive a un ida y vuelta completo, no cada campo por separado).
 *
 * <p>
 * {@code CompanyJpaEntity} se mockea porque su constructor sin argumentos es
 * {@code protected} en otro paquete y no es instanciable desde aqui; no tiene
 * logica propia, es un portador de datos. {@code NumberingResolutionJpaEntity}
 * SI se instancia real: su constructor tambien es {@code protected} pero este
 * test vive en su mismo paquete.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("NumberingResolutionJpaMapper")
class NumberingResolutionJpaMapperTest {

    private final NumberingResolutionJpaMapper mapper = new NumberingResolutionJpaMapper();

    @Mock
    private CompanyJpaEntity companyEntity;

    private NumberingResolutionJpaEntity entidadCompleta() {
        NumberingResolutionJpaEntity entity = new NumberingResolutionJpaEntity();
        entity.setId(NumberingResolutionMother.RESOLUTION_ID);
        entity.setBranchId(NumberingResolutionMother.BRANCH_ID);
        entity.setDocumentType(ElectronicDocumentType.FE_VENTA);
        entity.setResolutionNumber("18760000001");
        entity.setResolutionDate(NumberingResolutionMother.EXPEDIDA);
        entity.setPrefix("SETP");
        entity.setRangeFrom(100L);
        entity.setRangeTo(199L);
        entity.setValidFrom(NumberingResolutionMother.DESDE);
        entity.setValidTo(NumberingResolutionMother.HASTA);
        entity.setTechnicalKey("clave-tecnica");
        entity.setCurrentNumber(100L);
        entity.setCreatedDate(NumberingResolutionMother.CREADA);
        entity.setEnabled(true);
        return entity;
    }

    @Nested
    @DisplayName("toJpa — dominio a entidad")
    class ToJpa {

        @Test
        @DisplayName("copia cada campo escalar en su columna")
        void copia_cada_campo_escalar_en_su_columna() {
            NumberingResolution resolucion = NumberingResolutionMother.activaDeEmpresa();

            NumberingResolutionJpaEntity entity = mapper.toJpa(resolucion, companyEntity);

            assertThat(entity.getId()).isEqualTo(NumberingResolutionMother.RESOLUTION_ID);
            assertThat(entity.getBranchId()).isNull();
            assertThat(entity.getDocumentType()).isEqualTo(ElectronicDocumentType.FE_VENTA);
            assertThat(entity.getResolutionNumber()).isEqualTo("18760000001");
            assertThat(entity.getResolutionDate()).isEqualTo(NumberingResolutionMother.EXPEDIDA);
            assertThat(entity.getPrefix()).isEqualTo("SETP");
            assertThat(entity.getRangeFrom()).isEqualTo(100L);
            assertThat(entity.getRangeTo()).isEqualTo(199L);
            assertThat(entity.getValidFrom()).isEqualTo(NumberingResolutionMother.DESDE);
            assertThat(entity.getValidTo()).isEqualTo(NumberingResolutionMother.HASTA);
            assertThat(entity.getTechnicalKey()).isEqualTo("clave-tecnica");
            assertThat(entity.getCurrentNumber()).isEqualTo(100L);
            assertThat(entity.getCreatedDate()).isEqualTo(NumberingResolutionMother.CREADA);
            assertThat(entity.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("una resolucion de sede copia su branchId; la de empresa lo deja nulo")
        void una_resolucion_de_sede_copia_su_branch_id() {
            NumberingResolutionJpaEntity deSede = mapper
                    .toJpa(NumberingResolutionMother.activaDeSede(), companyEntity);

            assertThat(deSede.getBranchId()).isEqualTo(NumberingResolutionMother.BRANCH_ID);
        }

        @Test
        @DisplayName("engancha la empresa recibida como asociacion, no una copia")
        void engancha_la_empresa_recibida_como_asociacion() {
            NumberingResolutionJpaEntity entity = mapper
                    .toJpa(NumberingResolutionMother.activaDeEmpresa(), companyEntity);

            assertThat(entity.getCompany()).isSameAs(companyEntity);
        }

        @Test
        @DisplayName("una resolucion deshabilitada copia enabled en false")
        void una_resolucion_deshabilitada_copia_enabled_en_false() {
            NumberingResolution deshabilitada = NumberingResolutionMother.activaDeEmpresa();
            deshabilitada.disable();

            NumberingResolutionJpaEntity entity = mapper.toJpa(deshabilitada, companyEntity);

            assertThat(entity.isEnabled()).isFalse();
        }
    }

    @Nested
    @DisplayName("campos criticos DIAN — transporte sin perdida en los valores limite")
    class CamposCriticosDian {

        @Test
        @DisplayName("transporta el consecutivo cuando coincide con el limite superior del rango")
        void transporta_el_consecutivo_en_el_limite_superior_del_rango() {
            NumberingResolution agotadaAlLimite = new NumberingResolution(
                    NumberingResolutionMother.RESOLUTION_ID, NumberingResolutionMother.EMPRESA,
                    ElectronicDocumentType.FE_VENTA, "18760000001",
                    NumberingResolutionMother.EXPEDIDA, "SETP", 100L, 199L,
                    NumberingResolutionMother.DESDE, NumberingResolutionMother.HASTA,
                    "clave-tecnica", 199L, NumberingResolutionMother.CREADA, true, null);

            NumberingResolutionJpaEntity entity = mapper.toJpa(agotadaAlLimite, companyEntity);
            NumberingResolution vuelta = mapper.toDomain(entity, NumberingResolutionMother.EMPRESA);

            assertThat(entity.getCurrentNumber()).isEqualTo(199L);
            assertThat(vuelta.getCurrentNumber()).isEqualTo(agotadaAlLimite.getRangeTo());
        }

        @Test
        @DisplayName("transporta una vigencia ya vencida sin alterar sus fechas")
        void transporta_una_vigencia_ya_vencida_sin_alterarla() {
            LocalDate desdeVencida = LocalDate.of(2020, 1, 1);
            LocalDate hastaVencida = LocalDate.of(2020, 12, 31);
            NumberingResolution vencida = new NumberingResolution(
                    NumberingResolutionMother.RESOLUTION_ID, NumberingResolutionMother.EMPRESA,
                    ElectronicDocumentType.FE_VENTA, "18760000001",
                    NumberingResolutionMother.EXPEDIDA, "SETP", 100L, 199L, desdeVencida,
                    hastaVencida, "clave-tecnica", 100L, NumberingResolutionMother.CREADA, true,
                    null);

            NumberingResolutionJpaEntity entity = mapper.toJpa(vencida, companyEntity);
            NumberingResolution vuelta = mapper.toDomain(entity, NumberingResolutionMother.EMPRESA);

            assertThat(entity.getValidFrom()).isEqualTo(desdeVencida);
            assertThat(entity.getValidTo()).isEqualTo(hastaVencida);
            assertThat(vuelta.getValidFrom()).isEqualTo(desdeVencida);
            assertThat(vuelta.getValidTo()).isEqualTo(hastaVencida);
        }
    }

    @Nested
    @DisplayName("toDomain con CompanyRef dado — camino de escritura")
    class ToDomainConCompanyRefDado {

        @Test
        @DisplayName("reconstruye el agregado sin tocar la asociacion JPA de la empresa")
        void reconstruye_el_agregado_sin_tocar_la_asociacion() {
            // Este overload existe para no inicializar el proxy de getReferenceById: si
            // leyera entity.getCompany(), Hibernate lanzaria un SELECT extra por save.
            NumberingResolution resolucion = mapper.toDomain(entidadCompleta(),
                    NumberingResolutionMother.EMPRESA);

            assertThat(resolucion.getId()).isEqualTo(NumberingResolutionMother.RESOLUTION_ID);
            assertThat(resolucion.getCompany()).isEqualTo(NumberingResolutionMother.EMPRESA);
            assertThat(resolucion.getBranchId()).isEqualTo(NumberingResolutionMother.BRANCH_ID);
            assertThat(resolucion.getDocumentType()).isEqualTo(ElectronicDocumentType.FE_VENTA);
            assertThat(resolucion.getResolutionNumber()).isEqualTo("18760000001");
            assertThat(resolucion.getPrefix()).isEqualTo("SETP");
            assertThat(resolucion.getRangeFrom()).isEqualTo(100L);
            assertThat(resolucion.getRangeTo()).isEqualTo(199L);
            assertThat(resolucion.getCurrentNumber()).isEqualTo(100L);
            assertThat(resolucion.getCreatedDate()).isEqualTo(NumberingResolutionMother.CREADA);
            assertThat(resolucion.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("la ida y vuelta dominio -> entidad -> dominio no pierde nada")
        void la_ida_y_vuelta_no_pierde_nada() {
            NumberingResolution original = NumberingResolutionMother.activaDeSede();

            NumberingResolutionJpaEntity entity = mapper.toJpa(original, companyEntity);
            NumberingResolution vuelta = mapper.toDomain(entity, original.getCompany());

            assertThat(vuelta).usingRecursiveComparison().isEqualTo(original);
        }
    }

    @Nested
    @DisplayName("toDomain desde la asociacion — camino de lectura")
    class ToDomainDesdeAsociacion {

        @Test
        @DisplayName("construye el CompanyRef desde la asociacion cuando la empresa existe")
        void construye_el_company_ref_desde_la_asociacion() {
            when(companyEntity.getId()).thenReturn(NumberingResolutionMother.COMPANY_ID);
            when(companyEntity.getName()).thenReturn("Veterinaria Central");
            when(companyEntity.getIdentifier()).thenReturn("900123456");

            NumberingResolutionJpaEntity entity = entidadCompleta();
            entity.setCompany(companyEntity);

            NumberingResolution resolucion = mapper.toDomain(entity);

            assertThat(resolucion.getCompany()).isEqualTo(new CompanyRef(
                    NumberingResolutionMother.COMPANY_ID, "Veterinaria Central", "900123456"));
        }

        @Test
        @DisplayName("sin empresa asociada, el CompanyRef nulo choca con la validacion del dominio")
        void sin_empresa_asociada_choca_con_la_validacion_del_dominio() {
            // DEFECTO: el mapper evita la NPE de leer c.getId() sobre una asociacion nula
            // (rama company == null del ternario) construyendo un CompanyRef nulo, pero
            // NumberingResolution.validate() exige company no nulo — la rama defensiva
            // del mapper no protege nada, revienta un paso mas adelante. En produccion la
            // columna company_id es NOT NULL asi que hoy no es alcanzable, pero el
            // contrato del mapper miente: promete tolerar una empresa ausente y no lo hace.
            NumberingResolutionJpaEntity entity = entidadCompleta();
            entity.setCompany(null);

            assertThatThrownBy(() -> mapper.toDomain(entity))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("company is required");
        }
    }
}
