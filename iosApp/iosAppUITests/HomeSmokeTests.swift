import XCTest

final class HomeSmokeTests: XCTestCase {
    func testHomeNavigationAndUnimplementedAction() {
        let app = XCUIApplication()
        app.launchArguments = ["--date", "2026-09-17", "--scenario", "empty"]
        app.launch()
        let title = app.descendants(matching: .any)["month-title"].firstMatch
        XCTAssertTrue(title.waitForExistence(timeout: 15))
        XCTAssertTrue(title.label.contains("2026년 9월"))
        app.descendants(matching: .any)["previous-month"].firstMatch.tap()
        XCTAssertTrue(title.label.contains("2026년 8월"))
        app.descendants(matching: .any)["next-month"].firstMatch.tap()
        XCTAssertTrue(title.label.contains("2026년 9월"))
        let future = app.descendants(matching: .any)["day-2026-09-18"].firstMatch
        XCTAssertFalse(future.isEnabled)
        let today = app.descendants(matching: .any)["today-action"].firstMatch
        XCTAssertTrue(today.isHittable)
        XCTAssertLessThanOrEqual(today.frame.maxX, app.frame.maxX)
        today.tap()
        XCTAssertTrue(app.staticTexts["사진 기록을 준비하고 있어요"].waitForExistence(timeout: 3))
    }

    func testSampleAndSavedStates() {
        let app = XCUIApplication()
        app.launchArguments = ["--date", "2026-09-17", "--scenario", "saved"]
        app.launch()
        let today = app.descendants(matching: .any)["today-action"].firstMatch
        XCTAssertTrue(today.waitForExistence(timeout: 15))
        XCTAssertTrue(today.label.contains("오늘의 사진 보기"))
        let screenshot = XCTAttachment(screenshot: app.screenshot())
        screenshot.name = "home-saved"
        screenshot.lifetime = .keepAlways
        add(screenshot)
        app.descendants(matching: .any)["day-2026-09-16"].firstMatch.tap()
        XCTAssertTrue(app.staticTexts["기록 보기를 준비하고 있어요"].waitForExistence(timeout: 3))
    }
}
