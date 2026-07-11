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
            DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
            Document document = documentBuilder.parse(fis);
            this.node = document.getFirstChild();
        } catch (ParserConfigurationException e) {
            throw new RuntimeException(e);
        } catch (SAXException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        this.imageDataDir = imageDataDir;
    }

    private XMLDomMapleData(Node node) {
        this.node = node;
    }

    @Override
    public synchronized Data getChildByPath(String path) {  // the whole XML reading system seems susceptible to give nulls on strenuous read scenarios
        String[] segments = path.split("/");
        if (segments[0].equals("..")) {
            return ((Data) getParent()).getChildByPath(path.substring(path.indexOf("/") + 1));
        }

        Node myNode;
        myNode = node;
        for (String s : segments) {
            NodeList childNodes = myNode.getChildNodes();
            boolean foundChild = false;
            for (int i = 0; i < childNodes.getLength(); i++) {
                Node childNode = childNodes.item(i);
                if (childNode.getNodeType() == Node.ELEMENT_NODE
                        && childNode.getAttributes().getNamedItem("name").getNodeValue().equals(s)) {
                    myNode = childNode;
                    foundChild = true;
                    break;
                }
            }
            if (!foundChild) {
                return null;
            }
        }

        XMLDomMapleData ret = new XMLDomMapleData(myNode);
        ret.imageDataDir = imageDataDir.resolve(getName().trim()).resolve(path).getParent();
        return ret;
    }

    @Override
    public synchronized List<Data> getChildren() {
        List<Data> ret = new ArrayList<>();

        NodeList childNodes = node.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++) {
            Node childNode = childNodes.item(i);
            if (childNode.getNodeType() == Node.ELEMENT_NODE) {
                XMLDomMapleData child = new XMLDomMapleData(childNode);
                child.imageDataDir = imageDataDir.resolve(getName().trim());
                ret.add(child);
            }
        }

        return ret;
    }

    @Override
    public synchronized Object getData() {
        NamedNodeMap attributes = node.getAttributes();
        DataType type = getType();
        switch (type) {
            case DOUBLE:
            case FLOAT:
            case INT:
            case SHORT: {
                String value = attributes.getNamedItem("value").getNodeValue();
                Number nval = GameConstants.parseNumber(value);

                switch (type) {
                    case DOUBLE:
                        return nval.doubleValue();
                    case FLOAT:
                        return nval.floatValue();
                    case INT:
                        return nval.intValue();
                    case SHORT:
                        return nval.shortValue();
                    default:
                        return null;
                }
            }
            case STRING:
            case UOL: {
                String value = attributes.getNamedItem("value").getNodeValue();
                return value;
            }
            case VECTOR: {
                String x = attributes.getNamedItem("x").getNodeValue();
                String y = attributes.getNamedItem("y").getNodeValue();
                return new Point(Integer.parseInt(x), Integer.parseInt(y));
            }
            default:
                return null;
        }
    }

    @Override
    public synchronized DataType getType() {
        String nodeName = node.getNodeName();

        switch (nodeName) {
            case "imgdir":
                return DataType.PROPERTY;
            case "canvas":
                return DataType.CANVAS;
            case "convex":
                return DataType.CONVEX;
            case "sound":
                return DataType.SOUND;
            case "uol":
                return DataType.UOL;
            case "double":
                return DataType.DOUBLE;
            case "float":
                return DataType.FLOAT;
            case "int":
                return DataType.INT;
            case "short":
                return DataType.SHORT;
            case "string":
                return DataType.STRING;
            case "vector":
                return DataType.VECTOR;
            case "null":
                return DataType.IMG_0x00;
        }
        return null;
    }

    @Override
    public synchronized DataEntity getParent() {
        Node parentNode;
        parentNode = node.getParentNode();
        if (parentNode.getNodeType() == Node.DOCUMENT_NODE) {
            return null;
        }
        XMLDomMapleData parentData = new XMLDomMapleData(parentNode);
        parentData.imageDataDir = imageDataDir.getParent();
        return parentData;
    }

    @Override
    public synchronized String getName() {
        return node.getAttributes().getNamedItem("name").getNodeValue();
    }

    @Override
    public synchronized Iterator<Data> iterator() {
        return getChildren().iterator();
    }
}
