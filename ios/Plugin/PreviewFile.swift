import Foundation
import QuickLook
import CoreServices
import Capacitor

@objc public class PreviewFile: NSObject {
    lazy var previewItem = NSURL()
    private var plugin: PreviewFilePlugin?
    private var savedCall: CAPPluginCall?

    init(_ plugin: PreviewFilePlugin?) {
        super.init()
        self.plugin = plugin
    }

    @objc public func previewByPath(_ call: CAPPluginCall, path: String, mimeType: String, name: String) {
        savedCall = call
        var fileName = "";
        var ext:String = "";
        if(!name.isEmpty){
            fileName = name
        }else if(!mimeType.isEmpty){
            let uti = UTTypeCreatePreferredIdentifierForTag(kUTTagClassMIMEType, mimeType as CFString, nil);
            let NewExt = UTTypeCopyPreferredTagWithClass((uti?.takeRetainedValue())!, kUTTagClassFilenameExtension);
            ext = NewExt!.takeRetainedValue() as String;
            fileName = "file."+ext;
        }

        self.previewFile(path: path, name: fileName)
    }

    @objc public func previewBase64(_ call: CAPPluginCall, base64: String, type: String, name: String) {
        savedCall = call
        var base64String = base64
        var mimeType = type
        var fileName = "";
        var ext:String = "";

        if(base64String.isEmpty){
            call.reject("No Base64 code found")
            return;
        }else if(base64String.contains(";base64,")){
            let baseTmp = base64String.components(separatedBy: ",");
            base64String = baseTmp[1];
            mimeType = baseTmp[0].replacingOccurrences(of: "data:",with: "").replacingOccurrences(of: ";base64",with: "");
        }

        if(name.isEmpty && mimeType.isEmpty){
            call.reject("You must define file name or mime type")
            return;
        }

        if(!name.isEmpty){
            fileName = name
        }else if(!mimeType.isEmpty){
            let uti = UTTypeCreatePreferredIdentifierForTag(kUTTagClassMIMEType, mimeType as CFString, nil);
            let NewExt = UTTypeCopyPreferredTagWithClass((uti?.takeRetainedValue())!, kUTTagClassFilenameExtension);
            ext = NewExt!.takeRetainedValue() as String;
            fileName = "file."+ext;
        }

        guard
            var documentsURL = (FileManager.default.urls(for: .documentDirectory, in: .userDomainMask)).last,
            let convertedData = Data(base64Encoded: base64String)
        else {
            call.reject("base64 not valid")
            return
        }
        documentsURL.appendPathComponent(fileName)
        do {
            try convertedData.write(to: documentsURL)
        } catch {
            call.reject("cannot write the base64")
        }

        let docPath:String = documentsURL.path;

        self.previewFile(path: docPath, name: fileName)
    }

    func dismissPreviewCallback(){
        savedCall?.resolve()
    }

    func previewFile(path: String, name: String){
        self.downloadFile(withName: path, fileName: name, completion: {(success, fileLocationURL, callback) in
            if success {
                self.previewItem = fileLocationURL! as NSURL

                DispatchQueue.main.async(execute: {
                    let previewController = QLPreviewController();
                    previewController.dataSource = self;
                    previewController.delegate = self;
                    self.plugin?.bridge?.viewController?.present(previewController, animated: true, completion: nil);
                    if ((self.plugin?.bridge?.viewController?.isViewLoaded) != nil) {
                        self.savedCall?.resolve()
                    }
                    else{
                        self.savedCall?.reject("FAILED")
                    }
                });
            }else{
                self.savedCall?.reject(callback?.localizedDescription ?? "")
            }
        })
    }

    func downloadFile(withName myUrl: String,fileName:String,completion: @escaping (_ success: Bool,_ fileLocation: URL? , _ callback : NSError?) -> Void){
        let  url = myUrl.addingPercentEncoding(withAllowedCharacters:NSCharacterSet.urlQueryAllowed)!;
        var itemUrl: URL? = Foundation.URL(string: url);

        if FileManager.default.fileExists(atPath: itemUrl!.path) {

            if(itemUrl?.scheme == nil){
                itemUrl = Foundation.URL(string: "file://\(url)");
            }
            return completion(true, itemUrl,nil)
        }

        let documentsDirectoryURL =  FileManager.default.urls(for: .documentDirectory, in: .userDomainMask).first!
        var disFileName = "";
        if(fileName.isEmpty){
            disFileName = itemUrl?.lastPathComponent ?? "file.pdf";
        }else{
            disFileName = fileName;
        }
        let destinationUrl = documentsDirectoryURL.appendingPathComponent(disFileName);

        if FileManager.default.fileExists(atPath: destinationUrl.path) {
            do {
                try FileManager.default.removeItem(at: destinationUrl)
                //let error as NSError
            } catch let error as NSError  {
                completion(false, nil,error)
            }
        }
        let downloadTask = URLSession.shared.downloadTask(with: itemUrl!, completionHandler: { (location, response, error) -> Void in
            if error != nil{
                completion(false, nil, error as NSError?)
            }
            guard let tempLocation = location, error == nil else { return }
            do {
                try FileManager.default.moveItem(at: tempLocation, to: destinationUrl)
                completion(true, destinationUrl,nil)
                //let error as NSError
            } catch  let error as NSError  {
                completion(false, nil, error)
            }
        });

        downloadTask.resume();
    }
}

extension PreviewFile: QLPreviewControllerDataSource, QLPreviewControllerDelegate {
    public func numberOfPreviewItems(in controller: QLPreviewController) -> Int {
        return 1
    }

    public func previewController(_ controller: QLPreviewController, previewItemAt index: Int) -> QLPreviewItem {
        return self.previewItem as QLPreviewItem
    }

    public func previewControllerWillDismiss(_ controller: QLPreviewController) {
        self.dismissPreviewCallback();

    }
}

