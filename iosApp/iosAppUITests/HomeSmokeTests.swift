import XCTest
import UIKit

/// Records are created only through the in-app camera and only for "today". `--date` pins today and
/// `--camera-preview` feeds Debug-only fake frames, so these runs never exercise a real camera sensor.
final class HomeSmokeTests: XCTestCase {
    private func element(_ app: XCUIApplication, _ id: String) -> XCUIElement { app.descendants(matching: .any)[id].firstMatch }
    private func launch(date: String, camera: Bool) -> XCUIApplication {
        let app = XCUIApplication(); app.launchArguments = ["--date", date] + (camera ? ["--camera-preview"] : []); app.launch()
        XCTAssertTrue(element(app, "month-title").waitForExistence(timeout: 20))
        return app
    }
    /// Opens the camera from the home capture button. An interrupted earlier run may have left a record
    /// for this test day, in which case the app asks before replacing it.
    private func openCamera(_ app: XCUIApplication) {
        element(app, "capture-today").tap()
        if element(app, "confirm-action").waitForExistence(timeout: 2) { element(app, "confirm-action").tap() }
    }
    func testFilmShelfPreview() {
        let app = launch(date: "2026-09-17", camera: false)
        app.tabBars.buttons["필름"].tap()
        XCTAssertTrue(element(app, "film-shelf-screen").waitForExistence(timeout: 5))
        XCTAssertTrue(app.staticTexts["필름 진열장"].exists)
        XCTAssertFalse(app.staticTexts["필름 없음"].exists)
        attach(app, "film-shelf")
        for id in ["vintage", "monochrome", "warm", "clear", "fade", "natural"] {
            let card = element(app, "film-card-" + id)
            for _ in 0..<4 {
                if card.exists && card.frame.maxY < app.tabBars.firstMatch.frame.minY && card.frame.minY > 60 { break }
                let shelf = element(app, "film-shelf-screen")
                shelf.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.8))
                    .press(forDuration: 0.1, thenDragTo: shelf.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.3)),
                           withVelocity: .slow, thenHoldForDuration: 0.3)
            }
            XCTAssertTrue(card.isHittable)
            XCTAssertLessThan(card.frame.midY, app.tabBars.firstMatch.frame.minY)
            element(app, "film-card-" + id).tap()
            XCTAssertTrue(element(app, "film-preview-sheet").waitForExistence(timeout: 5))
            XCTAssertTrue(element(app, "film-preview-effect").isSelected)
            element(app, "film-preview-original").tap()
            XCTAssertTrue(element(app, "film-preview-original").isSelected)
            element(app, "film-preview-effect").tap()
            XCTAssertTrue(element(app, "film-preview-effect").isSelected)
            attach(app, "film-preview-" + id)
            element(app, "film-preview-scrim").coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.5)).tap()
            XCTAssertTrue(element(app, "film-shelf-screen").waitForExistence(timeout: 5))
        }
        app.tabBars.buttons["설정"].tap()
        element(app, "light-mode-toggle").tap()
        app.tabBars.buttons["필름"].tap()
        attach(app, "film-shelf-alternate-theme")
        app.tabBars.buttons["설정"].tap()
        element(app, "light-mode-toggle").tap()
        app.tabBars.buttons["기록"].tap()
        XCTAssertTrue(element(app, "month-title").waitForExistence(timeout: 5))
    }

    func testNewFilmsCanBeSelectedAndSaved() {
        var app = launch(date: "2026-09-17", camera: true)
        openCamera(app)
        XCTAssertTrue(element(app, "camera-shutter").waitForExistence(timeout: 10))
        element(app, "camera-films").tap()
        let list = element(app, "camera-film-list")
        for (id, label) in [("Warm", "온기"), ("Clear", "청명"), ("Fade", "바램"), ("Natural", "내추럴")] {
            let tile = element(app, "camera-film-" + id)
            for _ in 0..<4 {
                if tile.exists && list.frame.contains(tile.frame) { break }
                list.coordinate(withNormalizedOffset: CGVector(dx: 0.85, dy: 0.5))
                    .press(forDuration: 0.1, thenDragTo: list.coordinate(withNormalizedOffset: CGVector(dx: 0.15, dy: 0.5)),
                           withVelocity: .slow, thenHoldForDuration: 0.3)
            }
            XCTAssertTrue(list.frame.contains(tile.frame))
            let center = tile.frame
            app.coordinate(withNormalizedOffset: .zero)
                .withOffset(CGVector(dx: center.midX, dy: center.midY)).tap()
            let selectedFilm = XCTNSPredicateExpectation(predicate: NSPredicate(format: "label CONTAINS %@", label),
                                                        object: element(app, "camera-films"))
            XCTAssertEqual(XCTWaiter.wait(for: [selectedFilm], timeout: 5), .completed)
            attach(app, "camera-film-" + id)
        }
        element(app, "camera-films").tap()
        element(app, "camera-films").tap()
        XCTAssertTrue(element(app, "camera-film-Natural").isHittable, "reopening reveals the selected film")
        element(app, "camera-shutter").tap()
        XCTAssertTrue(element(app, "month-title").waitForExistence(timeout: 15))
        expectation(for: NSPredicate(format: "label CONTAINS %@", "기록 보기"), evaluatedWith: element(app, "day-2026-09-17"))
        waitForExpectations(timeout: 10)
        app.terminate(); app = launch(date: "2026-09-17", camera: true)
        element(app, "day-2026-09-17").tap()
        XCTAssertTrue(element(app, "record-meta").waitForExistence(timeout: 5))
        XCTAssertTrue(element(app, "record-meta").label.contains("NATURAL"))
        attach(app, "detail-natural")
        element(app, "delete-record").tap(); element(app, "confirm-action").tap()
        XCTAssertTrue(element(app, "month-title").waitForExistence(timeout: 10))
    }

    private func attach(_ app: XCUIApplication, _ name: String) {
        let shot = XCTAttachment(screenshot: app.screenshot()); shot.name = name; shot.lifetime = .keepAlways; add(shot)
    }

    private func assertCameraBackground(_ app: XCUIApplication, light: Bool) {
        let image = app.screenshot().image.cgImage!
        // The outer margin is UI, not live camera pixels or the lens artwork.
        let pixel = image.cropping(to: CGRect(x: 4, y: image.height / 2, width: 1, height: 1))!
        var rgba = [UInt8](repeating: 0, count: 4)
        let context = CGContext(data: &rgba, width: 1, height: 1, bitsPerComponent: 8, bytesPerRow: 4,
            space: CGColorSpaceCreateDeviceRGB(), bitmapInfo: CGImageAlphaInfo.premultipliedLast.rawValue)!
        context.draw(pixel, in: CGRect(x: 0, y: 0, width: 1, height: 1))
        for value in rgba.prefix(3) {
            if light { XCTAssertGreaterThan(value, 220) } else { XCTAssertLessThan(value, 20) }
        }
    }

    func testSplashHidesTabsAndCameraFollowsAppearance() {
        let app = XCUIApplication()
        app.launchArguments = ["--date", "2026-09-17", "--camera-preview", "--splash-hold-ms", "6000"]
        app.launch()
        XCTAssertTrue(element(app, "launch-splash").waitForExistence(timeout: 3))
        XCTAssertFalse(app.tabBars.buttons["설정"].isHittable)
        XCTAssertEqual(element(app, "launch-splash").frame.maxY, app.frame.maxY, accuracy: 1)
        XCTAssertFalse(element(app, "capture-today").isHittable)
        attach(app, "splash-no-tabs")
        let splashGone = XCTNSPredicateExpectation(predicate: NSPredicate(format: "exists == false"), object: element(app, "launch-splash"))
        XCTAssertEqual(XCTWaiter.wait(for: [splashGone], timeout: 15), .completed)
        XCTAssertTrue(app.tabBars.buttons["설정"].isHittable)
        attach(app, "home-after-splash")
        app.tabBars.buttons["설정"].tap()
        let initiallyLight = app.staticTexts["따뜻한 종이빛으로 보기"].exists
        if !initiallyLight { element(app, "light-mode-toggle").tap() }
        app.tabBars.buttons["기록"].tap()
        openCamera(app)
        XCTAssertTrue(element(app, "camera-shutter").waitForExistence(timeout: 10))
        assertCameraBackground(app, light: true)
        attach(app, "camera-light")
        element(app, "camera-films").tap()
        attach(app, "camera-films-light")
        element(app, "camera-close").tap()
        app.tabBars.buttons["설정"].tap()
        element(app, "light-mode-toggle").tap()
        app.tabBars.buttons["기록"].tap()
        openCamera(app)
        XCTAssertTrue(element(app, "camera-shutter").waitForExistence(timeout: 10))
        assertCameraBackground(app, light: false)
        attach(app, "camera-dark")
        element(app, "camera-close").tap()
        if initiallyLight {
            app.tabBars.buttons["설정"].tap()
            element(app, "light-mode-toggle").tap()
        }
    }

    func testTabsAndAppearancePersistence() {
        var app = launch(date: "2026-09-17", camera: true)
        element(app, "previous-month").tap()
        app.tabBars.buttons["설정"].tap()
        let toggle = element(app, "light-mode-toggle")
        XCTAssertTrue(toggle.waitForExistence(timeout: 5))
        let original = toggle.value as? String
        attach(app, "settings-before")
        toggle.tap()
        XCTAssertNotEqual(toggle.value as? String, original)
        let changed = toggle.value as? String
        attach(app, "settings-after")
        app.tabBars.buttons["기록"].tap()
        XCTAssertTrue(element(app, "month-title").label.contains("2026년 8월"))
        attach(app, "home-with-tabs")
        app.terminate()
        app = launch(date: "2026-09-17", camera: true)
        app.tabBars.buttons["설정"].tap()
        XCTAssertEqual(element(app, "light-mode-toggle").value as? String, changed)
        element(app, "light-mode-toggle").tap()
        XCTAssertEqual(element(app, "light-mode-toggle").value as? String, original)
        app.tabBars.buttons["기록"].tap()
        openCamera(app)
        XCTAssertTrue(element(app, "camera-close").waitForExistence(timeout: 10))
        XCTAssertFalse(app.tabBars.buttons["설정"].isHittable)
        element(app, "camera-close").tap()
        XCTAssertTrue(app.tabBars.buttons["설정"].waitForExistence(timeout: 5))
    }

    func testHomeIsCalendarOnly() {
        let app = launch(date: "2026-09-17", camera: false)
        XCTAssertTrue(element(app, "home-calendar").exists)
        XCTAssertFalse(element(app, "view-mode-picker").exists)
        app.tabBars.buttons["설정"].tap()
        app.tabBars.buttons["기록"].tap()
        XCTAssertTrue(element(app, "home-calendar").exists)
        app.terminate()
        app.launch()
        XCTAssertTrue(element(app, "home-calendar").waitForExistence(timeout: 20))
    }

    func testCalendarRulesAndCameraUnavailable() {
        let app = launch(date: "2026-09-17", camera: false)
        let title = element(app, "month-title")
        element(app, "previous-month").tap(); XCTAssertTrue(title.label.contains("2026년 8월"))
        element(app, "today-legend").tap(); XCTAssertTrue(title.label.contains("2026년 9월"))
        element(app, "next-month").tap(); XCTAssertTrue(title.label.contains("2026년 10월"))
        element(app, "previous-month").tap(); XCTAssertTrue(title.label.contains("2026년 9월"))
        XCTAssertFalse(element(app, "day-2026-09-18").isEnabled, "future days are inert")
        XCTAssertFalse(element(app, "day-2026-09-01").isEnabled, "missed days cannot be filled in afterwards")
        attach(app, "home")
        openCamera(app)
        XCTAssertTrue(app.staticTexts["이 기기에서는 촬영할 수 없어요. 담하루는 앱에서 찍은 사진만 남길 수 있어요."].waitForExistence(timeout: 5))
    }

    func testCaptureSavesImmediatelyThenRelaunchMemoAndDelete() {
        var app = launch(date: "2026-09-17", camera: true)
        openCamera(app)
        XCTAssertTrue(element(app, "camera-shutter").waitForExistence(timeout: 10))
        element(app, "camera-close").tap()
        XCTAssertTrue(element(app, "month-title").waitForExistence(timeout: 5), "closing the camera returns home unchanged")

        openCamera(app)
        let zoom = element(app, "camera-zoom")
        XCTAssertTrue(zoom.waitForExistence(timeout: 10))
        XCTAssertEqual(zoom.value as? String, "1×")
        zoom.coordinate(withNormalizedOffset: CGVector(dx: 0.8, dy: 0.5))
            .press(forDuration: 0.1, thenDragTo: zoom.coordinate(withNormalizedOffset: CGVector(dx: 0.2, dy: 0.5)))
        // The ruler is logarithmic: dragging left by about 1.6 doublings from 1x lands well past 2x.
        let zoomed = Double((zoom.value as? String ?? "").replacingOccurrences(of: "×", with: "")) ?? 0
        XCTAssertGreaterThan(zoomed, 2.0); XCTAssertLessThanOrEqual(zoomed, 6.0)
        // Load the monochrome film: the list takes the zoom ruler's place and the button remembers the choice.
        element(app, "camera-films").tap()
        XCTAssertTrue(element(app, "camera-film-Monochrome").waitForExistence(timeout: 5))
        XCTAssertFalse(element(app, "camera-zoom").exists)
        element(app, "camera-film-Monochrome").tap()
        XCTAssertTrue(element(app, "camera-films").label.contains("흑백"))
        attach(app, "custom-camera-film")
        element(app, "camera-films").tap()
        XCTAssertTrue(element(app, "camera-zoom").waitForExistence(timeout: 5))
        attach(app, "custom-camera")
        element(app, "camera-shutter").tap()
        // The capture shrinks into today's cell. Screen grabs do not wait for the app to go idle, so they
        // catch the flight mid-air; they are kept as attachments rather than asserted on.
        for frame in 0..<12 {
            let shot = XCTAttachment(screenshot: XCUIScreen.main.screenshot()); shot.name = "capture-flight-\(frame)"; shot.lifetime = .keepAlways; add(shot)
            Thread.sleep(forTimeInterval: 0.08)
        }
        // No picker, editor or confirm step: the shutter saves and returns home.
        XCTAssertTrue(element(app, "month-title").waitForExistence(timeout: 15), app.debugDescription)
        XCTAssertFalse(element(app, "save-photo").exists)
        let saved = NSPredicate(format: "label CONTAINS %@", "기록 보기")
        expectation(for: saved, evaluatedWith: element(app, "day-2026-09-17")); waitForExpectations(timeout: 10)

        app.terminate(); app = launch(date: "2026-09-17", camera: true)
        element(app, "day-2026-09-17").tap()
        XCTAssertTrue(element(app, "edit-memo").waitForExistence(timeout: 5), "the capture survived a relaunch")
        XCTAssertTrue(element(app, "change-photo").exists, "today's photo can be retaken")
        XCTAssertTrue(element(app, "record-meta").label.contains("MONOCHROME"), "the record remembers its film")
        element(app, "edit-memo").tap()
        let input = element(app, "memo-input"); XCTAssertTrue(input.waitForExistence(timeout: 5)); input.tap(); input.typeText("A quiet afternoon")
        element(app, "save-memo").tap()
        // The saved line is itself the edit control, so it is exposed as a button rather than static text.
        XCTAssertTrue(app.descendants(matching: .any).matching(NSPredicate(format: "label == %@", "A quiet afternoon")).firstMatch.waitForExistence(timeout: 5))
        attach(app, "photo-detail")
        element(app, "delete-record").tap(); element(app, "confirm-action").tap()
        XCTAssertTrue(element(app, "month-title").waitForExistence(timeout: 10))

        app.terminate(); app = launch(date: "2026-09-17", camera: true)
        element(app, "day-2026-09-17").tap()
        XCTAssertTrue(element(app, "camera-shutter").waitForExistence(timeout: 10), "an empty today opens the camera directly")
        element(app, "camera-close").tap()
    }

    func testPastRecordKeepsItsPhoto() {
        var app = launch(date: "2026-09-16", camera: true)
        openCamera(app)
        XCTAssertTrue(element(app, "camera-shutter").waitForExistence(timeout: 10))
        // Vintage is the film that carries grain and vignette; without a film a photo stays as captured.
        element(app, "camera-films").tap()
        XCTAssertTrue(element(app, "camera-film-Vintage").waitForExistence(timeout: 5))
        element(app, "camera-film-Vintage").tap()
        attach(app, "custom-camera-vintage")
        element(app, "camera-shutter").tap()
        XCTAssertTrue(element(app, "month-title").waitForExistence(timeout: 15))
        expectation(for: NSPredicate(format: "label CONTAINS %@", "기록 보기"), evaluatedWith: element(app, "day-2026-09-16")); waitForExpectations(timeout: 10)

        // The next day the same record is a past day: viewable and deletable, but never retaken.
        app.terminate(); app = launch(date: "2026-09-17", camera: true)
        element(app, "day-2026-09-16").tap()
        XCTAssertTrue(element(app, "delete-record").waitForExistence(timeout: 5))
        XCTAssertFalse(element(app, "change-photo").exists)
        XCTAssertTrue(element(app, "record-meta").label.contains("VINTAGE"))
        attach(app, "photo-detail-vintage")
        element(app, "delete-record").tap()
        XCTAssertTrue(app.staticTexts.containing(NSPredicate(format: "label CONTAINS %@", "다시 채울 수 없어요")).firstMatch.waitForExistence(timeout: 5))
        element(app, "confirm-action").tap()
        XCTAssertTrue(element(app, "month-title").waitForExistence(timeout: 10))
        XCTAssertFalse(element(app, "day-2026-09-16").isEnabled, "once deleted, the missed day stays empty")
    }
}
