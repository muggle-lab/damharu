import XCTest

final class HomeSmokeTests: XCTestCase {
    private func element(_ app: XCUIApplication, _ id: String) -> XCUIElement { app.descendants(matching: .any)[id].firstMatch }
    func testCalendarAndCameraUnavailable() {
        let app = XCUIApplication(); app.launchArguments = ["--date", "2026-09-17"]; app.launch()
        let title = element(app, "month-title")
        XCTAssertTrue(title.waitForExistence(timeout: 20))
        element(app, "previous-month").tap(); XCTAssertTrue(title.label.contains("2026년 8월"))
        element(app, "next-month").tap(); XCTAssertTrue(title.label.contains("2026년 9월"))
        XCTAssertFalse(element(app, "day-2026-09-18").isEnabled)
        element(app, "day-2026-09-01").tap()
        XCTAssertTrue(element(app, "take-photo").waitForExistence(timeout: 5))
        element(app, "take-photo").tap()
        XCTAssertTrue(app.staticTexts["이 기기에서는 촬영할 수 없어요. 사진 보관함에서 선택해 주세요."].waitForExistence(timeout: 5))
    }
    func testPhotoSaveRelaunchMemoAndDelete() {
        let app = XCUIApplication(); app.launchArguments = ["--date", "2026-09-17"]; app.launch()
        XCTAssertTrue(element(app, "month-title").waitForExistence(timeout: 20))
        element(app, "day-2026-09-02").tap()
        // Idempotent reruns: enter replacement if a previous interrupted run saved this day.
        if element(app, "change-photo").waitForExistence(timeout: 2) { element(app, "change-photo").tap() }
        XCTAssertTrue(element(app, "pick-photo").waitForExistence(timeout: 5)); element(app, "pick-photo").tap()
        let cancel = app.buttons["Cancel"].firstMatch
        XCTAssertTrue(cancel.waitForExistence(timeout: 10)); cancel.tap()
        XCTAssertTrue(element(app, "pick-photo").waitForExistence(timeout: 5)); element(app, "pick-photo").tap()
        let cell = app.images.matching(identifier: "PXGGridLayout-Info").firstMatch
        XCTAssertTrue(cell.waitForExistence(timeout: 10), app.debugDescription)
        // PHPicker's remote image cells report isHittable=false on iOS 26 despite visible frames.
        cell.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.5)).tap()
        XCTAssertTrue(element(app, "filter-Warm").waitForExistence(timeout: 15), app.debugDescription)
        element(app, "filter-Warm").tap()
        let editorShot = XCTAttachment(screenshot: app.screenshot()); editorShot.name = "photo-editor"; editorShot.lifetime = .keepAlways; add(editorShot)
        if !element(app, "save-photo").isHittable { app.swipeUp(); Thread.sleep(forTimeInterval: 1) }
        element(app, "save-photo").tap()
        if element(app, "confirm-action").waitForExistence(timeout: 2) { element(app, "confirm-action").tap() }
        XCTAssertTrue(element(app, "month-title").waitForExistence(timeout: 15), app.debugDescription)
        app.terminate(); app.launch()
        XCTAssertTrue(element(app, "month-title").waitForExistence(timeout: 20))
        element(app, "day-2026-09-02").tap()
        XCTAssertTrue(element(app, "edit-memo").waitForExistence(timeout: 5)); element(app, "edit-memo").tap()
        let input = element(app, "memo-input"); XCTAssertTrue(input.waitForExistence(timeout: 5)); input.tap(); input.typeText("A quiet afternoon")
        if !element(app, "save-memo").isHittable { app.swipeUp(); Thread.sleep(forTimeInterval: 1) }
        element(app, "save-memo").tap()
        XCTAssertTrue(app.staticTexts["A quiet afternoon"].waitForExistence(timeout: 5))
        let screenshot = XCTAttachment(screenshot: app.screenshot()); screenshot.name = "photo-detail"; screenshot.lifetime = .keepAlways; add(screenshot)
        if !element(app, "delete-record").isHittable { app.swipeUp(); Thread.sleep(forTimeInterval: 1) }
        element(app, "delete-record").tap(); element(app, "confirm-action").tap()
        XCTAssertTrue(element(app, "month-title").waitForExistence(timeout: 10))
        app.terminate(); app.launch()
        XCTAssertTrue(element(app, "month-title").waitForExistence(timeout: 20)); element(app, "day-2026-09-02").tap()
        XCTAssertTrue(element(app, "pick-photo").waitForExistence(timeout: 5))
    }
}
