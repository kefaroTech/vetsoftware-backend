package com.vetsoftware.app.laboratorytestfile.application.port.out;

import com.vetsoftware.app.laboratorytestfile.domain.LaboratoryTestRef;
import com.vetsoftware.app.laboratorytestfile.domain.LaboratoryTestStoragePathRef;
import java.util.Optional;

/**
 * Sin variante ancha a proposito: la unica forma de resolver el examen de
 * laboratorio es acotando por empresa. Con un {@code findById(id)} pelado, un
 * adjunto de esta empresa podia quedar colgado del examen de otro tenant —la
 * carga propia ya acotada no lo impide, porque el defecto no es apropiarse de
 * la fila sino colgarla de un padre ajeno—, y aqui el adjunto es un resultado
 * clinico que despues se descarga por
 * {@code /laboratory-test-files/id/download}.
 *
 * <p>
 * {@code findStoragePath} tambien recibe la empresa, y no por simetria: la ruta
 * que devuelve lleva dentro el id de empresa, el del propietario y el nombre
 * del animal, asi que sin acotar seria una fuga de datos por si misma
 * —revelaria a quien pertenece un examen ajeno aunque la subida acabase
 * fallando— y ademas construiria la clave de S3 bajo el prefijo de la otra
 * empresa.
 */
public interface LaboratoryTestQueryPort {
    Optional<LaboratoryTestRef> findByIdAndCompanyId(Long laboratoryTestId, Long companyId);

    Optional<LaboratoryTestStoragePathRef> findStoragePath(Long laboratoryTestId, Long companyId);
}
