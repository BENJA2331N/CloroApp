# CloroApp — Medición de cloro en línea (DSY1105)

App Android (Kotlin + Jetpack Compose) para consultar niveles de cloro libre por granja y punto de
medición, recibir alertas y registrar acciones correctivas. Usa solo datos ficticios.

## Cómo probarla

1. Abrir el proyecto en Android Studio, sincronizar Gradle y ejecutar en un emulador o celular Android.
2. Iniciar sesión (todas las cuentas usan la contraseña `cloro123`):

| Perfil     | Correo                      | Qué puede hacer                                              |
|------------|-----------------------------|--------------------------------------------------------------|
| Operario   | `operario1@cloroapp.cl`     | Ver sus granjas, registrar controles y acciones correctivas  |
| Supervisor | `supervisor1@cloroapp.cl`   | Lo anterior + validar y cerrar alertas, panel, configurar rangos |
| Jefatura   | `jefatura1@cloroapp.cl`     | Vista general, panel, historial y alertas (solo consulta)    |

Hay 15 usuarios: `operario1..9`, `supervisor1..4`, `jefatura1..2`.

## Flujo para demostrar una alerta

1. Como operario/supervisor: pestaña **Registrar** → elegir un punto → ingresar `1,50` ppm → *Registrar control*.
   Se genera una alerta crítica (cloro insuficiente) y una notificación local.
2. En **Alertas** → abrir la alerta → *Registrar acción correctiva*.
3. Registrar una nueva medición del punto dentro de 3,00–5,00 ppm.
4. Como supervisor: abrir la alerta → *Validar y cerrar alerta*.

## Reglas del caso implementadas

- Rangos (respuesta 1): normal 3,00–5,00 ppm · advertencia 2,00–<3,00 y >5,00–6,00 · crítico <2,00 y >6,00.
  Son configurables desde **Perfil** (supervisor y jefatura). Dos decimales, coma decimal.
- Simulación (respuestas 10 y 11): 3 granjas × 4 puntos = 12 puntos, una medición cada 5 minutos,
  historial de 30 días (consultas de 24 h, 7 d y 30 d).
- Modo sin conexión (respuesta 12): los controles manuales se guardan en el teléfono y se envían al
  recuperar la conexión. En **Perfil** hay un interruptor para simular que no hay red.
- Alertas (respuestas 19–26): se generan al detectar una medición fuera de rango, distinguiendo cloro
  insuficiente de excesivo. El cierre exige una acción correctiva y una nueva medición normal, validado por supervisor.

## Recursos nativos integrados (IE 2.4.1)

- **Cámara nativa con FileProvider:** Captura de evidencia fotográfica del fotómetro en mediciones y respaldo de acciones correctivas mediante `ActivityResultContracts.TakePicture()` y `androidx.core.content.FileProvider`. Las fotos se guardan en el almacenamiento interno de la app y sus rutas persisten en SQLite con Room.
- **Notificaciones del sistema:** Canal de alta prioridad para avisos de niveles de cloro fuera de rango con solicitud en tiempo de ejecución de `POST_NOTIFICATIONS` (Android 13+).
- **Vibración háptica:** Patrón de alerta (`Vibrator` / `VibratorManager`) ante mediciones críticas y pulso corto de confirmación al registrar.
- **Compartir reportes:** Integración de `Intent.ACTION_SEND` (Android Sharesheet) para exportar reportes ejecutivos y fichas de alerta a otras aplicaciones.

## Estructura

```
domain/          Reglas puras: rangos, clasificación, formato, roles (probadas con JUnit)
data/            Room (entidades, DAOs), CloroRepository, preferencias (DataStore)
data/remote/     Retrofit + API simulada (ApiSimuladaInterceptor + SimuladorCloro)
notificaciones/  Notificaciones locales de alertas críticas
ui/              Navegación, pantallas Compose y ViewModels
```

## Conectar una API real

En `data/remote/CloroApi.kt` poner `USAR_API_SIMULADA = false` y apuntar `BASE_URL` al servidor
(en el emulador, `http://10.0.2.2:8080/`). Endpoints esperados: `GET api/puntos`,
`GET api/mediciones/ultimas`, `POST api/mediciones`. Para HTTP sin TLS hay que declarar
`android:usesCleartextTraffic="true"` en el manifest.

## Pruebas

`./gradlew test` ejecuta `ClasificadorCloroTest` y `SimuladorCloroTest`.
