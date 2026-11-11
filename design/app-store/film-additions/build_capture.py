"""Build a temporary screenshot fixture, restoring all app/test sources afterwards.

Run at repo root. The compiled Debug fixture reads Documents/store-preview.jpg only
when --camera-preview and --store-photo are both present. Release app is untouched.
"""
import subprocess
from pathlib import Path

ROOT = Path(__file__).resolve().parents[3]
app = ROOT / 'iosApp/iosApp/DamharuApp.swift'
tests = ROOT / 'iosApp/iosAppUITests/HomeSmokeTests.swift'
app_source, test_source = app.read_text(), tests.read_text()
start = app_source.index('    private func startDemoFrames() {')
end = app_source.index('\n    #endif', start)
fixture = '''    private func startDemoFrames() {
        let photoURL = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask)[0].appendingPathComponent("store-preview.jpg")
        guard ProcessInfo.processInfo.arguments.contains("--store-photo"),
              let image = UIImage(contentsOfFile: photoURL.path)?.cgImage else {
            report("스토어 샘플 사진을 찾지 못했어요."); return
        }
        var optionalBuffer: CVPixelBuffer?
        let attributes: [String: Any] = [kCVPixelBufferMetalCompatibilityKey as String: true,
                                       kCVPixelBufferIOSurfacePropertiesKey as String: [:]]
        guard CVPixelBufferCreate(kCFAllocatorDefault, 960, 1280, kCVPixelFormatType_32BGRA,
                                  attributes as CFDictionary, &optionalBuffer) == kCVReturnSuccess,
              let buffer = optionalBuffer else { return }
        CIContext().render(CIImage(cgImage: image).transformed(by: CGAffineTransform(
            scaleX: 960.0 / CGFloat(image.width), y: 1280.0 / CGFloat(image.height))), to: buffer)
        state.configure(minimum: 0.5, maximum: 6, initial: 1)
        let timer = DispatchSource.makeTimerSource(queue: frameQueue)
        timer.schedule(deadline: .now(), repeating: 1.0 / 30)
        timer.setEventHandler { [weak self] in self?.preview.offer(buffer) }
        demoTimer = timer; timer.resume()
    }'''
try:
    app.write_text(app_source[:start] + fixture + app_source[end:])
    tests.write_text(test_source + '\n' + (Path(__file__).parent / 'StoreScreenshotTests.swift').read_text().replace('import XCTest\n', '', 1))
    subprocess.run(['xcodebuild', '-project', 'iosApp/Damharu.xcodeproj', '-scheme', 'Damharu',
                    '-destination', 'platform=iOS Simulator,id=C882447A-8047-4F7D-B5E4-1A6DE2FD02B4',
                    '-derivedDataPath', 'build/store-film-additions', 'CODE_SIGNING_ALLOWED=NO',
                    'build-for-testing'], cwd=ROOT, check=True)
finally:
    app.write_text(app_source)
    tests.write_text(test_source)
    print('Restored app and UI-test sources to their pre-capture contents.', flush=True)
