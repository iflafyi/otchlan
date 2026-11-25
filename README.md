# Otchłań — Kroniki Popiołu

3D ARPG w stylu Diablo na Androida.

## Instalacja
1. Pobierz **[OtchlanLauncher.apk](https://raw.githubusercontent.com/iflafyi/otchlan/main/OtchlanLauncher.apk)** i zainstaluj.
2. Otwórz launcher → **Pobierz grę**.
3. Gdy wyjdzie nowa wersja, launcher pokaże **Aktualizuj**. Postęp gry zostaje zachowany.

## Jak to działa (dla autora)
- `version.json` — aktualna wersja gry i launchera, wieści, lista zmian. Launcher czyta go z gałęzi `main`.
- `releases/` — APK gry, `launcher/` — APK launchera, `launcher-src/` — kod launchera.
- Nowa wersja gry: zbuduj APK (ten sam klucz `otchlan.keystore`, większy `versionCode`), potem
  `python3 tools/publish.py game Otchlan.apk 1.3 4 "Zmiana 1" "Zmiana 2"` i `git push`.
