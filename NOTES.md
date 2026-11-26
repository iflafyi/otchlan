# Notatki projektu (dla następnego czatu z Claude)

- Repo `iflafyi/otchlan` = kanał aktualizacji gry Otchłań. Launcher (`ua.dima.otchlan.launcher`) czyta
  `https://raw.githubusercontent.com/iflafyi/otchlan/main/version.json` i pobiera APK z `releases/`.
- Gra: pakiet `ua.dima.otchlan`, aktualnie 2.0 (versionCode 4). Każda nowa wersja: versionCode +1
  i podpis tym samym kluczem co wcześniej (klucz trzymany prywatnie w archiwum źródeł gry, NIE w tym repo).
- Wydanie: `python3 tools/publish.py game <apk> <wersja> <versionCode> "zmiany..."`, opcjonalnie
  `tools/publish.py news "Tytuł" "Tekst"`, potem commit + push na `main`.
- Launcher: kod w `launcher-src/` (UI `assets/launcher.html` po polsku + Java: PackageInstaller, most JS `Launcher`).
  Build: `launcher-src/build_apk.sh <wersja> <code>` (SDK build-tools 35, platform 34),
  potem `tools/publish.py launcher build/OtchlanLauncher.apk <wersja> <code>` — launcher sam zaproponuje aktualizację.
- Pierwsza instalacja: Android prosi o „Zezwalaj z tego źródła” dla launchera (jednorazowo).
