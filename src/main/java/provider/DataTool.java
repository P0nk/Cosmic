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

import provider.wz.DataType;

import java.awt.*;

public class DataTool {
    public static String getString(Data data) {
        return ((String) data.getData());
    }

    public static String getString(Data data, String def) {
        if (data == null || data.getData() == null) {
            return def;
        } else {
            return ((String) data.getData());
        }
    }

    public static String getString(String path, Data data) {
        return getString(data.getChildByPath(path));
    }

    public static String getString(String path, Data data, String def) {
        return getString(data.getChildByPath(path), def);
    }

    public static double getDouble(Data data) {
        return (Double) data.getData();
    }

    public static float getFloat(Data data) {
        return (Float) data.getData();
    }

    public static int getInt(Data data) {
        if (data == null || data.getData() == null) {
            return 0;// DEF?
        }
        return (Integer) data.getData();
    }

    public static long getLong(Data data) {
        if (data == null || data.getData() == null) {
            return 0;// DEF?
        }
        return (Long) data.getData();
    }

    public static int getInt(String path, Data data) {
        return getInt(data.getChildByPath(path));
    }

    public static int getIntConvert(Data data) {
        if (data.getType() == DataType.STRING) {
            return Integer.parseInt(getString(data));
        } else {
            return getInt(data);
        }
    }

    public static int getIntConvert(Data data, int def) {
        if (data == null) {
            return def;
        }
        if (data.getType() == DataType.STRING) {
            String dd = getString(data);
            if (dd.endsWith("%")) {
                dd = dd.substring(0, dd.length() - 1);
            }
            try {
                return Integer.parseInt(dd);
            } catch (NumberFormatException nfe) {
                return def;
            }
        } else {
            return getInt(data, def);
        }
    }

    public static int getIntConvert(String path, Data data) {
        Data d = data.getChildByPath(path);
        if (d.getType() == DataType.STRING) {
            return Integer.parseInt(getString(d));
        } else {
            return getInt(d);
        }
    }

    public static long getLongConvert(String path, Data data) {
        Data d = data.getChildByPath(path);
        if (d.getType() == DataType.STRING) {
            return Long.parseLong(getString(d));
        } else {
            return getLong(d);
        }
    }

    public static int getInt(Data data, int def) {
        if (data == null || data.getData() == null) {
            return def;
        } else if (data.getType() == DataType.STRING) {
            return Integer.parseInt(getString(data));
        } else {
            Object numData = data.getData();
            if (numData instanceof Integer) {
                return (Integer) numData;
            } else {
                return (Short) numData;
            }
        }
    }

    public static int getInt(String path, Data data, int def) {
        return getInt(data.getChildByPath(path), def);
    }

    public static int getIntConvert(String path, Data data, int def) {
        if (data == null || path == null) {
            return def;
        }

        Data d = data.getChildByPath(path);
        if (d == null || d.getType() == null) {
            return def;
        }

        try {
            if (d.getType() == DataType.STRING) {
                return Integer.parseInt(getString(d));
            } else {
                return getInt(d, def);
            }
        } catch (NumberFormatException nfe) {
            nfe.printStackTrace();
            return def;
        }
    }

    public static Point getPoint(Data data) {
        return ((Point) data.getData());
    }

    public static Point getPoint(String path, Data data) {
        return getPoint(data.getChildByPath(path));
    }

    public static Point getPoint(String path, Data data, Point def) {
        final Data pointData = data.getChildByPath(path);
        if (pointData == null) {
            return def;
        }
        return getPoint(pointData);
    }

    public static String getFullDataPath(Data data) {
        String path = "";
        DataEntity myData = data;
        while (myData != null) {
            path = myData.getName() + "/" + path;
            myData = myData.getParent();
        }
        return path.substring(0, path.length() - 1);
    }
}
