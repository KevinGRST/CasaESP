# CasaESP

Aplicación Android para controlar una casa inteligente mediante Firebase Realtime Database.

## Requisitos

- Android Studio compatible con AGP 9.2.1
- JDK 11 o superior
- Un proyecto Firebase con Authentication anónima y Realtime Database habilitadas

## Configuración local

1. Descarga el archivo `google-services.json` desde la configuración de tu aplicación Android en Firebase.
2. Colócalo en `app/google-services.json`.
3. No subas ese archivo al repositorio. Está excluido mediante `.gitignore`.
4. Comprueba que las reglas de seguridad de Realtime Database permitan únicamente las operaciones necesarias para tu aplicación.

## Compilar

En Windows:

```powershell
.\gradlew.bat assembleDebug
```

Para ejecutar las pruebas unitarias:

```powershell
.\gradlew.bat test
```

## Estructura principal

- `app/src/main/java/com/example/casaesp`: pantalla y lógica de control.
- `app/src/main/java/com/example/casaesp/ui/theme`: tema visual de Compose.
- `gradle/libs.versions.toml`: versiones y dependencias.
