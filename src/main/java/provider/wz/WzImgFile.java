package provider.wz;

import provider.Data;
import provider.DataDirectoryEntry;
import provider.DataProvider;
import provider.wz.binary.WzArchive;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * Hybrid data provider for v83-style WZ folders.
 * <p>
 * A folder root may contain a mix of binary {@code *.img} files (HaRepacker "Wz Image") and
 * HaRepacker-exported {@code *.img.xml} files. Both layouts are supported simultaneously, so
 * a contributor can swap any individual {@code .img} for its XML twin (or vice-versa) without
 * reorganizing the folder tree.
 * <p>
 * Resolution order for any given path is <b>binary first, XML as fallback</b>.
 */
public class WzImgFile implements DataProvider {
    private static final String XML_SUFFIX = ".xml";

    private final File root;
    private final WZDirectoryEntry rootForNavigation;

    public WzImgFile(File fileIn) {
        root = fileIn;
        rootForNavigation = new WZDirectoryEntry(fileIn.getName(), 0, 0, null);
        fillMapleDataEntitys(root, rootForNavigation);
    }

    private void fillMapleDataEntitys(File lroot, WZDirectoryEntry wzdir) {
        File[] files = lroot.listFiles();
        if (files == null) {
            return;
        }
        for (File file : files) {
            String fileName = file.getName();
            if (file.isDirectory() && !fileName.endsWith(".img")) {
                WZDirectoryEntry newDir = new WZDirectoryEntry(fileName, 0, 0, wzdir);
                wzdir.addDirectory(newDir);
                fillMapleDataEntitys(file, newDir);
            } else if (file.isFile() && fileName.endsWith(".img")) {
                // Binary IMG: register under its own name. Wins over any sibling .img.xml.
                if (wzdir.getEntry(fileName) == null) {
                    wzdir.addFile(new WZFileEntry(fileName, 0, 0, wzdir));
                }
            } else if (file.isFile() && fileName.endsWith(".img" + XML_SUFFIX)) {
                // XML fallback: register under the .img name only if no binary already claimed it.
                String imgName = fileName.substring(0, fileName.length() - XML_SUFFIX.length());
                if (wzdir.getEntry(imgName) == null && !new File(lroot, imgName).isFile()) {
                    wzdir.addFile(new WZFileEntry(imgName, 0, 0, wzdir));
                }
            }
        }
    }

    @Override
    public synchronized Data getData(String path) {
        File binary = new File(root, path);
        if (binary.isFile()) {
            try {
                WzArchive archive = WzArchive.from(binary);
                return new WzImgMapleData(binary.getName(), archive.getImage().getProperty());
            } catch (IOException e) {
                throw new RuntimeException("Failed to read img file " + binary.getAbsolutePath(), e);
            }
        }

        File xml = new File(root, path + XML_SUFFIX);
        if (xml.isFile()) {
            try (FileInputStream fis = new FileInputStream(xml)) {
                return new XMLDomMapleData(fis, xml.getParentFile().toPath());
            } catch (FileNotFoundException e) {
                return null;
            } catch (IOException e) {
                throw new RuntimeException("Failed to read xml file " + xml.getAbsolutePath(), e);
            }
        }

        return null;
    }

    @Override
    public DataDirectoryEntry getRoot() {
        return rootForNavigation;
    }
}
