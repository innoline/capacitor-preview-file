package pro.innoline.plugins.previewfile;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.util.Base64;
import android.webkit.MimeTypeMap;

import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.getcapacitor.Bridge;
import com.getcapacitor.PluginCall;

public class PreviewFile {
    private final Bridge bridge;
    private PluginCall savedCall;
    private String mimeType = null;

    private static boolean notEmpty(String what) {
        return what != null && !"".equals(what) && !"null".equalsIgnoreCase(what);
    }

    public PreviewFile(Bridge bridge) {
        this.bridge = bridge;
    }

    public void previewByPath(PluginCall call, String path, String mimeType, String name) throws URISyntaxException {
        if (notEmpty(mimeType))
            this.mimeType = mimeType;
        else
            this.mimeType = notEmpty(name) ? bathToMime(name) : bathToMime(path);

        savedCall = call;
        viewFile(pathToUri(path));
    }

    public void previewBase64(PluginCall call, String base64, String mimeType, String name) throws URISyntaxException, IOException {
        if (notEmpty(mimeType))
            this.mimeType = mimeType;

        savedCall = call;

        Uri savedFile = base64ToPath(base64, name);
        if (!Uri.EMPTY.equals(savedFile))
            viewFile(savedFile);
    }

    private String base64ToMime(final String encoded) {
        final Pattern mime = Pattern.compile("^data:([a-zA-Z0-9]+/[a-zA-Z0-9]+).*,.*");
        final Matcher matcher = mime.matcher(encoded);
        if (matcher.find())
            mimeType = Objects.requireNonNull(matcher.group(1)).toLowerCase();
        return mimeType;
    }

    private String bathToMime(String url) {

        String extension = MimeTypeMap.getFileExtensionFromUrl(url);
        if (notEmpty(extension))
            mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
        return mimeType;
    }

    private Uri pathToUri(String url) throws URISyntaxException {
        Uri uri = null;
        if (url.startsWith("file:")) {
            File file = new File(new URI(url));
            uri = uriFromFile(file);
        } else {
            uri = Uri.parse(url);
        }
        return uri;
    }

    private Uri uriFromFile(File file) {
      Uri uri = FileProvider.getUriForFile(this.bridge.getActivity(),
        this.bridge.getActivity().getApplicationContext().getPackageName() + ".fileprovider", file);
      return uri;
    }

    private void viewFile(Uri uri) {
        try {
            if (!notEmpty(mimeType))
                mimeType = "application/*";
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, mimeType);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            this.bridge.getActivity().startActivityForResult(intent, 1);
            this.savedCall.resolve();
        } catch (ActivityNotFoundException t) {
            if (Objects.requireNonNull(t.getLocalizedMessage()).toLowerCase().contains("no activity")
                    && !mimeType.equalsIgnoreCase("application/*")) {
                mimeType = "application/*";
                viewFile(uri);
            } else {
                this.savedCall.reject(t.getLocalizedMessage());
            }
        } catch (Exception t) {
          this.savedCall.reject(t.getLocalizedMessage());
        }
    }

    private Uri base64ToPath(String base64, String fileName) throws IOException {
        String dir = getDownloadDir();
        String localFile = null;
        String encodedBase64 = null;
        if (base64.startsWith("data:")) {
            // content is not a valid base64
            if (!base64.contains(";base64,")) {
                return null;
            }
            this.mimeType = base64ToMime(base64);
            // image looks like this: data:image/png;base64,R0lGODlhDAA...
            encodedBase64 = base64.substring(base64.indexOf(";base64,") + 8);

        } else {
            if (!notEmpty(this.mimeType))
                this.mimeType = bathToMime(fileName);
            encodedBase64 = base64;
        }

        if (!notEmpty(this.mimeType)) {
            savedCall.reject("You must specify either file name with extension or MimeType");
            return null;
        }

        if (!notEmpty(fileName)) {
            String ext = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType);
            fileName = System.currentTimeMillis() + "_file" + (notEmpty(ext) ? "." + ext : "");
        }
        //fileName = URLEncoder.encode(fileName, "UTF-8");

        int fileLength = fileName.length();
        if (fileLength > 120) {
          int index = fileName.lastIndexOf('.');
          if (index > 0) {
            fileName = fileName.substring(0, 120) + fileName.substring(index);
          } else {
            fileName = fileName.substring(0, 120);
          }
        }

        try {
            File file = saveFile(Base64.decode(encodedBase64, Base64.DEFAULT), dir, fileName);
            if (file.exists() && !file.isDirectory())
                return uriFromFile(file);
            else
                savedCall.reject("cannot write the base64 to a file");
        } catch (Exception e) {
            savedCall.reject(e.getMessage());
        }
        return null;
    }

    private File saveFile(byte[] bytes, String dirName, String fileName) throws IOException {
        final File dir = new File(dirName);
        final File file = new File(dir, fileName);
        final FileOutputStream fos = new FileOutputStream(file);
        fos.write(bytes);
        fos.flush();
        fos.close();
        return file;
    }

    private String getDownloadDir() throws IOException {
        // better check, otherwise it may crash the app
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            // we need to use external storage since we need to share to another app
            final String dir = bridge.getContext().getExternalFilesDir(null) + "/capacitor-preview-file";
            createOrCleanDir(dir);
            return dir;
        } else {
            return null;
        }
    }

    private void createOrCleanDir(final String downloadDir) throws IOException {
        final File dir = new File(downloadDir);
        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                throw new IOException("CREATE_DIRS_FAILED");
            }
        } else {
            cleanupOldFiles(dir);
        }
    }

    private void cleanupOldFiles(File dir) {
        for (File f : dir.listFiles()) {
            // noinspection ResultOfMethodCallIgnored
            f.delete();
        }
    }
}

