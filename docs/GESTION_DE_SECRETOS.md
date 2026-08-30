# Gestión de secretos

Este repositorio no admite credenciales, tokens, claves privadas ni material criptográfico funcional en Git.

## Desarrollo local

1. Copie `deploy/env/local.env.example` como `.env.local`.
2. Genere valores únicos fuera de Git. Para `DIAN_ENC_KEY`, use 32 bytes aleatorios codificados en base64.
3. Nunca reutilice valores locales en `dev` o `prod`.
4. Copie `src/main/resources/application-local.yml.example` como
   `src/main/resources/application-local.yml` y sustituya los marcadores `REEMPLAZAR_*`.
   La plantilla se versiona; el archivo resultante no.
5. Compruebe con `git check-ignore -v .env.local src/main/resources/application-local.yml`
   que ambos archivos siguen ignorados. `.gitignore` **no surte efecto sobre un archivo
   ya rastreado**: `application-local.yml` estuvo versionado con la contrasena de MySQL
   en claro pese a figurar en `.gitignore`, y viajaba dentro del jar publicado en ECR.

El backend falla al iniciar si `DIAN_ENC_KEY` no está definida. No existe una clave predeterminada en el código.

## Controles obligatorios

- `.husky/pre-commit` ejecuta Gitleaks sobre el contenido staged y bloquea el commit ante cualquier hallazgo.
- `Secret history scan` analiza el historial disponible, contenido codificado y archivos incluidos en cada push o pull request.
- Gitleaks se ejecuta desde una imagen oficial fijada por digest; Docker ausente o detenido bloquea el control local.
- En GitHub, `Secret history scan` debe configurarse como required check y Secret scanning/Push protection deben habilitarse en la organización.

Si una credencial llega a Git, debe revocarse o rotarse primero. Borrar el archivo en un commit posterior no elimina el secreto de los commits anteriores.

## Excepciones aceptadas en el historial

> **Estado: ACEPTADO, NO RESUELTO. Las credenciales NO se han rotado.**
> Decisión del dueño del producto, tomada el **2026-08-30**. Este apartado existe para que
> nadie lea dentro de seis meses el check verde de `Secret history scan` como «esto se
> arregló», porque no se arregló: se decidió convivir con ello.

### Qué se aceptó

Dos credenciales, presentes en el historial de Git desde el arranque del repositorio:

1. La **contraseña local histórica de MySQL** de la familia `cronos20XX`, en `docker-compose.yml`
   y en `application.yml`/`application-local.yml`. La detecta la regla propia
   `vetsoftware-legacy-local-password` de `.gitleaks.toml` — las reglas por defecto de Gitleaks
   no la ven: no es un token de proveedor conocido y su entropía es la de una palabra con un año.
2. Los **valores por defecto embebidos en placeholders de Spring** (`${VAR:valor}`) para
   propiedades sensibles, en `application.yml` y `application-dev.yml`. Los detecta la regla
   propia `spring-inline-secret-default`. El override por entorno da la sensación de estar bien;
   el valor por defecto embebido es una credencial funcional que conoce cualquiera con acceso de
   lectura al repositorio.

Origen: INF-34 / `kefaroTech/vetsoftware-infrastructure#123`. El árbol de trabajo actual ya no
las contiene: `application-local.yml` dejó de versionarse y los defaults se retiraron. Lo que
queda es el **historial**, y el historial solo se limpia reescribiéndolo.

### Qué NO se hizo, y hay que saberlo

- **No se rotaron.** Quien tenga una copia del repositorio —incluido cualquier clon anterior al
  cambio— sigue teniendo esas credenciales, y siguen siendo válidas allí donde no se hayan
  cambiado a mano fuera de Git.
- **No se reescribió el historial.** Ni `filter-repo` ni `filter-branch`. Los commits siguen ahí.
- **No se retiró la excepción de `docker-compose.yml`** de `.gitleaks.toml`, que su propio
  comentario condiciona a la rotación (paso 4 de INF-34). Mientras no se rote, esa excepción
  sigue siendo necesaria.

### Cómo se materializa la aceptación

Con un fichero `.gitleaksignore` en la raíz, **por huella y nunca por ruta ni por regla**. Una
huella es `<sha>:<fichero>:<regla>:<línea>`: identifica un hallazgo en un commit concreto, así
que aceptar estos no puede dejar ciego el escaneo ante una aparición **nueva** del mismo secreto
en el mismo fichero. El gate de pre-commit sigue funcionando igual, y rechazaría estas mismas dos
credenciales si alguien intentara reintroducirlas hoy: sería otro commit, y por tanto otra huella.

Excluir por ruta (`paths`) o por regla habría apagado la detección hacia el futuro. Es la
diferencia entre «acepto estos once hallazgos» y «dejo de mirar aquí», y solo la primera es una
decisión que se pueda revisar después.

### Cuántos son

La aceptación se pidió sobre **once** hallazgos. La ejecución de
`sh scripts/security/scan-secrets.sh history` del 2026-08-30 reporta **17 hallazgos, 15 huellas
distintas** (dos hallazgos salen duplicados en `96697582…` y en `d74dc62d…`, el mismo literal
contado dos veces). `.gitleaksignore` recoge las **15** huellas: aceptar once y dejar cuatro
fuera devolvería el check a rojo permanente, que es exactamente lo que esta decisión existe para
evitar. **La diferencia entre once y quince no está explicada** y conviene que el dueño la
contraste con el recuento del que partió.

### Cómo revisar esta decisión

1. Rotar las dos credenciales fuera de Git.
2. Retirar de `.gitleaks.toml` la excepción de `docker-compose.yml` (paso 4 de INF-34).
3. Borrar `.gitleaksignore` entero. Si el escaneo de historial sigue en verde, la deuda se cerró
   de verdad; si vuelve a rojo, es que quedaba algo que estas huellas estaban tapando.
