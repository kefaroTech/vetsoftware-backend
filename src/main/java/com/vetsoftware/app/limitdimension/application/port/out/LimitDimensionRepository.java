package com.vetsoftware.app.limitdimension.application.port.out;

import com.vetsoftware.app.limitdimension.domain.LimitDimension;
import java.util.List;
import java.util.Optional;

/** Adaptador de salida del catálogo de ejes limitables. */
public interface LimitDimensionRepository {

    LimitDimension save(LimitDimension dimension);

    Optional<LimitDimension> findById(Long id);

    Optional<LimitDimension> findByCode(String code);

    boolean existsByCode(String code);

    /**
     * El catálogo entero, ordenado por código.
     *
     * <p>
     * <strong>No se llama {@code findAll}</strong>, y no es capricho: ese nombre
     * está vigilado en todo el árbol porque un {@code findAll()} sin empresa es una
     * fuga entre tenants esperando a ocurrir. Aquí no la hay —la tabla no tiene
     * empresa—, pero el nombre tiene que decirlo en vez de obligar a comprobarlo.
     * No lleva empresa porque la tabla no la tiene, y por eso el caso de uso que lo
     * sirve está cerrado a {@code hasRole('SYSTEM')}.
     */
    List<LimitDimension> findAllOrderedByCode();
}
