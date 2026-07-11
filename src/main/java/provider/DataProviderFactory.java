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
package provider;

import provider.wz.WZFiles;
import provider.wz.WzImgFile;

/**
 * Returns a {@link DataProvider} for one of the standard v83 WZ roots ({@code Item}, {@code Map}, ...).
 * <p>
 * Backed by {@link WzImgFile}, which transparently serves both binary {@code *.img} files
 * and HaRepacker-exported {@code *.img.xml} files from the same folder tree. When both forms
 * are present at the same path, the binary {@code .img} wins.
 */
public class DataProviderFactory {
    public static DataProvider getDataProvider(WZFiles in) {
        return new WzImgFile(in.getFile().toFile());
    }
}
