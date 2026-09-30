# SEMAlert

Sistema de alerta y asistencia inteligente para Estacionamiento Medido (SEM) en Android.

```
SEMAlert
│
├── 📍 Ubicación
├── 🗺️ Mapa (OpenStreetMap / Leaflet interactivo)
├── 🚗 Estoy estacionado aquí (One-tap parking)
├── 📏 Radio de alerta perimetral (50 m - 1000 m)
├── 🔔 Notificaciones & Registro de eventos
├── ⏱️ Temporizador SEM con alarmas exactas
└── ⚙️ Configuración & Diagnóstico de permisos
```

---

## 🚀 Características Principales

1. **📍 Ubicación**: Telemetría GPS en tiempo real con geocodificación inversa automática para identificar la calle y altura del estacionamiento.
2. **🗺️ Mapa Interactivo**: Integración con **OpenStreetMap** y **Leaflet** embebido en WebView nativo (sin requerir tarjetas de crédito ni API Keys pagas de Google). Muestra posición actual, vehículo estacionado y radio perimetral.
3. **🚗 Estoy estacionado aquí**: Marcado rápido en un toque, cálculo de distancia caminada en tiempo real (*"Estás a 85 m de tu auto"*) y botón de retorno con Google Maps.
4. **📏 Radio de Alerta**: Geocercas dinámicas configurables (50 a 1000 metros) que vigilan el perímetro del vehículo.
5. **🔔 Notificaciones Inteligentes**: Avisos al alejarse del auto, recordatorios previos al vencimiento y notificación persistente en barra de estado.
6. **⏱️ Temporizador SEM**: Cuenta regresiva exacta respaldada por `AlarmManager.setExactAndAllowWhileIdle()` para sonar incluso con la pantalla apagada (Doze Mode).
7. **⚙️ Configuración & Diagnóstico**: Panel de permisos (Ubicación precisa, segundo plano, notificaciones y optimización de batería).

---

## 🛠️ Compilación & Desarrollo

- **IDE**: Android Studio
- **Lenguaje**: Kotlin 2.2 / Java 17
- **SDK**: Android SDK 37 (Min SDK 26 - Android 8.0+)
- **CI/CD**: GitHub Actions automatizado en `.github/workflows/build-apk.yml`

Para compilar desde la consola:
```powershell
cd SEMAlert
./gradlew assembleDebug
```
El archivo APK resultante se generará en:
`SEMAlert/app/build/outputs/apk/debug/app-debug.apk`
