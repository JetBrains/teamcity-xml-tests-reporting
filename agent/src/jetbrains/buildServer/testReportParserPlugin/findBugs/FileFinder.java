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
    if (filePath.contains("$") && filePath.endsWith(".java")) {
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

  private static String getOSPath(String path) {
    return path.replace("\\", File.separator).replace("/", File.separator);
  }

  private static String getDependentPath(String path, String separator) {
    return path.replace("\\", separator).replace("/", separator);
  }

  private static interface Entry {
    String getFilePath(String fileName);
  }

  private static class DirectoryEntry implements Entry {
    private final String myRoot;

    public DirectoryEntry(String root) {
      myRoot = root;
    }

    public String getFilePath(String fileName) {
//      File file = new File(myRoot + File.separator + fileName);
//      if (file.exists()) {
//        return file.getAbsolutePath();
//      }
//      return null;
      return getFilePathRecursive(new File(myRoot).listFiles(), getOSPath(fileName));
    }

    private String getFilePathRecursive(File[] files, String relativePath) {
      if (files == null) {
        return null;
      }
      for (int i = 0; i < files.length; ++i) {
        if (files[i].isFile()) {
          final String path = files[i].getAbsolutePath();
          if (path.endsWith(relativePath)) {
            return path;
          }
        } else if (files[i].isDirectory()) {
          final String path = getFilePathRecursive(files[i].listFiles(), relativePath);
          if (path != null) {
            return path;
          }
        }
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
      final Enumeration<? extends ZipEntry> e = myArchive.entries();
      while (e.hasMoreElements()) {
        final String entry = e.nextElement().getName();
        if (entry.endsWith(getDependentPath(fileName, "/"))) {
          return myArchive.getName() + File.separator + getOSPath(entry);
        }
      }
      return null;
    }
  }

  private static class ClassEntry implements Entry {
    private final String myFile;

    public ClassEntry(String file) {
      myFile = file;
    }

    public String getFilePath(String fileName) {
      if (myFile.endsWith(getOSPath(fileName))) {
        return myFile;
      }
      return null;
    }
  }
}
