// ------------------------------------------------------------------------ //
// Copyright 2016 Nagoya Institute of Technology                            //
//                                                                          //
// Licensed under the Apache License, Version 2.0 (the "License");          //
// you may not use this file except in compliance with the License.         //
// You may obtain a copy of the License at                                  //
//                                                                          //
//     http://www.apache.org/licenses/LICENSE-2.0                           //
//                                                                          //
// Unless required by applicable law or agreed to in writing, software      //
// distributed under the License is distributed on an "AS IS" BASIS,        //
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. //
// See the License for the specific language governing permissions and      //
// limitations under the License.                                           //
// ------------------------------------------------------------------------ //

package jp.ac.nitech.sp.voist;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import javax.activation.FileTypeMap;

public class FileUpload {
  //
  private HashMap<String, String> data;

  //
  private String requestUrl;

  public FileUpload(String url) {
    data = new HashMap<String, String>();
    requestUrl = url;
  }

  public void setRequestUrl(String url) {
    requestUrl = url;
  }

  public String getRequestUrl() {
    return requestUrl;
  }

  public void put(String key, String value) {
    data.put(key, value);
  }

  public boolean upload(File file, String seed) {
    if (file == null) {
      return false;
    }

    if (!file.exists() || file.isDirectory()) {
      return false;
    }

    String key = createKey(seed);
    if (key == null) {
      return false;
    }

    data.put("file", file.getAbsolutePath());
    data.put("key", key);

    return post(data);
  }

  private String createKey(String seed) {
    MessageDigest md;
    try {
      md = MessageDigest.getInstance("SHA-256");
      md.update(seed.getBytes());
    } catch (NoSuchAlgorithmException e) {
      e.printStackTrace();
      return null;
    }

    byte[] hash = md.digest();
    StringBuffer sb = new StringBuffer();
    int count = hash.length;
    for (int i = 0; i < count; i++) {
      sb.append(Integer.toHexString((hash[i] >> 4) & 0x0F));
      sb.append(Integer.toHexString(hash[i] & 0x0F));
    }

    return sb.toString();
  }

  public static String calculateChecksum(String filename) {
    MessageDigest md;
    try {
      md = MessageDigest.getInstance("MD5");
      FileInputStream fis = new FileInputStream(filename);
      byte[] dataBytes = new byte[1024];
      int nread = 0;
      while ((nread = fis.read(dataBytes)) != -1) {
        md.update(dataBytes, 0, nread);
      }
      fis.close();
    } catch (NoSuchAlgorithmException e) {
      e.printStackTrace();
      return null;
    } catch (FileNotFoundException e) {
      e.printStackTrace();
      return null;
    } catch (IOException e) {
      e.printStackTrace();
      return null;
    }

    byte[] hash = md.digest();
    StringBuffer sb = new StringBuffer();
    int count = hash.length;
    for (int i = 0; i < count; i++) {
      sb.append(Integer.toHexString((hash[i] >> 4) & 0x0F));
      sb.append(Integer.toHexString(hash[i] & 0x0F));
    }

    return sb.toString();
  }

  private boolean post(HashMap<String, String> postData) {
    final String twoHyphens = "--";
    final String eol = "\r\n";
    final String boundary = String.format("%x", new Random().hashCode());
    final String charset = "UTF-8";
    final int bufferSize = 1024;

    // Form contents will be sent to the server.
    StringBuilder contentsBuilder = new StringBuilder();

    for (Map.Entry<String, String> data : postData.entrySet()) {
      String key = data.getKey();
      String val = data.getValue();

      if (!new File(val).isFile()) {
        contentsBuilder.append(String.format("%s%s%s", twoHyphens, boundary, eol));
        contentsBuilder.append(
            String.format("Content-Disposition: form-data; name=\"%s\"%s", key, eol));
        contentsBuilder.append(eol);
        contentsBuilder.append(val);
        contentsBuilder.append(eol);
      }
    }

    File file = null;
    for (Map.Entry<String, String> data : postData.entrySet()) {
      String key = data.getKey();
      String val = data.getValue();

      if (new File(val).isFile()) {
        contentsBuilder.append(String.format("%s%s%s", twoHyphens, boundary, eol));
        contentsBuilder.append(
            String.format(
                "Content-Disposition: form-data; name=\"%s\"; filename=\"%s\"%s", key, val, eol));
        file = new File(val);
        break;
      }
    }

    long contentLength = 0;
    if (file == null) {
      contentsBuilder.append(String.format("Content-Type: application/octet-stream%s", eol));
    } else {
      // Set information about file.
      contentLength = file.length();

      // Get and set MIME.
      FileTypeMap filetypeMap = FileTypeMap.getDefaultFileTypeMap();
      String mime = filetypeMap.getContentType(file.getAbsolutePath());
      contentsBuilder.append(String.format("Content-Type: %s%s", mime, eol));
    }

    contentsBuilder.append(eol);
    String closingContents =
        String.format("%s%s%s%s%s", eol, twoHyphens, boundary, twoHyphens, eol);

    // Get contents length.
    try {
      contentLength += contentsBuilder.toString().getBytes(charset).length;
      contentLength += closingContents.getBytes(charset).length;
    } catch (UnsupportedEncodingException e) {
      e.printStackTrace();
    }

    HttpURLConnection connection = null;
    DataOutputStream dos = null;
    BufferedReader br = null;

    // Try to connect the server.
    VoistUtils.info("Connecting server");
    try {
      URL url = URI.create(requestUrl).toURL();

      connection = (HttpURLConnection) url.openConnection();
      connection.setDoInput(true);
      connection.setDoOutput(true);
      connection.setUseCaches(false); // Do not use cache.
      connection.setFixedLengthStreamingMode(contentLength);
      connection.setRequestMethod("POST");
      connection.setRequestProperty("Connection", "Keep-Alive");
      connection.setRequestProperty(
          "Content-Type", String.format("multipart/form-data; boundary=%s", boundary));
      connection.setRequestProperty("Content-Length", String.valueOf(contentLength));
      connection.setConnectTimeout(10 * 1000);
      connection.setReadTimeout(10 * 1000);

      // Send data.
      dos = new DataOutputStream(connection.getOutputStream());
      dos.write(contentsBuilder.toString().getBytes(charset));

      FileInputStream fis = null;
      if (file != null) {
        byte[] buffer = new byte[bufferSize];
        fis = new FileInputStream(file);
        int readLength = fis.read(buffer, 0, bufferSize);
        while (readLength > 0) {
          dos.write(buffer, 0, readLength);
          readLength = fis.read(buffer, 0, bufferSize);
        }
        fis.close();
      }

      dos.writeBytes(closingContents);
      dos.flush();
      dos.close();

      // Get response.
      int responseCode = connection.getResponseCode();

      if (responseCode == HttpURLConnection.HTTP_OK) {
        VoistUtils.info("Connection success: " + String.valueOf(responseCode));

        br = new BufferedReader(new InputStreamReader(connection.getInputStream()));

        String line;
        boolean error = false;
        while ((line = br.readLine()) != null) {
          VoistUtils.info("Response -> " + line);
          if (line.contains("error")) {
            error = true;
          }
        }

        br.close();
        return !error;
      } else {
        VoistUtils.info("Connection failure: " + String.valueOf(responseCode));
        return false;
      }
    } catch (MalformedURLException e) {
      e.printStackTrace();
      return false;
    } catch (IOException e) {
      e.printStackTrace();
      return false;
    } finally {
      if (connection != null) {
        connection.disconnect();
      }
    }
  }
}
