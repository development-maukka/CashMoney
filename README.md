# CashMoney 0.9.0

Teletext-inspired Android market tracker. Stocks, ETFs, funds and indices from Yahoo Finance; favorites, price changes and dividend yield.

- Finnish, German and English from the primary device language; other languages use English.
- Updates: checked at launch or through Info → Check for app updates. Downloads come from this repository’s latest stable GitHub Release. Android asks for installation approval.
- Package: `fi.cashmoney`; Android 11 or newer. Version code: 17.
- Development Maukka

[Download APK](https://github.com/development-maukka/CashMoney/releases/latest) · [All versions](https://github.com/development-maukka/CashMoney/releases)

## Build

Install Android SDK platform 35 and build-tools 35.0.0, plus JDK 17 or newer. Set `ANDROID_SDK_ROOT` and `CASHMONEY_KEYSTORE` (the original private signing key), then run `bash build-apk.sh`. Signing keys are excluded from this repository and source archives. Preserve the original key for compatible updates.

VT323 font: SIL Open Font License 1.1, included in `licenses/`.
