import Foundation
import Capacitor

@objc(PreviewFilePlugin)
public class PreviewFilePlugin: CAPPlugin {
    private var implementation: PreviewFile?

    override public func load() {
        self.implementation = PreviewFile(self)
    }

    @objc func previewByPath(_ call: CAPPluginCall) {
        let path = call.getString("path") ?? ""
        let mimeType = call.getString("mimeType") ?? ""
        let name = call.getString("name") ?? ""

        implementation?.previewByPath(call, path: path, mimeType: mimeType, name: name);
    }

    @objc func previewBase64(_ call: CAPPluginCall) {
        let base64 = call.getString("base64") ?? ""
        let mimeType = call.getString("mimeType") ?? ""
        let name = call.getString("name") ?? ""

        implementation?.previewBase64(call, base64: base64, type: mimeType, name: name);
    }
}
