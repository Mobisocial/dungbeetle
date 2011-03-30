package edu.stanford.mobisocial.dungbeetle.util;
import java.io.IOException;
import java.io.FileOutputStream;
import java.io.File;
import android.os.Environment;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.InputStream;
import android.os.AsyncTask;

public class HTTPDownloadFileToExternalTask extends AsyncTask<String, Void, String> {
    @Override
    public String doInBackground(String... urls) {
        for (String url : urls) {
            try {
                URL apkUrl = new URL(url);
                HttpURLConnection c = (HttpURLConnection) apkUrl.openConnection();
                c.setRequestMethod("GET");
                c.setDoOutput(true);
                c.connect();

                String PATH = Environment.getExternalStorageDirectory() + "/download/";
                File file = new File(PATH);
                file.mkdirs();
                File outputFile = new File(file, "app.apk");
                FileOutputStream fos = new FileOutputStream(outputFile);

                InputStream is = c.getInputStream();

                byte[] buffer = new byte[1024];
                int len1 = 0;
                while ((len1 = is.read(buffer)) != -1) {
                    fos.write(buffer, 0, len1);
                }
                fos.close();
                is.close();
                return outputFile.getPath();
            } catch (IOException e) {
            }
        }
        return null;
    }
}