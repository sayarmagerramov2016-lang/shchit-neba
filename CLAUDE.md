# Щит неба 3D — заметки для Claude

Владелец не программист: всё делает Claude. Общение по-русски.
В каждой новой игре сразу делать «чёрный ящик» (журнал вылетов и зависаний), не дожидаясь напоминания.

## Что это
Игра про ПВО на Java/libGDX 1.13.1. Порт старой версии 2.0.4 (Godot 4.3).
Модули: `core` (вся игра), `lwjgl3` (ПК), `android` (APK). Ресурсы: `core/assets` (data.json, шрифты).

## Как начать работу в новом чате
1. Код лежит здесь, в репозитории. Компьютер чата каждый раз чистый.
2. Android SDK ставится заново (~2 мин):
   ```
   S=/opt/android-sdk; mkdir -p $S/cmdline-tools && cd $S/cmdline-tools
   curl -sSLo clt.zip https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip
   unzip -qo clt.zip && mv cmdline-tools latest
   yes | latest/bin/sdkmanager --sdk_root=$S "platforms;android-34" "build-tools;34.0.0" "platform-tools"
   echo "sdk.dir=$S" > <repo>/local.properties
   ```
3. Maven Central иногда отвечает 429 — просто повторить сборку через 20 с.

## Сборка
- APK: `./gradlew :android:assembleRelease` → `android/build/outputs/apk/release/android-release.apk`
  Пакет `ru.shitneba3d`, ключ `android/shitneba.keystore` (пароль shitneba123, alias shitneba).
- ПК: `./gradlew :lwjgl3:dist` → `lwjgl3/build/libs/ShitNeba3D-desktop.jar`.
  Для Windows-архива Java берётся из pip-пакета jdk4py (win_amd64), запуск `ShchitNeba3D.bat`.
- Версия: `G.VER` и строка в `BlackBox.java` + versionCode/versionName в `android/build.gradle`.

## Проверка
- Автотест со скриншотами (Xvfb):
  `xvfb-run -a -s "-screen 0 1280x720x24" java -jar ShitNeba3D-desktop.jar <папка> [машина|-] [camp|strike|duelhost|dueljoin]`
- Чёрный ящик: запуск без папки скриншотов с `SN_TEST=crash` или `SN_TEST=freeze` → `crash.txt`/`freeze.txt`;
  `SN_REPORT=1` показывает отчёт и в режиме скриншотов.

## После работы
- Закоммитить и запушить сюда.
- Обновить список версий ниже и документ проекта.

## Версии
- 3.0.0 — меню «тактический терминал», бесконечный бой, 5 комплексов, гараж, улучшения, задания, достижения, рекорды, ежедневная награда.
- 3.0.1 — окраска «Аметист».
- 3.1.0 — кампания (11×5), 16 испытаний, режим «Рой», дуэль по Wi‑Fi.
- 3.1.1 — ускорение для телефона (пониженное разрешение, авто-FPS), ПК-версия.
- 3.1.2 — «чёрный ящик».
- 18.10.2026 — исходники 3.1.2 восстановлены декомпиляцией ПК-сборки (оригинальные исходники пропали
  вместе с компьютером старого чата). Android-модуль и иконка собраны заново.
  Ключ подписи в репозитории НОВЫЙ: APK с ним не встанет поверх старой 3.1.2 — нужен оригинальный
  `shitneba.keystore` из ShchitNeba3D-Java-source.zip (если найдётся, заменить и закоммитить).
