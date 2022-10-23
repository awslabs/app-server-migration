package com.amazon.aws.am2.appmig.utils;

import com.amazon.aws.am2.appmig.estimate.Plan;
import com.amazon.aws.am2.appmig.estimate.Recommendation;
import org.apache.commons.io.FileUtils;
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

    public static String parseFile(String pathname) {
        String content = null;
        try {
            File file = new File(pathname);
            content = FileUtils.readFileToString(file);
        } catch (IOException e) {
            LOGGER.error("Unable to read the file {}", pathname);
        }
        return content;
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

    public static int findLineNumber(List<String> lines, String... searchString) {
        int lineNum = -1;
        for (int i = 0; i < lines.size() && lineNum == -1; i++) {
            String line = lines.get(i);
            for (String search : searchString) {
                if (line.contains(search)) {
                    lineNum = i + 1;
                } else {
                    lineNum = -1;
                    break;
                }
            }
        }
        return lineNum;
    }

    public static Plan convertRuleToPlan(JSONObject rule) {
        int id = ((Long) rule.get(ID)).intValue();
        String name = (String) rule.get(NAME);
        String description = (String) rule.get(DESCRIPTION);
        String complexity = (String) rule.get(COMPLEXITY);
        int mhrs = ((Long) rule.get(MHRS)).intValue();
        Plan plan = new Plan(id, name, description, complexity, mhrs);
        int recommendation = ((Long) rule.get(RECOMMENDATION)).intValue();
        plan.setRecommendations(recommendation);
        return plan;
    }

    public static List<String> findAllNodeValues(String path, String nodeName) throws Exception {
        List<String> lstNodeValue = new ArrayList<>();
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        dbFactory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        dbFactory.setXIncludeAware(false);
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
        } catch(Exception exp) {
        	LOGGER.error("Unable to parse the XML file {} due to {}", file, parse(exp));
        } finally {
        	if(reader != null) {
        		reader.close();
        	}
        }	
        return groupId;
    }

	public static Map<Integer, Recommendation> getAllRecommendations(String file, String ruleFiles) {
		Map<Integer, Recommendation> recommendations = new HashMap<>();
		File[] files = getRuleFiles(ruleFiles.split(","), "recommendation");
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
    
	public boolean getRecommendationFiles(File dir, String name, String[] ruleFiles) {
		for (String ruleFile : ruleFiles) {
			if (name.startsWith(ruleFile) && name.endsWith("recommdations.json")) {
				return true;
			}
		}
		return false;
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

    public static File[] getRuleFiles() {
        File Selected_Folder = new File(System.getProperty(USER_DIR) + RESOURCE_FOLDER_PATH);
        File[] listFiles = Selected_Folder.listFiles(new RuleFileFilter());
        return listFiles;
    }
    
    public static File[] getRuleFiles(String[] ruleFiles, String type) {
        File Selected_Folder = new File(System.getProperty(USER_DIR) + RESOURCE_FOLDER_PATH);
        File[] listFiles = Selected_Folder.listFiles(new RuleFileFilter(ruleFiles, type));
        return listFiles;
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
                queue.addAll(Files.list(path).filter(dir -> dir.toFile().isDirectory()).collect(Collectors.toList()));
            }
        }
        return found;
    }
}
