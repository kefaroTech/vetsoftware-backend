package com.vetsoftware.app.companyusageevent.infrastructure.persistence;

import com.vetsoftware.app.companyusageevent.application.port.out.LimitDimensionQueryPort;
import com.vetsoftware.app.companyusageevent.domain.LimitDimensionRef;
import com.vetsoftware.app.limitdimension.infrastructure.persistence.LimitDimensionJpaRepository;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * El unico fichero de esta rodaja que conoce las tablas de
 * {@code limitdimension}. Es el cruce de vertical slicing permitido:
 * {@code infrastructure/persistence} puede importar el {@code XxxJpaRepository}
 * de otra feature; el {@code domain} y el {@code application} de esta no saben
 * que la otra existe.
 *
 * <p>
 * <strong>El nombre de bean es explicito, y conviene saber por que NO.</strong>
 * {@code entitlement} y {@code catalogitemlimit} declaran clases con este mismo
 * nombre simple, pero eso <em>no</em> produce un
 * {@code ConflictingBeanDefinitionException}: {@code VetSoftwareApplication}
 * declara {@code @SpringBootApplication(nameGenerator =
 * FullyQualifiedAnnotationBeanNameGenerator.class)}, asi que el nombre por
 * defecto de cada bean es su clase <b>cualificada</b> y dos homonimas conviven
 * sin chocar. La prueba esta en el propio arbol: hay una veintena de grupos de
 * clases homonimas con anotacion de bean sin nombre —cinco
 * {@code JpaAnimalChildrenQueryPort}, cuatro
 * {@code OpenAccountRefresherAdapter}— funcionando desde hace meses. Si la
 * premisa contraria fuese cierta, este repositorio no arrancaria.
 *
 * <p>
 * Se cualifica por dos motivos mas estrechos: <b>consistencia</b> con las otras
 * homonimas del arbol, y el caso que si muerde —un contexto de test que
 * <b>escanee</b> por nombre simple en vez de importar—. Por eso
 * {@code PersistenceSliceConfig} usa {@code @Import} y no
 * {@code @ComponentScan}: a las clases importadas tambien las nombra el
 * generador cualificado.
 *
 * <p>
 * <strong>No filtra por {@code enabled}, a proposito.</strong> Un eje que se
 * retira del catalogo deja de venderse, pero los contratos que ya lo compraron
 * siguen consumiendo contra el, y la clave foranea {@code fk_cue_dimension}
 * solo exige que la fila exista. Filtrar aqui dejaria de medir —en silencio— el
 * consumo de clientes que lo estan pagando; el dia que haya que impedir
 * <em>nuevas</em> ventas, eso se decide donde se vende, no donde se mide.
 */
@Component("companyUsageEventJpaLimitDimensionQueryPort")
public class JpaLimitDimensionQueryPort implements LimitDimensionQueryPort {

    private final LimitDimensionJpaRepository limitDimensionJpaRepository;

    public JpaLimitDimensionQueryPort(LimitDimensionJpaRepository limitDimensionJpaRepository) {
        this.limitDimensionJpaRepository = limitDimensionJpaRepository;
    }

    @Override
    public Optional<LimitDimensionRef> findByCode(String code) {
        return limitDimensionJpaRepository.findByCode(code)
                .map(entity -> new LimitDimensionRef(entity.getId(), entity.getCode()));
    }
}
