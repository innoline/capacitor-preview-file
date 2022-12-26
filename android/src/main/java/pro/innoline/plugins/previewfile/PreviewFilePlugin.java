package pro.innoline.plugins.previewfile;

import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

import java.io.IOException;
import java.net.URISyntaxException;

@CapacitorPlugin(name = "PreviewFile")
public class PreviewFilePlugin extends Plugin {

    private PreviewFile implementation;

    public void load() {
        implementation = new PreviewFile(this.getBridge());
    }

    @PluginMethod
    public void previewByPath(PluginCall call) throws URISyntaxException {
        String path = call.getString("path");
        String mimeType = call.getString("mimeType");
        String name = call.getString("name");

        implementation.previewByPath(call, path, mimeType, name);
    }

    @PluginMethod
    public void previewBase64(PluginCall call) throws URISyntaxException, IOException {
        String base64 = call.getString("base64");
        String mimeType = call.getString("mimeType");
        String name = call.getString("name");

        implementation.previewBase64(call, base64, mimeType, name);
    }
}
