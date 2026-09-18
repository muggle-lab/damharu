import SwiftUI
import PhotosUI
import AVFoundation
import ImageIO
import DamharuShared

@main
struct DamharuApp: App {
    var body: some Scene { WindowGroup { DiaryRoot().ignoresSafeArea() } }
}

private struct DiaryRoot: UIViewControllerRepresentable {
    func makeCoordinator() -> PhotoCoordinator { PhotoCoordinator() }
    func makeUIViewController(context: Context) -> UIViewController {
        var date = ""
        #if DEBUG
        let args = ProcessInfo.processInfo.arguments
        if let index = args.firstIndex(of: "--date"), index + 1 < args.count { date = args[index + 1] }
        #endif
        let controller = MainViewControllerKt.MainViewController(photoSource: context.coordinator, dateOverride: date)
        context.coordinator.host = controller
        return controller
    }
    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

/** Keeps Apple picker/delegate lifetime outside the shared UI. Library access uses PHPicker,
    so no broad photo-library permission is requested. Imported copies strip location metadata. */
private final class PhotoCoordinator: NSObject, PhotoSource, PHPickerViewControllerDelegate, UIImagePickerControllerDelegate, UINavigationControllerDelegate {
    weak var host: UIViewController?
    private var completion: ((String?, String?) -> Void)?
    private let queue = DispatchQueue(label: "damharu.photo-import", qos: .userInitiated)
    private let temp = FileManager.default.temporaryDirectory.appendingPathComponent("damharu-imports", isDirectory: true)

    override init() {
        super.init()
        try? FileManager.default.createDirectory(at: temp, withIntermediateDirectories: true)
        // Imports are never persisted references; remove abandoned imports on next launch.
        for file in (try? FileManager.default.contentsOfDirectory(at: temp, includingPropertiesForKeys: nil)) ?? [] { try? FileManager.default.removeItem(at: file) }
    }
    func request(camera: Bool, completion: @escaping (String?, String?) -> Void) {
        guard self.completion == nil else { completion(nil, "사진을 불러오는 중이에요."); return }
        self.completion = completion
        if camera {
            guard UIImagePickerController.isSourceTypeAvailable(.camera) else { finish(nil, "이 기기에서는 촬영할 수 없어요. 사진 보관함에서 선택해 주세요."); return }
            switch AVCaptureDevice.authorizationStatus(for: .video) {
            case .authorized: presentCamera()
            case .notDetermined:
                AVCaptureDevice.requestAccess(for: .video) { allowed in DispatchQueue.main.async {
                    if allowed { self.presentCamera() } else { self.finish(nil, "카메라 접근이 꺼져 있어요. 설정에서 담하루의 카메라 접근을 허용해 주세요.") }
                } }
            default: finish(nil, "카메라 접근이 꺼져 있어요. 설정에서 담하루의 카메라 접근을 허용해 주세요.")
            }
        } else {
            var config = PHPickerConfiguration(photoLibrary: .shared())
            config.filter = .images; config.selectionLimit = 1
            let picker = PHPickerViewController(configuration: config); picker.delegate = self
            present(picker)
        }
    }
    private func present(_ picker: UIViewController) {
        guard var presenter = host else { finish(nil, "화면을 열 수 없어요. 다시 시도해 주세요."); return }
        while let next = presenter.presentedViewController { presenter = next }
        presenter.present(picker, animated: true)
    }
    private func presentCamera() {
        let picker = UIImagePickerController(); picker.sourceType = .camera; picker.delegate = self; present(picker)
    }
    private func finish(_ path: String?, _ error: String?) {
        let callback = completion; completion = nil; callback?(path, error)
    }
    func picker(_ picker: PHPickerViewController, didFinishPicking results: [PHPickerResult]) {
        picker.dismiss(animated: true)
        guard let item = results.first?.itemProvider else { finish(nil, nil); return }
        item.loadFileRepresentation(forTypeIdentifier: "public.image") { url, error in
            // The provider URL only lives during this callback. Decode and write our own copy here.
            do {
                guard let url = url, error == nil,
                      let source = CGImageSourceCreateWithURL(url as CFURL, nil),
                      let image = CGImageSourceCreateThumbnailAtIndex(source, 0, [
                        kCGImageSourceCreateThumbnailFromImageAlways: true,
                        kCGImageSourceCreateThumbnailWithTransform: true,
                        kCGImageSourceThumbnailMaxPixelSize: 2048,
                        kCGImageSourceShouldCacheImmediately: true
                      ] as CFDictionary) else { throw ImportError.invalid }
                let path = try self.store(UIImage(cgImage: image))
                DispatchQueue.main.async { self.finish(path, nil) }
            } catch { DispatchQueue.main.async { self.finish(nil, "사진을 불러오지 못했어요. 기기에 내려받은 다른 사진으로 다시 시도해 주세요.") } }
        }
    }
    func imagePickerControllerDidCancel(_ picker: UIImagePickerController) { picker.dismiss(animated: true); finish(nil, nil) }
    func imagePickerController(_ picker: UIImagePickerController, didFinishPickingMediaWithInfo info: [UIImagePickerController.InfoKey: Any]) {
        picker.dismiss(animated: true)
        guard let image = info[.originalImage] as? UIImage else { finish(nil, "사진을 읽지 못했어요."); return }
        queue.async {
            do { let path = try self.store(image); DispatchQueue.main.async { self.finish(path, nil) } }
            catch { DispatchQueue.main.async { self.finish(nil, "촬영한 사진을 준비하지 못했어요. 다시 시도해 주세요.") } }
        }
    }
    private func store(_ image: UIImage) throws -> String {
        let scale = min(1, 2048 / max(image.size.width, image.size.height))
        let size = CGSize(width: max(1, image.size.width * scale), height: max(1, image.size.height * scale))
        let format = UIGraphicsImageRendererFormat(); format.scale = 1; format.opaque = true
        let normalized = UIGraphicsImageRenderer(size: size, format: format).image { context in
            UIColor.white.setFill(); context.fill(CGRect(origin: .zero, size: size)); image.draw(in: CGRect(origin: .zero, size: size))
        }
        guard let data = normalized.jpegData(compressionQuality: 0.96) else { throw ImportError.invalid }
        let url = temp.appendingPathComponent(UUID().uuidString + ".jpg")
        try data.write(to: url, options: .atomic)
        return url.path
    }
    private enum ImportError: Error { case invalid }
}
