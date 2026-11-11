"""Build an isolated Android store-capture app, then restore all production files.

App id: com.mugglelab.damharu.storepreview. Existing app data is never overwritten.
The replacement camera feeds a photo to real commonMain CameraScreen controls.
"""
from pathlib import Path
import shutil
import subprocess

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[1]
gradle = ROOT / 'androidApp/build.gradle.kts'
main = ROOT / 'androidApp/src/main/kotlin/com/mugglelab/damharu/android/MainActivity.kt'
camera = main.with_name('AndroidCamera.kt')
test = ROOT / 'androidApp/src/androidTest/kotlin/com/mugglelab/damharu/android/StoreScreenshotTest.kt'
assets = ROOT / 'androidApp/src/debug/assets'
originals = {p: p.read_bytes() for p in [gradle, main, camera]}
assert not test.exists()
assert not (assets / 'store-diary').exists() and not (assets / 'store-preview.jpg').exists()
try:
    gradle.write_text(gradle.read_text().replace('applicationId = "com.mugglelab.damharu"', 'applicationId = "com.mugglelab.damharu.storepreview"'))
    old_date = 'Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date'
    assert old_date in main.read_text()
    main.write_text(main.read_text().replace(old_date, 'kotlinx.datetime.LocalDate(2026, 10, 23)'))
    camera.write_bytes((HERE / 'fixtures/AndroidCamera.kt').read_bytes())
    shutil.copy2(HERE / 'fixtures/StoreScreenshotTest.kt', test)
    assets.mkdir(parents=True, exist_ok=True)
    shutil.copytree(ROOT / 'design/app-store/home/sample-data/diary', assets / 'store-diary')
    photo = ROOT / 'design/app-store/screens/assets/store-preview.jpg'
    shutil.copy2(photo, assets / 'store-preview.jpg')
    shutil.copy2(photo, assets / 'store-diary/2026-10-01-store-photo.jpg')
    subprocess.run(['./scripts/gradle.sh', ':androidApp:assembleDebug', ':androidApp:assembleDebugAndroidTest'], cwd=ROOT, check=True)
    dest = ROOT / 'build/play-store'
    shutil.copy2(ROOT / 'androidApp/build/outputs/apk/debug/androidApp-debug.apk', dest / 'store-preview.apk')
    shutil.copy2(ROOT / 'androidApp/build/outputs/apk/androidTest/debug/androidApp-debug-androidTest.apk', dest / 'store-preview-test.apk')
finally:
    for path, contents in originals.items():
        path.write_bytes(contents)
    test.unlink(missing_ok=True)
    shutil.rmtree(assets / 'store-diary', ignore_errors=True)
    (assets / 'store-preview.jpg').unlink(missing_ok=True)
    print('Restored Android app sources and removed temporary capture fixtures.', flush=True)
