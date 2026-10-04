import SwiftUI
import AVFoundation
import MetalKit
import DamharuShared

@main
struct DamharuApp: App {
    // The app uses one fixed dark palette, so system chrome (status bar, keyboard) follows it.
    var body: some Scene { WindowGroup { DiaryRoot().ignoresSafeArea().preferredColorScheme(.dark) } }
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

/** The in-app camera is the only photo input. Captures are normalized into an app-owned temporary
    JPEG (max 2048px, upright, no location metadata) and handed to the shared UI, which saves them at once. */
private final class PhotoCoordinator: NSObject, PhotoSource {
    weak var host: UIViewController?
    private var completion: ((String?, String?, String?) -> Void)?
    private weak var camera: NativeCameraController?
    private let queue = DispatchQueue(label: "damharu.photo-capture", qos: .userInitiated)
    private let temp = FileManager.default.temporaryDirectory.appendingPathComponent("damharu-captures", isDirectory: true)

    override init() {
        super.init()
        try? FileManager.default.createDirectory(at: temp, withIntermediateDirectories: true)
        // Captures are never persisted references; remove abandoned ones on next launch.
        for file in (try? FileManager.default.contentsOfDirectory(at: temp, includingPropertiesForKeys: nil)) ?? [] { try? FileManager.default.removeItem(at: file) }
    }
    func capture(completion: @escaping (String?, String?, String?) -> Void) {
        guard self.completion == nil else { completion(nil, nil, "사진을 준비하는 중이에요."); return }
        self.completion = completion
        guard NativeCameraController.available else { finish(nil, nil, "이 기기에서는 촬영할 수 없어요. 담하루는 앱에서 찍은 사진만 남길 수 있어요."); return }
        #if DEBUG
        if ProcessInfo.processInfo.arguments.contains("--camera-preview") { presentCamera(); return }
        #endif
        // There is no other way to add a photo, so a denied permission must point at Settings.
        let denied = "카메라 접근이 꺼져 있어요. 설정 > 담하루에서 카메라를 허용하면 오늘을 남길 수 있어요."
        switch AVCaptureDevice.authorizationStatus(for: .video) {
        case .authorized: presentCamera()
        case .notDetermined:
            AVCaptureDevice.requestAccess(for: .video) { allowed in DispatchQueue.main.async {
                if allowed { self.presentCamera() } else { self.finish(nil, nil, denied) }
            } }
        default: finish(nil, nil, denied)
        }
    }
    private func presentCamera() {
        guard var presenter = host else { finish(nil, nil, "화면을 열 수 없어요. 다시 시도해 주세요."); return }
        while let next = presenter.presentedViewController { presenter = next }
        // The capture stays unfiltered here; the shared store bakes the film in when it saves.
        let camera = NativeCameraController { [weak self] image, film in
            guard let self else { return }
            guard let image else { self.finish(nil, nil, nil); return }
            // After a shot the camera is still on screen, frozen. The shared UI closes it through
            // closeCamera() once the photo is ready underneath, so nothing shows in between.
            self.queue.async {
                do { let path = try self.store(image); DispatchQueue.main.async { self.finish(path, film.id, nil) } }
                catch { DispatchQueue.main.async {
                    self.closeCamera()
                    self.finish(nil, nil, "촬영한 사진을 준비하지 못했어요. 다시 시도해 주세요.")
                } }
            }
        }
        self.camera = camera
        presenter.present(camera, animated: true)
    }
    func closeCamera() {
        camera?.dismiss(animated: false); camera = nil
    }
    private func finish(_ path: String?, _ film: String?, _ error: String?) {
        let callback = completion; completion = nil; callback?(path, film, error)
    }
    private func store(_ image: UIImage) throws -> String {
        let scale = min(1, 2048 / max(image.size.width, image.size.height))
        let size = CGSize(width: max(1, image.size.width * scale), height: max(1, image.size.height * scale))
        let format = UIGraphicsImageRendererFormat(); format.scale = 1; format.opaque = true
        let normalized = UIGraphicsImageRenderer(size: size, format: format).image { context in
            UIColor.white.setFill(); context.fill(CGRect(origin: .zero, size: size)); image.draw(in: CGRect(origin: .zero, size: size))
        }
        guard let data = normalized.jpegData(compressionQuality: 0.96) else { throw CaptureError.invalid }
        let url = temp.appendingPathComponent(UUID().uuidString + ".jpg")
        try data.write(to: url, options: .atomic)
        return url.path
    }
    private enum CaptureError: Error { case invalid }
}

/** AVFoundation owns capture on one serial queue. Compose owns controls and UI state. */
private final class NativeCameraController: UIViewController, CameraActions, AVCapturePhotoCaptureDelegate, AVCaptureVideoDataOutputSampleBufferDelegate {
    static var available: Bool {
        #if DEBUG
        if ProcessInfo.processInfo.arguments.contains("--camera-preview") { return true }
        #endif
        return AVCaptureDevice.default(.builtInWideAngleCamera, for: .video, position: .back) != nil
    }
    private let state = CameraState()
    private let preview = CameraMetalPreview()
    private let session = AVCaptureSession()
    private let photos = AVCapturePhotoOutput()
    private let video = AVCaptureVideoDataOutput()
    private let cameraQueue = DispatchQueue(label: "damharu.camera", qos: .userInitiated)
    private let frameQueue = DispatchQueue(label: "damharu.camera-frames", qos: .userInteractive)
    private var device: AVCaptureDevice?
    private var multiplier: CGFloat = 1
    private var observers: [NSObjectProtocol] = []
    #if DEBUG
    private var demoTimer: DispatchSourceTimer?
    #endif
    private let zoomLock = NSLock()
    private var pendingZoom: CGFloat? // zoomLock
    private var zoomScheduled = false // zoomLock
    private var closed = false // cameraQueue only
    private var capturing = false // cameraQueue only
    private var completion: ((UIImage?, Film) -> Void)? // main only
    private var demo: Bool {
        #if DEBUG
        return ProcessInfo.processInfo.arguments.contains("--camera-preview")
        #else
        return false
        #endif
    }
    init(completion: @escaping (UIImage?, Film) -> Void) {
        self.completion = completion
        super.init(nibName: nil, bundle: nil)
        // The calendar keeps rendering underneath, so closing without a transition reveals it instantly.
        modalPresentationStyle = .overFullScreen
    }
    required init?(coder: NSCoder) { fatalError("init(coder:) has not been implemented") }
    override var supportedInterfaceOrientations: UIInterfaceOrientationMask { .portrait }
    override func viewDidLoad() {
        super.viewDidLoad()
        preview.backgroundColor = .black; preview.clipsToBounds = true
        // Compose draws above this native view but cannot clip it, so the preview rounds its own layer.
        preview.layer.cornerRadius = CGFloat(CameraScreenKt.CAMERA_PREVIEW_CORNER_DP); preview.layer.cornerCurve = .continuous
        preview.onFramePresented = { [weak self] in
            guard let self, self.completion != nil, self.state.error == nil else { return }
            if !self.state.ready { self.state.ready = true }
        }
        preview.onFailure = { [weak self] in self?.report("카메라 미리보기를 표시하지 못했어요. 닫고 다시 시도해 주세요.") }
        let controls = MainViewControllerKt.CameraViewController(state: state, actions: self, preview: preview)
        addChild(controls); view.addSubview(controls.view); controls.view.translatesAutoresizingMaskIntoConstraints = false
        NSLayoutConstraint.activate([controls.view.leadingAnchor.constraint(equalTo: view.leadingAnchor), controls.view.trailingAnchor.constraint(equalTo: view.trailingAnchor), controls.view.topAnchor.constraint(equalTo: view.topAnchor), controls.view.bottomAnchor.constraint(equalTo: view.bottomAnchor)])
        controls.didMove(toParent: self)
        observe(UIApplication.didEnterBackgroundNotification) { camera in
            camera.state.ready = false
            camera.preview.suspend()
            camera.cameraQueue.async { camera.session.stopRunning() }
        }
        observe(UIApplication.didBecomeActiveNotification) { camera in
            camera.preview.resume()
            camera.cameraQueue.async {
                guard !camera.closed, camera.device != nil else { return }
                camera.session.startRunning()
            }
        }
        observe(.AVCaptureSessionWasInterrupted, object: session) { camera in
            camera.state.ready = false; camera.state.busy = false
            camera.state.error = "카메라 사용이 중단됐어요. 잠시 기다리거나 닫고 다시 시도해 주세요."
        }
        observe(.AVCaptureSessionInterruptionEnded, object: session) { camera in
            camera.state.error = nil
            camera.cameraQueue.async { if !camera.closed { camera.session.startRunning() } }
        }
        observe(.AVCaptureSessionRuntimeError, object: session) { camera in
            camera.state.ready = false; camera.state.busy = false
            camera.state.error = "카메라를 사용할 수 없어요. 닫고 다시 시도해 주세요."
        }
        guard preview.isAvailable else { report("카메라 미리보기를 사용할 수 없어요."); return }
        #if DEBUG
        if demo { startDemoFrames(); return }
        #endif
        cameraQueue.async { self.configure() }
    }
    private func observe(_ name: Notification.Name, object: Any? = nil, action: @escaping (NativeCameraController) -> Void) {
        observers.append(NotificationCenter.default.addObserver(forName: name, object: object, queue: .main) { [weak self] _ in
            guard let self else { return }; action(self)
        })
    }
    private func configure() {
        guard !closed else { return }
        do {
            let chosen = AVCaptureDevice.default(.builtInTripleCamera, for: .video, position: .back)
                ?? AVCaptureDevice.default(.builtInDualWideCamera, for: .video, position: .back)
                ?? AVCaptureDevice.default(.builtInWideAngleCamera, for: .video, position: .back)
            guard let chosen else { throw CameraError.unavailable }
            let input = try AVCaptureDeviceInput(device: chosen)
            session.beginConfiguration()
            session.sessionPreset = .photo
            guard session.canAddInput(input), session.canAddOutput(photos), session.canAddOutput(video) else {
                session.commitConfiguration(); throw CameraError.unavailable
            }
            session.addInput(input); session.addOutput(photos); session.addOutput(video)
            video.videoSettings = [kCVPixelBufferPixelFormatTypeKey as String: kCVPixelFormatType_32BGRA]
            video.alwaysDiscardsLateVideoFrames = true
            video.automaticallyConfiguresOutputBufferDimensions = false
            video.deliversPreviewSizedOutputBuffers = true
            video.setSampleBufferDelegate(self, queue: frameQueue)
            video.connection(with: .video)?.videoOrientation = .portrait
            photos.connection(with: .video)?.videoOrientation = .portrait
            session.commitConfiguration()
            device = chosen
            if #available(iOS 18.0, *) { multiplier = chosen.displayVideoZoomFactorMultiplier }
            else if chosen.constituentDevices.contains(where: { $0.deviceType == .builtInUltraWideCamera }),
                    let wideSwitch = chosen.virtualDeviceSwitchOverVideoZoomFactors.first { multiplier = 1 / CGFloat(truncating: wideSwitch) }
            let minZoom = chosen.minAvailableVideoZoomFactor * multiplier
            let maxZoom = max(minZoom, min(6, chosen.maxAvailableVideoZoomFactor * multiplier))
            let initial = min(max(1, minZoom), maxZoom)
            try chosen.lockForConfiguration()
            if chosen.isFocusModeSupported(.continuousAutoFocus) { chosen.focusMode = .continuousAutoFocus }
            if chosen.isExposureModeSupported(.continuousAutoExposure) { chosen.exposureMode = .continuousAutoExposure }
            if chosen.isWhiteBalanceModeSupported(.continuousAutoWhiteBalance) { chosen.whiteBalanceMode = .continuousAutoWhiteBalance }
            chosen.videoZoomFactor = initial / multiplier
            chosen.unlockForConfiguration()
            DispatchQueue.main.async { self.state.configure(minimum: Float(minZoom), maximum: Float(maxZoom), initial: Float(initial)) }
            session.startRunning()
        } catch { report("카메라를 열지 못했어요. 닫고 다시 시도해 주세요.") }
    }
    /// The ruler sends a value every frame while dragging or coasting. Only the newest one matters, so
    /// requests are coalesced instead of queueing a lock/unlock per frame behind a busy camera queue.
    func zoom(value: Float) {
        zoomLock.lock()
        pendingZoom = CGFloat(value)
        let schedule = !zoomScheduled
        if schedule { zoomScheduled = true }
        zoomLock.unlock()
        guard schedule else { return }
        cameraQueue.async {
            self.zoomLock.lock()
            let target = self.pendingZoom; self.pendingZoom = nil; self.zoomScheduled = false
            self.zoomLock.unlock()
            guard let target, !self.closed, !self.capturing, let device = self.device else { return }
            do {
                try device.lockForConfiguration(); defer { device.unlockForConfiguration() }
                device.videoZoomFactor = min(max(target / self.multiplier, device.minAvailableVideoZoomFactor), device.maxAvailableVideoZoomFactor)
            } catch { self.report("배율을 변경하지 못했어요. 닫고 다시 시도해 주세요.") }
        }
    }
    func film(value: Film) { preview.selectFilm(value) }
    func capture() {
        guard !state.busy, state.ready else { return }
        state.busy = true
        if demo {
            let image = UIGraphicsImageRenderer(size: CGSize(width: 300, height: 400)).image { c in
                UIColor(red: 0.7, green: 0.6, blue: 0.45, alpha: 1).setFill(); c.fill(CGRect(x: 0, y: 0, width: 300, height: 400))
            }
            complete(image, state.film); return
        }
        cameraQueue.async {
            guard !self.closed, self.session.isRunning, !self.capturing else {
                DispatchQueue.main.async { self.state.busy = false }; return
            }
            self.capturing = true
            let settings = AVCapturePhotoSettings()
            settings.flashMode = .off
            self.photos.capturePhoto(with: settings, delegate: self)
        }
    }
    func photoOutput(_ output: AVCapturePhotoOutput, didFinishProcessingPhoto photo: AVCapturePhoto, error: Error?) {
        cameraQueue.async {
            guard !self.closed else { return }
            self.capturing = false
            guard error == nil, let data = photo.fileDataRepresentation(), let image = UIImage(data: data) else {
                self.report("촬영하지 못했어요. 닫고 다시 시도해 주세요."); return
            }
            DispatchQueue.main.async { self.complete(image, self.state.film) }
        }
    }
    func captureOutput(_ output: AVCaptureOutput, didOutput sampleBuffer: CMSampleBuffer, from connection: AVCaptureConnection) {
        guard let buffer = CMSampleBufferGetImageBuffer(sampleBuffer) else { return }
        // Never render or wait here. The preview retains at most one waiting buffer.
        preview.offer(buffer)
    }
    private func report(_ message: String) {
        DispatchQueue.main.async { self.state.busy = false; self.state.ready = false; self.state.error = message }
    }
    func close() { guard !state.busy else { return }; complete(nil, .none) }
    private func complete(_ image: UIImage?, _ film: Film) {
        guard let callback = completion else { return }; completion = nil
        shutdown()
        // A cancel slides away as usual. A shot stays up, frozen on its last frame, until the owner closes it.
        if image == nil { dismiss(animated: true) { callback(nil, film) } } else { callback(image, film) }
    }
    private func shutdown() {
        preview.suspend()
        #if DEBUG
        demoTimer?.cancel(); demoTimer = nil
        #endif
        observers.forEach { NotificationCenter.default.removeObserver($0) }; observers.removeAll()
        cameraQueue.async {
            self.closed = true; self.session.stopRunning(); self.video.setSampleBufferDelegate(nil, queue: nil)
        }
    }
    override func viewDidDisappear(_ animated: Bool) {
        super.viewDidDisappear(animated); shutdown()
        children.forEach { child in child.willMove(toParent: nil); child.view.removeFromSuperview(); child.removeFromParent() }
    }
    deinit { observers.forEach { NotificationCenter.default.removeObserver($0) } }
    #if DEBUG
    private func startDemoFrames() {
        let label = UILabel(); label.text = "카메라 UI 테스트"; label.textColor = .white; label.textAlignment = .center
        preview.addSubview(label); label.frame = CGRect(x: 0, y: 30, width: 280, height: 44)
        var optionalBuffer: CVPixelBuffer?
        let attributes: [String: Any] = [kCVPixelBufferMetalCompatibilityKey as String: true,
                                       kCVPixelBufferIOSurfacePropertiesKey as String: [:]]
        guard CVPixelBufferCreate(kCFAllocatorDefault, 480, 640, kCVPixelFormatType_32BGRA,
                                  attributes as CFDictionary, &optionalBuffer) == kCVReturnSuccess,
              let buffer = optionalBuffer else { report("테스트 프레임을 만들지 못했어요."); return }
        CVPixelBufferLockBaseAddress(buffer, [])
        if let canvas = CGContext(data: CVPixelBufferGetBaseAddress(buffer), width: 480, height: 640,
                                  bitsPerComponent: 8, bytesPerRow: CVPixelBufferGetBytesPerRow(buffer),
                                  space: CGColorSpaceCreateDeviceRGB(),
                                  bitmapInfo: CGBitmapInfo.byteOrder32Little.rawValue | CGImageAlphaInfo.premultipliedFirst.rawValue) {
            canvas.setFillColor(red: 0.7, green: 0.3, blue: 0.15, alpha: 1)
            canvas.fill(CGRect(x: 0, y: 0, width: 480, height: 640))
            canvas.setFillColor(red: 0.15, green: 0.45, blue: 0.8, alpha: 1)
            canvas.fill(CGRect(x: 0, y: 320, width: 480, height: 320))
        }
        CVPixelBufferUnlockBaseAddress(buffer, [])
        state.configure(minimum: 0.5, maximum: 6, initial: 1)
        let timer = DispatchSource.makeTimerSource(queue: frameQueue)
        timer.schedule(deadline: .now(), repeating: 1.0 / 30)
        timer.setEventHandler { [weak self] in self?.preview.offer(buffer) }
        demoTimer = timer; timer.resume()
    }
    #endif
    private enum CameraError: Error { case unavailable }
}

/** Camera buffers remain GPU resources. One frame renders while only the newest waits. */
private final class CameraMetalPreview: MTKView, MTKViewDelegate {
    private let imageContext: CIContext?
    private let commands: MTLCommandQueue?
    private let outputSpace = CGColorSpace(name: CGColorSpace.sRGB)!
    private let frameLock = NSLock()
    // These fields are protected by frameLock; UIKit state and film parameters are main-thread only.
    private var latestBuffer: CVPixelBuffer?
    private var drawScheduled = false
    private var inFlight = false
    private var acceptingFrames = true
    private var needsReadySignal = true
    private var epoch = 0
    private var filmParameters: [String: Any]?
    var onFramePresented: (() -> Void)?
    var onFailure: (() -> Void)?
    var isAvailable: Bool { commands != nil && imageContext != nil }

