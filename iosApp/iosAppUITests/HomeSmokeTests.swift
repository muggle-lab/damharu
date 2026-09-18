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
}
