#import <Foundation/Foundation.h>
#import <Capacitor/Capacitor.h>

CAP_PLUGIN(PreviewFilePlugin, "PreviewFile",
           CAP_PLUGIN_METHOD(previewByPath, CAPPluginReturnPromise);
           CAP_PLUGIN_METHOD(previewBase64, CAPPluginReturnPromise);
)
