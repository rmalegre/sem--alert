# SEM Alert

Aplicación Android para asistencia y alerta de estacionamiento medido (SEM) independiente, con mapa interactivo OpenStreetMap, geofencing perimetral y temporizador con alarmas.

```
SEMAlert
│
├── 📍 Ubicación
├── 🗺️ Mapa
├── 🚗 Estoy estacionado aquí
├── 📏 Radio de alerta
├── 🔔 Notificaciones
├── ⏱️ Temporizador
└── ⚙️ Configuración
```

---

## 🚀 Módulos Implementados

### 1. 📍 Ubicación
- Integración con `FusedLocationProviderClient` de Google Play Services.
- Geocodificación inversa automática (`Geocoder`) para resolver calles y alturas reales.
- Cálculo de precisión de señal GPS y botón de refresco en tiempo real.

### 2. 🗺️ Mapa
- **OpenStreetMap con Leaflet** embebido mediante WebView nativo bidireccional.
- Funciona sin necesidad de tarjetas de crédito ni API Keys pagas de Google Cloud.
- Marcador de usuario pulsante en azul 📍.
- Marcador de vehículo estacionado en rojo 🚗.
- Círculo dinámico de radio de alerta perimetral 📏.
- Toque interactivo en el mapa para marcar el auto manualmente.

### 3. 🚗 Estoy estacionado aquí
- **One-tap Parking**: Al presionar un botón, guarda las coordenadas, la dirección y la hora actual.
- Cálculo de distancia en vivo: *"🚶 Estás a 85 metros de tu auto"*.
- Acceso directo a navegación *"Cómo volver a mi auto"* (Google Maps / Waze).
- Campo de notas para guardar el número de dársena, piso o parquímetro.
- Botón para liberar el estacionamiento y limpiar las alarmas.

### 4. 📏 Radio de alerta
- Control deslizante interactivo (*SeekBar*) de 50 a 1000 metros.
- Actualización en tiempo real del círculo proyectado en el mapa.
- Sincronización automática con `GeofencingClient` (alertas al alejarse del vehículo).

### 5. 🔔 Notificaciones
- Canales de notificación diferenciados con sonido y vibración.
- Alerta al abandonar el radio de seguridad del vehículo.
- Alerta previa al vencimiento del ticket SEM.
- Notificación persistente con el estado del auto.
- Registro cronológico e historial de avisos.

### 6. ⏱️ Temporizador (Countdown SEM)
- Cuenta regresiva digital con horas, minutos y segundos.
- Botones de adición rápida: `+15 min`, `+30 min`, `+1 h`, `+2 h`.
- Alarma garantizada mediante `AlarmManager.setExactAndAllowWhileIdle()` incluso en modo reposo (Doze Mode).
- Aviso anticipado configurable (5, 10 o 15 minutos antes).

### 7. ⚙️ Configuración & Diagnóstico
- Semáforo de diagnóstico para permisos del sistema:
  - Ubicación precisa (`ACCESS_FINE_LOCATION`).
  - Ubicación todo el tiempo (`ACCESS_BACKGROUND_LOCATION`).
  - Permiso de notificaciones (`POST_NOTIFICATIONS` en Android 13+).
  - Optimización de batería / Doze Mode bypass.
- Parámetros generales y horarios operativos SEM.
- Restablecimiento completo de datos.

---

## 🛠️ Requisitos & Compilación

- **Android Studio** (Koala / Ladybug o superior).
- **JDK 17**.
- **Android SDK 37** (soporta desde Android 8.0 Oreo, API 26 en adelante).
- Dispositivo o emulador con **Google Play Services**.

Abrí la carpeta `SEMAlert` directamente en Android Studio y sincronizá Gradle (`Sync Project with Gradle Files`).
