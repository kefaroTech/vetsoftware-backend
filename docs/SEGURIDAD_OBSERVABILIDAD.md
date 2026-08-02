# Seguridad del stack de observabilidad

**Hallazgo que cierra:** OBS-025 — exposición de servicios y credenciales débiles por defecto
**Implementación:** `docker-compose.yml`, `docker-compose.secure.yml`, `docker-compose.debug.yml`,
`docker/otel-collector.yml`, `docker/otel-collector-secure.yml`, `docker/secure/generate-material.sh`

## 1. El problema

El stack local publicaba el receptor OTLP, Loki, Prometheus, Grafana, MySQL, Redis, SonarQube y
LocalStack en **todas las interfaces** del host, sin TLS ni credencial en el transporte de telemetría,
y Grafana arrancaba con `admin/admin` cuando `GRAFANA_PASSWORD` no estaba definida.

En una máquina de escritorio aislada eso no se nota. En una máquina alcanzable desde otra red —una
oficina, una VPN, un portátil en una red compartida— cualquiera con ruta al host podía:

- empujar telemetría falsa por OTLP y contaminar dashboards, SLO y alertas;
- leer los logs de la aplicación en Loki, que contienen datos de operación de clínicas;
- entrar a Grafana con la credencial por omisión y desde ahí consultar las tres fuentes de datos.

## 2. Tres decisiones

### Loopback por omisión

Todo puerto publicado se enlaza a `${LOCAL_BIND_HOST}`, cuyo valor por omisión es `127.0.0.1`. No hay
un solo `- "puerto:puerto"` suelto en el compose. Exponerlo a la red exige escribir
`LOCAL_BIND_HOST=0.0.0.0`, lo cual es una decisión visible en el archivo de entorno y no un descuido.

| Servicio | Publicado en el host | Por qué |
|---|---|---|
| Receptor OTLP (4317/4318) | sí, loopback | el backend corre en el host |
| Grafana (3000) | sí, loopback | interfaz de consulta |
| Prometheus (9090) | sí, loopback | inspección de targets y reglas |
| Alertmanager (9093), Mailpit (8025) | sí, loopback | verificar alertas y correos |
| MySQL (3306), Redis (6379), LocalStack (4566) | sí, loopback | dependencias del backend del host |
| SonarQube (9000) | sí, loopback | herramienta de desarrollo |
| **Loki (3100)** | **no** | se consulta por Grafana |
| **Tempo (3200)** | **no** | se consulta por Grafana |

### Red interna para el plano de telemetría

Loki y Tempo viven solo en la red `vetsoftware_telemetry`, declarada `internal: true`. Docker no le
instala ruta hacia el exterior: esos contenedores no salen a Internet ni reciben tráfico de fuera del
host, y **publicar un puerto sobre una red `internal` no funciona** — se verificó: el `curl` desde el
host a un puerto publicado sobre una red interna no conecta. Es decir, la red interna no es una capa
decorativa sobre el binding a loopback; es un control distinto.

```
host ── 127.0.0.1:4318 ──▶ ┌───────────────┐
                           │ otel-collector│──┐
                           └───────────────┘  │  red telemetry (internal)
                                              ├──▶ loki
host ── 127.0.0.1:3000 ──▶ ┌───────────────┐  ├──▶ tempo
                           │    grafana    │──┘
                           └───────────────┘
host ── 127.0.0.1:9090 ──▶ prometheus ──── raspa loki/tempo/collector por telemetry
                                      └─── raspa el backend por host.docker.internal (red default)
```

El collector, Prometheus y Grafana están en las dos redes porque necesitan ambos lados: recibir o
servir desde el host y hablar con Loki/Tempo. `otel-queue-init` corre con `network_mode: none`: solo
ajusta permisos de un volumen y no necesita red en absoluto.

Consecuencia práctica: para consultar Loki o Tempo con `curl`/`logcli` desde el host hay que usar el
override de depuración.

```bash
docker compose -f docker-compose.yml -f docker-compose.debug.yml --env-file .env.local up -d
```

`docker/tests/resilience.sh` **no** lo necesita: alcanza ambos servicios desde un contenedor `curl`
conectado a la red interna.

### Secretos sin valor predeterminado

`GRAFANA_PASSWORD` ya no tiene fallback. Si falta, `docker compose up` aborta con el mensaje que
explica qué definir, en lugar de levantar una interfaz con `admin/admin`:

```
error while interpolating services.grafana.environment.GF_SECURITY_ADMIN_PASSWORD:
required variable GRAFANA_PASSWORD is missing a value: defina GRAFANA_PASSWORD ...
```

Grafana además queda con anónimo deshabilitado, sin auto-registro, cookies `SameSite=strict` y sin
telemetría hacia grafana.com. Loki arranca con `-reporting.enabled=false` por lo mismo.

**Trampa de Grafana que hay que conocer:** `GF_SECURITY_ADMIN_PASSWORD` solo se aplica cuando Grafana
**crea** el usuario admin, es decir en la primera inicialización del volumen `vetsoftware_grafana_data`.
Sobre un volumen que ya existía, la variable se ignora y la contraseña anterior sigue vigente —así que
un stack que venía de `admin/admin` lo conserva aunque el compose ya exija el secreto. Para rotarla:

