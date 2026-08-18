package com.vetsoftware.app.publishadminpermissions.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.membership.infrastructure.persistence.MembershipJpaEntity;
import com.vetsoftware.app.membershipsubmodule.infrastructure.persistence.MembershipSubModuleJpaEntity;
import com.vetsoftware.app.membershipsubmodule.infrastructure.persistence.MembershipSubModuleJpaRepository;
import com.vetsoftware.app.submodule.infrastructure.persistence.SubModuleJpaEntity;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Las entidades JPA se mockean porque sus constructores sin argumentos son
 * {@code protected} y no son instanciables desde este paquete. No tienen logica
 * propia: son portadoras de datos.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("JpaMembershipSubModuleIdsQueryPort — agrupacion de submodulos por membresia")
class JpaMembershipSubModuleIdsQueryPortTest {

    @Mock
    private MembershipSubModuleJpaRepository membershipSubModuleJpaRepository;
    @Mock
    private MembershipSubModuleJpaEntity enlaceUno;
    @Mock
    private MembershipSubModuleJpaEntity enlaceDos;
    @Mock
    private MembershipSubModuleJpaEntity enlaceTres;
    @Mock
    private MembershipJpaEntity membresiaA;
    @Mock
    private MembershipJpaEntity membresiaB;
    @Mock
    private SubModuleJpaEntity subModuloUno;
    @Mock
    private SubModuleJpaEntity subModuloDos;
    @Mock
    private SubModuleJpaEntity subModuloTres;
    @InjectMocks
    private JpaMembershipSubModuleIdsQueryPort port;

    @Nested
    @DisplayName("agrupamientos vacios")
    class AgrupamientosVacios {

        @Test
        @DisplayName("sin membresias que consultar no toca el repositorio y devuelve mapa vacio")
        void sin_membresias_no_toca_el_repositorio() {
            Map<Long, Set<Long>> resultado = port.findSubModuleIdsByMembershipIds(Set.of());

            assertThat(resultado).isEmpty();
            verifyNoInteractions(membershipSubModuleJpaRepository);
        }

        @Test
        @DisplayName("con membresias pero sin submodulos habilitados devuelve mapa vacio")
        void sin_submodulos_habilitados_devuelve_mapa_vacio() {
            when(membershipSubModuleJpaRepository.findByMembershipIdIn(Set.of(10L)))
                    .thenReturn(List.of());

            Map<Long, Set<Long>> resultado = port.findSubModuleIdsByMembershipIds(Set.of(10L));

            assertThat(resultado).isEmpty();
        }
    }

    @Nested
    @DisplayName("agrupacion")
    class Agrupacion {

        @Test
        @DisplayName("agrupa varios submodulos bajo la misma membresia")
        void agrupa_varios_submodulos_bajo_la_misma_membresia() {
            when(membresiaA.getId()).thenReturn(10L);
            when(subModuloUno.getId()).thenReturn(5L);
            when(subModuloDos.getId()).thenReturn(6L);
            when(enlaceUno.getMembership()).thenReturn(membresiaA);
            when(enlaceUno.getSubModule()).thenReturn(subModuloUno);
            when(enlaceDos.getMembership()).thenReturn(membresiaA);
            when(enlaceDos.getSubModule()).thenReturn(subModuloDos);
            when(membershipSubModuleJpaRepository.findByMembershipIdIn(Set.of(10L)))
                    .thenReturn(List.of(enlaceUno, enlaceDos));

            Map<Long, Set<Long>> resultado = port.findSubModuleIdsByMembershipIds(Set.of(10L));

            assertThat(resultado).containsExactly(Map.entry(10L, Set.of(5L, 6L)));
        }

        @Test
        @DisplayName("separa los submodulos por membresia cuando hay mas de una")
        void separa_los_submodulos_por_membresia() {
            when(membresiaA.getId()).thenReturn(10L);
            when(membresiaB.getId()).thenReturn(20L);
            when(subModuloUno.getId()).thenReturn(5L);
            when(subModuloTres.getId()).thenReturn(7L);
            when(enlaceUno.getMembership()).thenReturn(membresiaA);
            when(enlaceUno.getSubModule()).thenReturn(subModuloUno);
            when(enlaceTres.getMembership()).thenReturn(membresiaB);
            when(enlaceTres.getSubModule()).thenReturn(subModuloTres);
            when(membershipSubModuleJpaRepository.findByMembershipIdIn(Set.of(10L, 20L)))
                    .thenReturn(List.of(enlaceUno, enlaceTres));

            Map<Long, Set<Long>> resultado = port.findSubModuleIdsByMembershipIds(Set.of(10L, 20L));

            assertThat(resultado).containsOnly(Map.entry(10L, Set.of(5L)),
                    Map.entry(20L, Set.of(7L)));
        }
    }
}
