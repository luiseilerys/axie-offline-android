# Axie Offline Android

Juego de criaturas coleccionables **100% offline** para Android 5.0+ (API 21), escrito en **Java nativo** con `SurfaceView` + `Canvas`. Inspirado en las mecánicas de **Axie Infinity Classic** y **Axie Infinity: Origins**, sin blockchain ni WebView.

## Características

- **9 clases** (Bestia, Planta, Reptil, Acuático, Pájaro, Bicho, Mech, Dawn, Dusk) con ciclo de ventajas/desventajas (+15% / -15%)
- **6 partes corporales** con genes D / R1 / R2 y cartas asociadas
- **Batallas por turnos** en modo Classic y Origins (energía, robo de cartas, Rage/Fury, Leaf, Bubble, etc.)
- **Crianza genética** con probabilidades de herencia, límite 7 crianzas, mutaciones Mystic 7%, coste escalado
- **Economía local** (Shards) + guardado en SharedPreferences
- **Renderizado 2D** puro con Canvas/Paint (formas geométricas, sin imágenes externas)

## Requisitos

- minSdkVersion **21** (Android 5.0)
- targetSdkVersion **34**
- Sin permisos de Internet

## Compilar APK

El workflow de GitHub Actions se ejecuta en cada push a `main`:

1. Abre la pestaña **Actions**
2. Selecciona el workflow **Build APK** más reciente
3. Descarga el artifact **axie-offline-debug**
4. Instala el APK en tu dispositivo

### Local

```bash
./gradlew assembleDebug
```

APK: `app/build/outputs/apk/debug/app-debug.apk`

## Estructura

```
app/src/main/java/com/axieoffline/
  MainActivity.java
  GameView.java
  model/
    Creature.java
    BattleSystem.java
    BreedingSystem.java
    PlayerData.java
    GameState.java
```

## Repositorio

https://github.com/luiseilerys/axie-offline-android