```bash
docker compose exec grafana \
  grafana cli --homepath /usr/share/grafana admin reset-admin-password "$GRAFANA_PASSWORD"
```

Se ejecutó en este stack: `admin/admin` ya devuelve 401 y la credencial vigente es la de `.env.local`.
Al probarlo, cuidado con la protección de fuerza bruta: cinco intentos fallidos bloquean el usuario
unos cinco minutos y el error que devuelve es el mismo `Invalid username or password`, lo que hace
parecer que el reseteo falló cuando en realidad funcionó.

Las credenciales locales de MySQL **sí conservan** un valor predeterminado, y es deliberado: MySQL
solo las aplica al inicializar el volumen de datos, así que exigir un secreto nuevo sobre un volumen
ya creado no cambiaría la contraseña real —seguiría siendo la vieja— y solo rompería el arranque. El
control efectivo para MySQL y Redis es que su puerto no sale del host. En `dev` y `prod` no existe el
problema: no hay MySQL ni Redis en compose, son servicios administrados con credenciales del gestor de
secretos.

## 3. El endpoint OTLP

El receptor escucha en `0.0.0.0` **dentro del contenedor**, porque el tráfico entra por una interfaz
del contenedor y no por su loopback. Lo que recorta el origen es el `ports:` del compose: solo
`127.0.0.1` del host, más los contenedores del stack. Además:

- `max_recv_msg_size_mib: 8` en gRPC y `max_request_body_size: 8388608` en HTTP acotan lo que un
  emisor puede empujar en una sola llamada;
- `max_concurrent_streams: 32` acota los streams gRPC simultáneos;
- no se declara `cors`, así que ninguna página web puede empujar telemetría al receptor desde un
  navegador.

## 4. Ambientes compartidos: TLS y autenticación

Loopback no sirve cuando el emisor está en otra máquina. Para eso está el override, que exige
certificado y credencial en el receptor OTLP:

```bash
bash docker/secure/generate-material.sh backend        # material de laboratorio
docker compose -f docker-compose.yml -f docker-compose.secure.yml --env-file .env.local up -d
```

Qué cambia:

| Componente | Base | Con `docker-compose.secure.yml` |
|---|---|---|
| Receptor OTLP | sin TLS, sin credencial | TLS ≥1.2 + Basic auth (`basicauth` sobre htpasswd bcrypt) |
| Grafana | HTTP, contraseña obligatoria | HTTPS, cookies `Secure`, HSTS, `GF_SERVER_ROOT_URL` obligatoria |
| Prometheus, Alertmanager, Mailpit | publicados en loopback | sin puerto publicado (no tienen autenticación propia) |

El emisor necesita entonces endpoint `https`, la cabecera de autorización y confiar en la CA:

```properties
OTEL_EXPORTER_OTLP_TRACES_ENDPOINT=https://<host>:4318/v1/traces
OTEL_EXPORTER_OTLP_LOGS_ENDPOINT=https://<host>:4318/v1/logs
OTEL_EXPORTER_OTLP_HEADERS=Authorization=Basic%20<base64 de usuario:contraseña>
OTEL_EXPORTER_OTLP_CERTIFICATE=<ruta>/ca.crt
```

`generate-material.sh` imprime la cabecera ya armada. El material queda en
`docker/secure/material/`, ignorado por git. **Es material de laboratorio**: una CA propia sirve para
probar el camino y para una red interna, no para un servicio expuesto a terceros, donde el
certificado debe venir de una CA que los clientes ya confíen y la credencial del gestor de secretos.

Comprobado sobre el collector 0.153.0 con este material:

| Intento | Resultado |
|---|---|
| `POST https://…/v1/logs` sin credencial | `401` |
| `POST https://…/v1/logs` con credencial incorrecta | `401` |
| `POST https://…/v1/logs` con credencial correcta y la CA | `200` |
| `POST http://…/v1/logs` (texto claro contra el puerto TLS) | `400`, la telemetría no entra |
| `POST https://…` sin confiar en la CA | falla la verificación del certificado (`curl` 60) |

En Windows, `curl` usa schannel y falla con «the revocation status is unknown» ante una CA propia;
añada `--ssl-revoke-best-effort` al probar. No es un problema del stack.

## 5. Producción

Producción no levanta este compose. El backend envía OTLP directo a Grafana Cloud sobre HTTPS con
`Authorization` en `OTEL_EXPORTER_OTLP_HEADERS`, así que el transporte ya va cifrado y autenticado por
el proveedor (`docs/OBSERVABILIDAD_PROD_GRAFANA_S3.md`). El override de ambiente compartido cubre el
caso intermedio: un stack de compose que no es de un solo desarrollador.

## 6. Qué sigue abierto

- La interfaz de Prometheus y la de Alertmanager no tienen autenticación propia en ninguna variante;
  el override las deja sin publicar en lugar de protegerlas. Ponerlas detrás de un proxy con
  autenticación es trabajo pendiente si alguna vez hacen falta en un ambiente compartido.
- El tráfico entre contenedores dentro de `telemetry` va en texto claro. Es una red interna del
  demonio Docker en un solo host; cifrarla exigiría mTLS entre todos los componentes y no aporta
  frente al modelo de amenaza actual.
