package com.amazon.aws.am2.appmig.utils;

import com.amazon.aws.am2.appmig.estimate.Plan;
import com.amazon.aws.am2.appmig.estimate.Recommendation;
import org.apache.commons.lang3.StringUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.amazon.aws.am2.appmig.constants.IConstants.*;

public class Utility {

    private final static Logger LOGGER = LoggerFactory.getLogger(Utility.class);

    public static String parse(Throwable exp) {
        String strException = null;
        try (StringWriter sw = new StringWriter()) {
            exp.printStackTrace(new PrintWriter(sw));
            strException = sw.toString();
        } catch (IOException ioe) {
            LOGGER.error("Got exception while converting Throwable object to String due to {} ", ioe.getMessage());
        }
        return strException;
    }

    public static String today() {
        Date date = new Date();
        SimpleDateFormat formatter = new SimpleDateFormat(SIMPLE_DT_FORMAT);
        return formatter.format(date);
    }

    public static List<String> readFile(String pathname) {
        String line;
        List<String> list = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(pathname))) {
            while ((line = reader.readLine()) != null) {
                list.add(line);
            }
        } catch (Exception e) {
            LOGGER.error("Unable to read the file {}", pathname);
        }
        return list;
    }

    public static Plan convertRuleToPlan(JSONObject rule) {
        int id = ((Long) rule.get(ID)).intValue();
        String name = (String) rule.get(NAME);
        String description = (String) rule.get(DESCRIPTION);
        String complexity = (String) rule.get(COMPLEXITY);
        String ruleType = (String) rule.get(RULE_TYPE);
        Plan plan = new Plan(id, name, description, complexity, ruleType);
        int recommendation = ((Long) rule.get(RECOMMENDATION)).intValue();
        plan.setRecommendations(recommendation);
        return plan;
    }

    public static List<String> findAllNodeValues(String path, String nodeName) throws Exception {
        List<String> lstNodeValue = new ArrayList<>();
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        dbFactory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        dbFactory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        dbFactory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        dbFactory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        dbFactory.setXIncludeAware(false);
        dbFactory.setExpandEntityReferences(false);
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.parse(path);
        doc.getDocumentElement().normalize();
        NodeList nList = doc.getElementsByTagName(nodeName);
        for (int temp = 0; temp < nList.getLength(); temp++) {
            Node nNode = nList.item(temp);
            if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                Element eElement = (Element) nNode;
                String ele = eElement.getTextContent();
                lstNodeValue.add(ele);
            }
        }
        return lstNodeValue;
    }

    public static String getBasePackage(File file) throws FileNotFoundException, XMLStreamException {
        XMLInputFactory factory = XMLInputFactory.newInstance();
        factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
        factory.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setProperty("javax.xml.stream.isSupportingExternalEntities", false);
        XMLStreamReader reader = factory.createXMLStreamReader(new FileReader(file));
        String startElement = "";
        String groupId = null;
        try {
            while (reader.hasNext() && groupId == null) {
                int event = reader.next();
                switch (event) {
                    case XMLStreamConstants.START_ELEMENT:
                        startElement = reader.getLocalName();
                        break;
                    case XMLStreamConstants.CHARACTERS:
                        if (startElement.equals(GROUP_ID)) {
                            groupId = reader.getText().trim();
                        }
                        break;
                }
            }
        } catch (Exception exp) {
            LOGGER.error("Unable to parse the XML file {} due to {}", file, parse(exp));
        } finally {
            if (reader != null) {
                reader.close();
            }
        }
        return groupId;
    }

    public static Map<Integer, Recommendation> getAllRecommendations(String file, String ruleNames) {
        Map<Integer, Recommendation> recommendations = new HashMap<>();
        File[] files = getRuleFiles(ruleNames.split(","), "recommendation");
        for (File recommendationsFile : files) {
            JSONParser parser = new JSONParser();
            try (Reader reader = new FileReader(recommendationsFile)) {
                JSONObject jsonObject = (JSONObject) parser.parse(reader);
                JSONArray jsonRecommendations = (JSONArray) jsonObject.get(RECOMMENDATIONS);
                for (Object jsonRecommendation : jsonRecommendations) {
                    JSONObject recObj = (JSONObject) jsonRecommendation;
                    int id = ((Long) recObj.get(ID)).intValue();
                    String name = (String) recObj.get(NAME);
                    String desc = (String) recObj.get(DESCRIPTION);
                    Recommendation recommendation = new Recommendation(id, name, desc);
                    recommendations.put(id, recommendation);
                }
            } catch (IOException | ParseException exp) {
                LOGGER.error("Unable to load the recommendations from file {} due to {}", file, parse(exp));
            }
        }
        return recommendations;
    }

    public static String fetchComplexity(List<Plan> plans) {
        String complexity = COMPLEXITY_MINOR;
        for (Plan plan : plans) {
            if (StringUtils.equalsAnyIgnoreCase(COMPLEXITY_CRITICAL, plan.getComplexity())) {
                complexity = COMPLEXITY_CRITICAL;
                break;
            } else if (StringUtils.equalsAnyIgnoreCase(COMPLEXITY_MAJOR, plan.getComplexity())) {
                complexity = COMPLEXITY_MAJOR;
            }
        }
        return complexity;
    }

    public static int findLineNumber(List<String> lines, String... searchString) {
        int lineNum = -1;
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            for (String search : searchString) {
                if (line.contains(search)) {
                    lineNum = i + 1;
                    break;
                }
            }
        }
        return lineNum;
    }

    public static File[] getRuleFiles(String[] ruleFileNames, String type) {
        File Selected_Folder = new File(System.getProperty(USER_DIR) + RESOURCE_FOLDER_PATH);
        return Selected_Folder.listFiles(new RuleFileFilter(ruleFileNames, type));
    }

    public static String fetchExtension(String extension) {
        String ext = extension;
        if (extension.contains(".")) {
            String[] tokens = extension.split("[.]");
            int idx = tokens.length - 1;
            ext = tokens[idx];
        }
        return ext;
    }

    public static String findPath(Queue<Path> queue, final String searchPath) throws Exception {
        String found = null;
        while (!queue.isEmpty()) {
            Path path = queue.remove();
            if (StringUtils.equals(path.getFileName().toString(), searchPath)) {
                found = path.toAbsolutePath().toString();
                break;
            } else {
                try (Stream<Path> filePathStream = Files.list(path)) {
                    queue.addAll(filePathStream.filter(dir -> dir.toFile().isDirectory()).collect(Collectors.toList()));
                }
            }
        }
        return found;
    }
}
