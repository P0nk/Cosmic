/*
 *  This file is part of the OdinMS Maple Story Server
 *  Copyright (C) 2008 Patrick Huy <patrick.huy@frz.cc>
 *                 Matthias Butz <matze@odinms.de>
 *                 Jan Christian Meyer <vimes@odinms.de>
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as
 *  published by the Free Software Foundation version 3 as published by
 *  the Free Software Foundation. You may not use, modify or distribute
 *  this program under any other version of the GNU Affero General Public
 *  License.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package provider.wz;

import constants.game.GameConstants;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import provider.Data;
import provider.DataEntity;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.awt.*;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class XMLDomMapleData implements Data {
    private final Node node;
    private Path imageDataDir;

    public XMLDomMapleData(FileInputStream fis, Path imageDataDir) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(fis);
            this.node = document.getFirstChild();
        } catch (ParserConfigurationException | SAXException | IOException e) {
            throw new RuntimeException(e);
        }
        this.imageDataDir = imageDataDir;
    }

    private XMLDomMapleData(Node node) {
        this.node = node;
    }

    // =====================================================
    // ✅ REQUIRED BY provider.Data INTERFACE
    // =====================================================
    @Override
    public synchronized Data getChild(String name) {
        // Standard single-level child search (used by getChild("info"), etc.)
        NodeList childNodes = node.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++) {
            Node child = childNodes.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                NamedNodeMap attrs = child.getAttributes();
                if (attrs != null) {
                    Node nameAttr = attrs.getNamedItem("name");
                    if (nameAttr != null && nameAttr.getNodeValue().equals(name)) {
                        XMLDomMapleData ret = new XMLDomMapleData(child);
                        ret.imageDataDir = imageDataDir.resolve(getName().trim());
                        return ret;
                    }
                }
            }
        }
        return null;
    }

    @Override
    public synchronized Data getChildByPath(String path) {
        String[] segments = path.split("/");
        if (segments[0].equals("..")) {
            return ((Data) getParent()).getChildByPath(path.substring(path.indexOf("/") + 1));
        }

        Node currentNode = node;
        for (String s : segments) {
            NodeList childNodes = currentNode.getChildNodes();
            boolean found = false;
            for (int i = 0; i < childNodes.getLength(); i++) {
                Node childNode = childNodes.item(i);
                if (childNode.getNodeType() == Node.ELEMENT_NODE &&
                        childNode.getAttributes().getNamedItem("name").getNodeValue().equals(s)) {
                    currentNode = childNode;
                    found = true;
                    break;
                }
            }
            if (!found) return null;
        }

        XMLDomMapleData ret = new XMLDomMapleData(currentNode);
        ret.imageDataDir = imageDataDir.resolve(getName().trim()).resolve(path).getParent();
        return ret;
    }

    @Override
    public synchronized List<Data> getChildren() {
        List<Data> ret = new ArrayList<>();
        NodeList childNodes = node.getChildNodes();

        for (int i = 0; i < childNodes.getLength(); i++) {
            Node child = childNodes.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                XMLDomMapleData childData = new XMLDomMapleData(child);
                childData.imageDataDir = imageDataDir.resolve(getName().trim());
                ret.add(childData);
            }
        }

        return ret;
    }

    @Override
    public synchronized Object getData() {
        NamedNodeMap attrs = node.getAttributes();
        if (attrs == null) return null;

        DataType type = getType();
        if (type == null) return null;

        switch (type) {
            case DOUBLE:
            case FLOAT:
            case INT:
            case SHORT: {
                String value = attrs.getNamedItem("value").getNodeValue();
                Number num = GameConstants.parseNumber(value);
                switch (type) {
                    case DOUBLE: return num.doubleValue();
                    case FLOAT:  return num.floatValue();
                    case INT:    return num.intValue();
                    case SHORT:  return num.shortValue();
                }
                break;
            }
            case STRING:
            case UOL:
                return attrs.getNamedItem("value").getNodeValue();
            case VECTOR: {
                String x = attrs.getNamedItem("x").getNodeValue();
                String y = attrs.getNamedItem("y").getNodeValue();
                return new Point(Integer.parseInt(x), Integer.parseInt(y));
            }
        }
        return null;
    }

    @Override
    public synchronized DataType getType() {
        String nodeName = node.getNodeName();
        switch (nodeName) {
            case "imgdir": return DataType.PROPERTY;
            case "canvas": return DataType.CANVAS;
            case "convex": return DataType.CONVEX;
            case "sound":  return DataType.SOUND;
            case "uol":    return DataType.UOL;
            case "double": return DataType.DOUBLE;
            case "float":  return DataType.FLOAT;
            case "int":    return DataType.INT;
            case "short":  return DataType.SHORT;
            case "string": return DataType.STRING;
            case "vector": return DataType.VECTOR;
            case "null":   return DataType.IMG_0x00;
            default:       return null;
        }
    }

    @Override
    public synchronized DataEntity getParent() {
        Node parentNode = node.getParentNode();
        if (parentNode == null || parentNode.getNodeType() == Node.DOCUMENT_NODE) return null;

        XMLDomMapleData parent = new XMLDomMapleData(parentNode);
        parent.imageDataDir = imageDataDir.getParent();
        return parent;
    }

    @Override
    public synchronized String getName() {
        NamedNodeMap attrs = node.getAttributes();
        Node nameAttr = attrs != null ? attrs.getNamedItem("name") : null;
        return (nameAttr != null) ? nameAttr.getNodeValue() : "";
    }

    @Override
    public synchronized Iterator<Data> iterator() {
        return getChildren().iterator();
    }
}
