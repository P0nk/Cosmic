/*
	This file is part of the OdinMS Maple Story Server
    Copyright (C) 2008 Patrick Huy <patrick.huy@frz.cc>
		       Matthias Butz <matze@odinms.de>
		       Jan Christian Meyer <vimes@odinms.de>

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as
    published by the Free Software Foundation version 3 as published by
    the Free Software Foundation. You may not use, modify or distribute
    this program under any other version of the GNU Affero General Public
    License.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/
package provider.wz;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import provider.Data;
import provider.DataDirectoryEntry;
import provider.DataProvider;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class XMLWZFile implements DataProvider {
	private static final Logger log = LoggerFactory.getLogger(DataProvider.class);
	private final Path root;
    private final WZDirectoryEntry rootForNavigation;

    public XMLWZFile(Path fileIn) {
        root = fileIn;
        rootForNavigation = new WZDirectoryEntry(fileIn.getFileName().toString(), 0, 0, null);
        fillMapleDataEntitys(root, rootForNavigation);
    }

    private void fillMapleDataEntitys(Path lroot, WZDirectoryEntry wzdir) {

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(lroot)) {
            for (Path path : stream) {
                String fileName = path.getFileName().toString();
                if (Files.isDirectory(path) && !fileName.endsWith(".img")) {
                    WZDirectoryEntry newDir = new WZDirectoryEntry(fileName, 0, 0, wzdir);
                    wzdir.addDirectory(newDir);
                    fillMapleDataEntitys(path, newDir);
                } else if (fileName.endsWith(".xml")) {
                    wzdir.addFile(new WZFileEntry(fileName.substring(0, fileName.length() - 4), 0, 0, wzdir));
                }
            }
        } catch (IOException e) {
            log.warn("Can not open file/directory at " + lroot.toAbsolutePath().toString());
        }
    }

    @Override
    public synchronized Data getData(String path) {
        Path dataFile = root.resolve(path + ".xml");
        Path imageDataDir = root.resolve(path);
        if (!Files.exists(dataFile)) {
            return null;
        }
        final XMLDomMapleData domMapleData;
        try (FileInputStream fis = new FileInputStream(dataFile.toString())) {
            domMapleData = new XMLDomMapleData(fis, imageDataDir.getParent());
        } catch (FileNotFoundException e) {
            throw new RuntimeException("Datafile " + path + " does not exist in " + root.toAbsolutePath());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return domMapleData;
    }

	@Override
	public DataDirectoryEntry getRoot() {
		return rootForNavigation;
	}
}