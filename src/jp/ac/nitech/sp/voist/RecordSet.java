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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Objects;
import javax.swing.JOptionPane;
import javax.swing.SwingWorker;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import jp.ac.nitech.sp.voist.Audio.PlaybackEvent;
import net.reduls.igo.Tagger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

public class RecordSet {
  //
  private final PropertiesIO properties;

  //
  private final String saveDir;

  //
  private final String sampleDir;

  //
  private final ArrayList<Prompt> prompts;

  //
  private final ArrayList<RecordInfo> info;

  //
  private final RecordInfo tmpInfo;

  //
  private Tagger tagger;

  //
  private int position;

  //
  private int numRecordedPrompts;

  //
  private String userName;

  //
  private String promptSetName;

  //
  private int sampleRate;

  //
  private int sampleSize;

  public RecordSet(
      String propertiesFileName, String saveDir, String appDir, int sampleRate, int sampleSize) {
    Objects.requireNonNull(propertiesFileName);
    Objects.requireNonNull(saveDir);
    Objects.requireNonNull(appDir);

    properties = new PropertiesIO(propertiesFileName);
    try {
      properties.load();
    } catch (IOException e) {
      e.printStackTrace();
    }

    position = 0;
    numRecordedPrompts = 0;
    userName = null;
    promptSetName = null;
    this.saveDir = saveDir;
    sampleDir = FileUtils.createPath(appDir, properties.getString("DIR_SAMPLE"));
    setSampleRate(sampleRate);
    setSampleSize(sampleSize);

    prompts = new ArrayList<Prompt>(512);
    info = new ArrayList<RecordInfo>(512);
    tmpInfo =
        new RecordInfo(
            FileUtils.createPath(
                getRecordingDirectoryName(PlaybackEvent.VOICE_TMP),
                properties.getString("TMP_ORG_FILE")),
            FileUtils.createPath(
                getRecordingDirectoryName(PlaybackEvent.VOICE_TMP),
                properties.getString("TMP_CUT_FILE")),
            null,
            null);
    tmpInfo.toTmp(true);

    try {
      tagger = new Tagger(properties.getString("DIR_DIC"));
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public void dispose() {
    deleteTmpFile();
    clear();
    tagger = null;
  }

  public void clear() {
    prompts.clear();
    info.clear();
  }

  public void setSampleRate(final int sampleRate) {
    this.sampleRate = sampleRate;
  }

  public void setSampleSize(final int sampleSize) {
    this.sampleSize = sampleSize;
  }

  public int getSampleRate() {
    return sampleRate;
  }

  public int getSampleSize() {
    return sampleSize;
  }

  public boolean setPosition(int position) {
    if (position < 0 || prompts.size() <= position) {
      return false;
    }
    this.position = position;
    return true;
  }

  public void setUserName(String userName) {
    this.userName = userName;
  }

  public int getPosition() {
    return position;
  }

  public String getUserName() {
    return userName;
  }

  public int getNumPrompts() {
    return prompts.size();
  }

  public int getNumRecordedPrompts() {
    return numRecordedPrompts;
  }

  public void incNumRecordedPrompts() {
    numRecordedPrompts++;
  }

  public Prompt getPrompt() {
    return this.getPrompt(position);
  }

  public Prompt getPrompt(int index) {
    return (0 <= index && index < prompts.size()) ? prompts.get(index) : null;
  }

  public String getPromptSetName() {
    return promptSetName;
  }

  public String getFileName() {
    return getFileName(position);
  }

  public String getFileName(int index) {
    Prompt prompt = getPrompt(index);
    return (prompt == null) ? "" : prompt.getFullPromptName("raw");
  }

  public String getPromptID() {
    return getPromptID(position);
  }

  public String getPromptID(int index) {
    Prompt prompt = getPrompt(index);
    return (prompt == null) ? "" : prompt.getSimplePromptName();
  }

  public RecordInfo getRecordInfo() {
    return getRecordInfo(position);
  }

  public RecordInfo getRecordInfo(int index) {
    return (0 <= index && index < info.size()) ? info.get(index) : null;
  }

  public RecordInfo getRecordTmpInfo() {
    return tmpInfo;
  }

  public String getRecordingDirectoryName(PlaybackEvent e) {
    if (e == null) {
      return FileUtils.createPath(saveDir, userName);
    }

    switch (e) {
      case BEEP:
        return FileUtils.createPath(properties.getString("DIR_BEEP"));
      case SAMPLE:
        return FileUtils.createPath(sampleDir, promptSetName);
      case VOICE_ORG:
        return FileUtils.createPath(saveDir, userName, promptSetName, "rec_org");
      case VOICE_CUT:
        return FileUtils.createPath(saveDir, userName, promptSetName, "rec_cut");
      case VOICE_TMP:
        return FileUtils.createPath(saveDir);
      case VOICE_WAV:
        return FileUtils.createPath(saveDir, userName, promptSetName, "upload", "wav");
      default:
        return null;
    }
  }

  public int checkComplete(boolean ascending) {
    if (ascending) {
      for (int i = position + 1; i < getNumPrompts(); i++) {
        if (!getRecordInfo(i).isRecorded()) {
          return i;
        }
      }
      for (int i = 0; i <= position; i++) {
        if (!getRecordInfo(i).isRecorded()) {
          return i;
        }
      }
    } else {
      for (int i = position - 1; i >= 0; i--) {
        if (!getRecordInfo(i).isRecorded()) {
          return i;
        }
      }
      for (int i = getNumPrompts() - 1; i >= position; i--) {
        if (!getRecordInfo(i).isRecorded()) {
          return i;
        }
      }
    }
    return -1;
  }

  public void dumpLog() {
    getRecordInfo().dump();
  }

  public void overwrite() {
    RecordInfo src = tmpInfo;
    RecordInfo dest = info.get(position);

    dest.setMaxAmplitude(src.getMaxAmplitude());
    dest.setTopSilence(src.getTopSilence());
    dest.setEndSilence(src.getEndSilence());
    dest.setPower(src.getPower());
    dest.incrementNumRetakes();

    VoiceFileWriter writer = new VoiceFileWriter();
    writer.setSourceRecordInfo(src);
    writer.setDestinationRecordInfo(dest);
    writer.execute();
  }

  public void deleteTmpFile() {
    FileUtils.delete(tmpInfo.getVoiceOrgFile());
    FileUtils.delete(tmpInfo.getVoiceCutFile());
  }

  public boolean read(String fileName, boolean consoleOutput) {
    // Clear information about previous prompts.
    clear();

    try {
      // Setup instances to read the given XML file.
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      factory.setValidating(true);
      factory.setAttribute(
          "http://java.sun.com/xml/jaxp/properties/schemaLanguage",
          XMLConstants.W3C_XML_SCHEMA_NS_URI);
      factory.setAttribute(
          "http://java.sun.com/xml/jaxp/properties/schemaSource",
          new File(properties.getString("XSD_FILE")));
      DocumentBuilder builder = factory.newDocumentBuilder();
      XMLErrorHandler handler = new XMLErrorHandler();
      builder.setErrorHandler(handler);

      File file = new File(fileName);
      if (FileUtils.canRead(file)) {
        String s = file.getName();
        promptSetName = s.substring(0, s.lastIndexOf("."));
        Document doc = builder.parse(file);
        Element root = doc.getDocumentElement();
        NodeList children = root.getChildNodes();

        // Parse the XML file.
        int numPrompts = 0;
        numRecordedPrompts = 0;
        for (int i = 0; i < children.getLength(); i++) {
          Node child = children.item(i);
          NodeList params = child.getChildNodes();

          if (child.getNodeType() != Node.ELEMENT_NODE) {
            continue;
          }

          String copyright = null;
          String language = null;
          String content = null;
          String style = null;
          String db = null;
          String id = null;
          String script = null;

          for (int j = 0; j < params.getLength(); j++) {
            Node param = params.item(j);
            Node c = param.getFirstChild();
            String paramName = param.getNodeName();

            if (param.getNodeType() == Node.ELEMENT_NODE && c != null) {
              if (paramName.equals("copyright")) {
                copyright = c.getNodeValue();
              } else if (paramName.equals("language")) {
                language = c.getNodeValue();
              } else if (paramName.equals("content")) {
                content = c.getNodeValue();
              } else if (paramName.equals("style")) {
                style = c.getNodeValue();
              } else if (paramName.equals("db")) {
                db = c.getNodeValue();
              } else if (paramName.equals("id")) {
                id = c.getNodeValue();
              } else if (paramName.equals("script")) {
                script = c.getNodeValue();
              }
            }
          }

          if (numPrompts + 1 >= properties.getInteger("MAX_NUM_PROMPTS")) {
            JOptionPane.showMessageDialog(
                null,
                properties.getString("WARNING_MESSAGE_TOO_MANY_PROMPTS"),
                properties.getString("WARNING_MESSAGE_TITLE"),
                JOptionPane.WARNING_MESSAGE);
            break;
          }

          if (consoleOutput) {
            System.out.println(script);
          }

          Prompt p = new Prompt();
          if (!p.set(copyright, language, content, style, db, id, script, tagger, consoleOutput)) {
            JOptionPane.showMessageDialog(
                null,
                "Unexpected prompt: " + "(" + id + ") " + script,
                properties.getString("ERROR_MESSAGE_TITLE"),
                JOptionPane.ERROR_MESSAGE);
            return false;
          }
          prompts.add(p);

          info.add(
              new RecordInfo(
                  FileUtils.createPath(
                      getRecordingDirectoryName(PlaybackEvent.VOICE_ORG),
                      p.getFullPromptName("raw")),
                  FileUtils.createPath(
                      getRecordingDirectoryName(PlaybackEvent.VOICE_CUT),
                      p.getFullPromptName("raw")),
                  FileUtils.createPath(
                      getRecordingDirectoryName(PlaybackEvent.VOICE_WAV),
                      p.getFullPromptName("wav")),
                  FileUtils.createPath(getLogDirectoryName(), p.getFullPromptName("log"))));
          info.get(numPrompts).load();

          if (info.get(numPrompts).isRecorded()) {
            numRecordedPrompts++;
          }
          numPrompts++;
        }
        makeDirectories();
        return true;
      }
    } catch (ParserConfigurationException e) {
      e.printStackTrace();
    } catch (SAXException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }

    return false;
  }

  private String getLogDirectoryName() {
    return FileUtils.createPath(saveDir, userName, promptSetName, "upload", "log");
  }

  private void makeDirectories() {
    File[] dirs =
        new File[] {
          new File(getLogDirectoryName()),
          new File(getRecordingDirectoryName(PlaybackEvent.VOICE_ORG)),
          new File(getRecordingDirectoryName(PlaybackEvent.VOICE_CUT)),
          new File(getRecordingDirectoryName(PlaybackEvent.VOICE_WAV)),
        };
    for (File dir : dirs) {
      if (!dir.exists()) {
        dir.mkdirs();
      }
    }
  }

  private class VoiceFileWriter extends SwingWorker<String, String> {
    private RecordInfo src;
    private RecordInfo dest;
    private WaveHeader waveHeader;

    public VoiceFileWriter() {
      src = null;
      dest = null;
      waveHeader = new WaveHeader(sampleRate, sampleSize, 1);
    }

    public void setSourceRecordInfo(RecordInfo info) {
      src = info;
    }

    public void setDestinationRecordInfo(RecordInfo info) {
      dest = info;
    }

    @Override
    protected String doInBackground() throws Exception {
      if (src != null && dest != null) {
        FileUtils.copy(src.getVoiceOrgFile(), dest.getVoiceOrgFile());
        FileUtils.copy(src.getVoiceCutFile(), dest.getVoiceCutFile());
        waveHeader.writeWavFromRaw(dest.getVoiceCutFile(), dest.getVoiceWavFile());
      }
      return null;
    }
  }

  private class XMLErrorHandler implements ErrorHandler {
    public XMLErrorHandler() {}

    public void warning(SAXParseException e) throws SAXException {
      System.out.println("Warning: ");
      printInfo(e);
    }

    public void error(SAXParseException e) throws SAXException {
      System.out.println("Error: ");
      printInfo(e);
    }

    public void fatalError(SAXParseException e) throws SAXException {
      System.out.println("Fattal error: ");
      printInfo(e);
    }

    private void printInfo(SAXParseException e) {
      String str =
          VoistUtils.joinStrings(
              null,
              properties.getString("ERROR_MESSAGE_INVALID_PROMPTS"),
              "\n\n",
              "   Public ID: ",
              e.getPublicId(),
              "\n",
              "   System ID: ",
              e.getSystemId(),
              "\n",
              "   Line number: " + e.getLineNumber(),
              "\n",
              "   Column number: " + e.getColumnNumber(),
              "\n",
              "   Message: ",
              e.getMessage());
      JOptionPane.showMessageDialog(
          null, str, properties.getString("ERROR_MESSAGE_TITLE"), JOptionPane.ERROR_MESSAGE);
    }
  }
}
