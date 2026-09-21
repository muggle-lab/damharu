import XCTest

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
    private func attach(_ app: XCUIApplication, _ name: String) {
        let shot = XCTAttachment(screenshot: app.screenshot()); shot.name = name; shot.lifetime = .keepAlways; add(shot)
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
        XCTAssertFalse(element(app, "camera-filters").exists, "the camera has no filter control")
        element(app, "camera-close").tap()
        XCTAssertTrue(element(app, "month-title").waitForExistence(timeout: 5), "closing the camera returns home unchanged")

        openCamera(app)
        let zoom = element(app, "camera-zoom")
        XCTAssertTrue(zoom.waitForExistence(timeout: 10))
        zoom.coordinate(withNormalizedOffset: CGVector(dx: 0.8, dy: 0.5))
            .press(forDuration: 0.1, thenDragTo: zoom.coordinate(withNormalizedOffset: CGVector(dx: 0.2, dy: 0.5)))
        attach(app, "custom-camera")
        element(app, "camera-shutter").tap()
        // No picker, editor or confirm step: the shutter saves and returns home.
        XCTAssertTrue(element(app, "month-title").waitForExistence(timeout: 15), app.debugDescription)
        XCTAssertFalse(element(app, "save-photo").exists)

        app.terminate(); app = launch(date: "2026-09-17", camera: true)
        element(app, "day-2026-09-17").tap()
        XCTAssertTrue(element(app, "edit-memo").waitForExistence(timeout: 5), "the capture survived a relaunch")
        XCTAssertTrue(element(app, "change-photo").exists, "today's photo can be retaken")
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
        element(app, "camera-shutter").tap()
        XCTAssertTrue(element(app, "month-title").waitForExistence(timeout: 15))

        // The next day the same record is a past day: viewable and deletable, but never retaken.
        app.terminate(); app = launch(date: "2026-09-17", camera: true)
        element(app, "day-2026-09-16").tap()
        XCTAssertTrue(element(app, "delete-record").waitForExistence(timeout: 5))
        XCTAssertFalse(element(app, "change-photo").exists)
        element(app, "delete-record").tap()
        XCTAssertTrue(app.staticTexts.containing(NSPredicate(format: "label CONTAINS %@", "다시 채울 수 없어요")).firstMatch.waitForExistence(timeout: 5))
        element(app, "confirm-action").tap()
        XCTAssertTrue(element(app, "month-title").waitForExistence(timeout: 10))
        XCTAssertFalse(element(app, "day-2026-09-16").isEnabled, "once deleted, the missed day stays empty")
    }
}
