# Web TV – app per TV Android (TCL / Google TV)

## 1. Imposta il sito
Apri `app/src/main/res/values/strings.xml`:
- `site_url` → l'indirizzo completo, es. `https://www.esempio.it`
- `app_name` → il nome che vedrai sulla TV

Icone opzionali: sostituisci `app/src/main/res/drawable/banner.png` (320×180) e
`app/src/main/res/mipmap-xhdpi/ic_launcher.png` (96×96).

## 2. Crea l'APK
1. Installa Android Studio (gratuito) e apri la cartella `TVWebApp`.
2. Attendi la sincronizzazione Gradle (la prima volta scarica i componenti).
3. Menu **Build > Build App Bundle(s) / APK(s) > Build APK(s)**.
4. L'APK si trova in `app/build/outputs/apk/debug/app-debug.apk`.

## 3. Installa sulla TV TCL
1. Sulla TV: Impostazioni > Sistema > Info > premi 7 volte su "Build" per attivare le Opzioni sviluppatore.
2. Installa l'app **Downloader** (o **Send Files to TV**) dal Play Store e consenti
   l'installazione da origini sconosciute per quell'app.
3. Carica l'APK su Google Drive / un link e aprilo con Downloader, oppure invialo con Send Files to TV.
4. L'app compare nelle app installate e in **Projectivy** (lì puoi metterla tra i preferiti e cambiarle il banner).

## Comandi del telecomando
- **Frecce**: muovono il puntatore; al bordo la pagina scorre
- **OK**: clic
- **Indietro**: pagina precedente, poi esce
- **0 / Info**: attiva o disattiva il puntatore
- **Menu** (o OK tenuto premuto 2 secondi): cambia il sito
- I video a schermo intero si chiudono con Indietro
