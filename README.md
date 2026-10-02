# Axie Offline Android

Juego de criaturas coleccionables **100% offline** para Android 5.0+ (API 21), escrito en **Java nativo** con `SurfaceView` + `Canvas`.

## Composición por partes

Cada criatura se construye superponiendo **7 capas PNG** (Espalda, Cola, Cuerpo, Boca, Cuerno, Orejas, Ojos) generadas por código con Pillow. Los genes dominantes de cada slot determinan la variante (12 por clase × 9 clases).

```bash
pip install Pillow
python tools/generate_parts.py   # ~657 PNGs en app/src/main/res/drawable/
```

## Mecánicas

- **9 clases** con ciclo RPS ±15% y bonus misma clase
- **Genes D/R1/R2** por parte + pureza genética
- **Batalla Classic / Origins**: energía, Rage/Fury, Leaf, Bubble, críticos, IA
- **Crianza**: herencia 37.5%/9.375%/3.125%, límite 7, Mystic 7%, costes 900–15300
- **Shards**, AXP, guardado local (SharedPreferences)

## Compilar APK

El workflow de Actions:
1. Genera las partes con Python/Pillow
2. Compila con Gradle (`assembleDebug`)
3. Sube el artifact **axie-offline-debug**

https://github.com/luiseilerys/axie-offline-android/actions

```bash
python tools/generate_parts.py
gradle assembleDebug
```

## Requisitos

- minSdk **21** · targetSdk **34**
- Sin Internet, sin blockchain, sin WebView
