# TV Kviis — Google TV kest-rakendus

Minimalistlik Android TV/Google TV rakendus, mis avab täisekraanis sinu kviisi TV-vaate veebilehe (WebView'is).

## Automaatne APK ehitus (GitHub Actions)

Iga push main harusse käivitab `.github/workflows/build-apk.yml`, mis ehitab debug APK-i ja avaldab selle GitHub Release'ina.

Uusim APK on alati saadaval sellel lingil (ei muutu kunagi, uueneb iga ehitusega):

```
https://github.com/timotatar-art/tvkviis/releases/latest/download/app-debug.apk
```

Ehituse käiku näeb **Actions** vahekaardilt.

## Mida pead enne päris kasutamist muutma

1. **`app/src/main/java/com/example/tvquiz/MainActivity.kt`** — `tvDisplayUrl` peab osutama päris backendi TV-kuvale (https).
2. **`app/build.gradle.kts`** — `applicationId`/`namespace` oma projektile omaseks.
3. **Ikoon ja banner** on hetkel platseerijad.

## Testimine Google TV seadmel (sideload)

- Laadi alla ülal olevalt stabiilselt lingilt otse TV-sse **Downloader** äpiga, või
- `adb install app-debug.apk` ühendatud seadmele, või
- Android Studios kohalik build ja Run.

## Struktuur

```
tvkviis/
├── .github/workflows/build-apk.yml   ← CI, ehitab ja avaldab APK-i
├── app/
│   ├── build.gradle.kts
│   └── src/main/
│       ├── AndroidManifest.xml       ← LEANBACK_LAUNCHER intent-filter
│       ├── java/.../MainActivity.kt  ← WebView kest
│       └── res/
├── build.gradle.kts
├── settings.gradle.kts
└── gradle.properties
```
