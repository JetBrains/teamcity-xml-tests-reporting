package jetbrains.buildServer.testReportParserPlugin.findBugs;

import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;


public class FileFinder {
  private List<Entry> myJars;

  public FileFinder() {
    myJars = new LinkedList<Entry>();
  }

  public void addJars(List<String> jars) {
    for (String jar : jars) {
      if (jar.endsWith(".zip") || jar.endsWith(".jar")) {
        try {
          myJars.add(new ArchiveEntry(new ZipFile(jar)));
        } catch (IOException e) {
          //Ignored
          //TODO: log about this
        }
      } else if (jar.endsWith(".class")) {
        myJars.add(new ClassEntry(jar));
      } else {
        myJars.add(new DirectoryEntry(jar));
      }
    }
  }

  public String getVeryFullFilePath(String filePath) {
    if (filePath.contains("$")) {
      filePath = filePath.substring(0, filePath.indexOf("$")) + ".class";
    }
    for (Entry jar : myJars) {
      final String veryFullFilePath = jar.getFilePath(filePath);
      if (veryFullFilePath != null) {
        return veryFullFilePath;
      }
    }
    return "";
  }

  private static interface Entry {
    public String getFilePath(String fileName);
  }

  private static class DirectoryEntry implements Entry {
    private final String myRoot;

    public DirectoryEntry(String root) {
      myRoot = root;
    }

    public String getFilePath(String fileName) {
      File file = new File(myRoot + File.separator + fileName);
      if (file.exists()) {
        return file.getAbsolutePath();
      }
      return null;
    }
  }

  private static class ArchiveEntry implements Entry {
    private final ZipFile myArchive;

    public ArchiveEntry(ZipFile archive) {
      myArchive = archive;
    }

    public String getFilePath(String fileName) {
//      final List<String> pathElements = new ArrayList<String>();
//      while (true) {
//        if (!fileName.contains(File.separator)) {
//          break;
//        }
//        pathElements.add(fileName.substring(0, fileName.indexOf(File.separator)));
//        fileName = fileName.substring(fileName.indexOf(File.separator) + 1);
//      }
      final Enumeration<? extends ZipEntry> e = myArchive.entries();
      while (e.hasMoreElements()) {
        final String entry = e.nextElement().getName();
        if (entry.endsWith(fileName.replace(File.separator, "/"))) {
          return myArchive.getName() + File.separator + entry.replace("/", File.separator);
        }
//        if (!pathElements.contains(entry.getName())) {
//           return null;
//        }
      }
//      final ZipEntry e = myArchive.getEntry(fileName);
//      if (e != null) {
//        return myArchive.getName() + File.separator + e.getName();
//      }
      return null;
    }
  }

  private static class ClassEntry implements Entry {
    private final String myFile;

    public ClassEntry(String file) {
      myFile = file;
    }

    public String getFilePath(String fileName) {
      if (myFile.endsWith(fileName)) {
        return myFile;
      }
      return null;
    }
  }
}