    init() {
        let gpu = MTLCreateSystemDefaultDevice()
        commands = gpu?.makeCommandQueue()
        imageContext = gpu.map { CIContext(mtlDevice: $0, options: [.workingColorSpace: NSNull(), .cacheIntermediates: false]) }
        super.init(frame: .zero, device: gpu)
        framebufferOnly = false // Core Image writes the drawable texture directly.
        colorPixelFormat = .bgra8Unorm
        // MTKView.colorspace is macOS-only; tag the backing layer instead so sRGB output is explicit.
        (layer as? CAMetalLayer)?.colorspace = outputSpace
        isPaused = true
        enableSetNeedsDisplay = false
        autoResizeDrawable = false
        delegate = self
        isUserInteractionEnabled = false
    }
    required init(coder: NSCoder) { fatalError("init(coder:) has not been implemented") }
    override func layoutSubviews() {
        super.layoutSubviews()
        guard bounds.width > 0, bounds.height > 0 else { return }
        let pixels = window?.screen.scale ?? 2
        let scale = min(pixels, 1280 / max(bounds.width, bounds.height))
        let target = CGSize(width: max(1, (bounds.width * scale).rounded()), height: max(1, (bounds.height * scale).rounded()))
        if drawableSize != target { drawableSize = target }
    }
    /// The live preview runs the film's shared 4x5 matrix through Core Image, so it matches the saved photo.
    func selectFilm(_ film: Film) {
        guard let matrix = film.matrix else { filmParameters = nil; return }
        func vector(_ offset: Int32) -> CIVector {
            CIVector(x: CGFloat(matrix.get(index: offset)), y: CGFloat(matrix.get(index: offset + 1)),
                     z: CGFloat(matrix.get(index: offset + 2)), w: CGFloat(matrix.get(index: offset + 3)))
        }
        // Core Image bias is normalized; the shared matrix keeps offsets in 0..255.
        filmParameters = ["inputRVector": vector(0), "inputGVector": vector(5),
                          "inputBVector": vector(10), "inputAVector": vector(15),
                          "inputBiasVector": CIVector(x: CGFloat(matrix.get(index: 4)) / 255,
                                                      y: CGFloat(matrix.get(index: 9)) / 255,
                                                      z: CGFloat(matrix.get(index: 14)) / 255,
                                                      w: CGFloat(matrix.get(index: 19)) / 255)]
    }
    func offer(_ buffer: CVPixelBuffer) {
        frameLock.lock()
        guard acceptingFrames else { frameLock.unlock(); return }
        latestBuffer = buffer // Replace a stale waiting frame instead of queuing image conversions.
        let shouldDraw = !drawScheduled && !inFlight
        if shouldDraw { drawScheduled = true }
        frameLock.unlock()
        if shouldDraw { scheduleDraw() }
    }
    private func scheduleDraw() {
        DispatchQueue.main.async { [weak self] in self?.draw() }
    }
    func suspend() {
        frameLock.lock()
        acceptingFrames = false; latestBuffer = nil; needsReadySignal = true; epoch += 1
        frameLock.unlock()
    }
    func resume() {
        frameLock.lock(); acceptingFrames = true; frameLock.unlock()
    }
    func mtkView(_ view: MTKView, drawableSizeWillChange size: CGSize) {}
    func draw(in view: MTKView) {
        frameLock.lock()
        drawScheduled = false
        guard acceptingFrames, !inFlight, let buffer = latestBuffer else { frameLock.unlock(); return }
        latestBuffer = nil; inFlight = true
        let frameEpoch = epoch
        frameLock.unlock()
        guard window != nil, UIApplication.shared.applicationState == .active,
              drawableSize.width > 0, drawableSize.height > 0,
              let imageContext, let command = commands?.makeCommandBuffer(), let drawable = currentDrawable else {
            finishFrame(epoch: frameEpoch, presented: false); return
        }
        autoreleasepool {
            var image = CIImage(cvPixelBuffer: buffer)
            if let filmParameters { image = image.applyingFilter("CIColorMatrix", parameters: filmParameters) }
            let target = CGRect(x: 0, y: 0, width: drawable.texture.width, height: drawable.texture.height)
            let scale = min(target.width / image.extent.width, target.height / image.extent.height)
            image = image.transformed(by: CGAffineTransform(scaleX: scale, y: scale))
            image = image.transformed(by: CGAffineTransform(translationX: (target.width - image.extent.width) / 2,
                                                           y: (target.height - image.extent.height) / 2))
            image = image.composited(over: CIImage(color: .black).cropped(to: target))
            imageContext.render(image, to: drawable.texture, commandBuffer: command, bounds: target, colorSpace: outputSpace)
            command.present(drawable)
            command.addCompletedHandler { [weak self, buffer] result in
                // Retain the camera buffer until its GPU reads finish; never block the capture queue.
                withExtendedLifetime(buffer) {
                    self?.finishFrame(epoch: frameEpoch, presented: result.status == .completed, failed: result.status == .error)
                }
            }
            command.commit()
        }
    }
    private func finishFrame(epoch frameEpoch: Int, presented: Bool, failed: Bool = false) {
        frameLock.lock()
        inFlight = false
        let current = acceptingFrames && epoch == frameEpoch
        let signalReady = current && presented && needsReadySignal
        if signalReady { needsReadySignal = false }
        if current && failed { acceptingFrames = false; latestBuffer = nil }
        let shouldDraw = acceptingFrames && latestBuffer != nil && !drawScheduled
        if shouldDraw { drawScheduled = true }
        frameLock.unlock()
        if signalReady || (current && failed) {
            DispatchQueue.main.async { [weak self] in
                guard let self else { return }
                self.frameLock.lock(); let valid = self.epoch == frameEpoch; self.frameLock.unlock()
                guard valid else { return }
                if failed { self.onFailure?() } else { self.onFramePresented?() }
            }
        }
        if shouldDraw { scheduleDraw() }
    }
}
