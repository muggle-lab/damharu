import XCTest

// Capture fixture, temporarily appended to the existing UI-test source by build_capture.py.
// Runs only on the dedicated Damharu Store Preview simulator, with seeded sample data.
final class StoreScreenshotTests: XCTestCase {
    private func element(_ app: XCUIApplication, _ id: String) -> XCUIElement {
        app.descendants(matching: .any)[id].firstMatch
    }
    private func attach(_ app: XCUIApplication, _ name: String) {
        let shot = XCTAttachment(screenshot: app.screenshot())
        shot.name = name; shot.lifetime = .keepAlways; add(shot)
    }
    func testStoreScreens() {
        continueAfterFailure = false
        let app = XCUIApplication()
        app.launchArguments = ["--date", "2026-10-23", "--camera-preview", "--store-photo"]
        app.launch()
        XCTAssertTrue(element(app, "month-title").waitForExistence(timeout: 30))
        element(app, "capture-today").tap()
        XCTAssertTrue(element(app, "camera-shutter").waitForExistence(timeout: 15))
        Thread.sleep(forTimeInterval: 2)
        attach(app, "02-camera")
        element(app, "camera-films").tap()
        XCTAssertTrue(element(app, "camera-film-Vintage").waitForExistence(timeout: 5))
        element(app, "camera-film-Vintage").tap()
        XCTAssertTrue(element(app, "camera-films").label.contains("빈티지"))
        Thread.sleep(forTimeInterval: 1)
        attach(app, "03-film")
        element(app, "camera-close").tap()
        XCTAssertTrue(element(app, "month-title").waitForExistence(timeout: 10))
        element(app, "day-2026-10-01").tap()
        XCTAssertTrue(element(app, "edit-memo").waitForExistence(timeout: 10))
        XCTAssertTrue(element(app, "record-meta").label.contains("2026.10.01"))
        attach(app, "04-memo")
    }
}
