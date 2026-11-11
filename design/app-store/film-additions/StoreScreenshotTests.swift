import XCTest

final class StoreFilmAdditionTests: XCTestCase {
    private func element(_ app: XCUIApplication, _ id: String) -> XCUIElement {
        app.descendants(matching: .any)[id].firstMatch
    }
    private func attach(_ app: XCUIApplication, _ name: String) {
        let shot = XCTAttachment(screenshot: app.screenshot())
        shot.name = name; shot.lifetime = .keepAlways; add(shot)
    }
    func testFilmScreens() {
        continueAfterFailure = false
        let app = XCUIApplication()
        app.launchArguments = ["--date", "2026-10-23", "--camera-preview", "--store-photo"]
        app.launch()
        XCTAssertTrue(element(app, "month-title").waitForExistence(timeout: 30))
        app.tabBars.buttons["설정"].tap()
        if app.staticTexts["따뜻한 종이빛으로 보기"].exists { element(app, "light-mode-toggle").tap() }
        app.tabBars.buttons["기록"].tap()
        element(app, "capture-today").tap()
        if element(app, "confirm-action").waitForExistence(timeout: 2) { element(app, "confirm-action").tap() }
        XCTAssertTrue(element(app, "camera-shutter").waitForExistence(timeout: 15))
        element(app, "camera-films").tap()
        XCTAssertTrue(element(app, "camera-film-Vintage").waitForExistence(timeout: 5))
        element(app, "camera-film-Vintage").tap()
        XCTAssertTrue(element(app, "camera-films").label.contains("빈티지"))
        Thread.sleep(forTimeInterval: 1)
        attach(app, "05-film-picker")
        element(app, "camera-close").tap()
        XCTAssertTrue(element(app, "month-title").waitForExistence(timeout: 10))
        app.tabBars.buttons["필름"].tap()
        XCTAssertTrue(element(app, "film-card-coming-soon").waitForExistence(timeout: 5))
        Thread.sleep(forTimeInterval: 1)
        attach(app, "06-film-shelf")
    }
}
