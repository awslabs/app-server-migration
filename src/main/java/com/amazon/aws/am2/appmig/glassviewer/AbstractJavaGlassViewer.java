package com.amazon.aws.am2.appmig.glassviewer;

import com.amazon.aws.am2.appmig.glassviewer.db.AppDiscoveryGraphDB;
import com.amazon.aws.am2.appmig.glassviewer.utils.GlassViewerUtils;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.atn.ParserATNSimulator;
import org.antlr.v4.runtime.atn.PredictionContextCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import src.main.resources.Java8Lexer;
import src.main.resources.Java8Parser;

import java.io.BufferedInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

import static com.amazon.aws.am2.appmig.glassviewer.utils.GlassViewerUtils.parse;

public abstract class AbstractJavaGlassViewer implements IJavaGlassViewer {

    private final static Logger LOGGER = LoggerFactory.getLogger(AbstractJavaGlassViewer.class);

    protected String filePath;
    protected Java8Parser.CompilationUnitContext parseTree;
    protected Java8Parser parser;
    protected String basePackage;
    protected String projectId;

    protected abstract void processImports();

    protected abstract void processClassVariables();

    protected abstract void processStaticBlocks();

    protected abstract void processClasses();

    protected abstract void processInterfaces();

    protected abstract void processMethods();

    protected abstract void store();
    
    public abstract Map<Integer, String> searchReferences(String importStmt) throws Exception;

    public abstract Map<Integer, String> search(String pattern) throws Exception;

    public final void view(String filePath, String projectId) {
        try {
        	this.projectId = projectId;
        	generateParseTree(filePath);
            processClasses();
            processInterfaces();
            processImports();
            processStaticBlocks();
            processClassVariables();
            processMethods();
            store();
        } catch (FileNotFoundException fileException) {
            LOGGER.error("Could not proceed with processing the file {} due to {}", filePath, GlassViewerUtils.parse(fileException));
        } catch(Exception exp) {
            LOGGER.error("Unable to proceed with processing the file {} due to {}", filePath, GlassViewerUtils.parse(exp));
        }
    }

    private void generateParseTree(String filePath) throws FileNotFoundException {
        LOGGER.debug("reading and parsing file {}", filePath);
        this.filePath = filePath;
        try (InputStream inputStream = new BufferedInputStream(Files.newInputStream(Paths.get(filePath)))) {
            Lexer lexer = new Java8Lexer(CharStreams.fromStream(inputStream));
            TokenStream tokenStream = new CommonTokenStream(lexer);
            parser = new Java8Parser(tokenStream);
            parser.setTrimParseTree(true);
            parser.setInterpreter(new ParserATNSimulator(
            		  parser, parser.getATN(), parser.getInterpreter().decisionToDFA, new PredictionContextCache()));
            parseTree = parser.compilationUnit();
        } catch (Exception e) {
            LOGGER.error("Unable to read the file {}", filePath);
            throw new FileNotFoundException(parse(e));
        }
    }

    public void cleanup() {
        try {
            AppDiscoveryGraphDB.getInstance().close();
            parser.getInterpreter().clearDFA();
            parser.reset();	
            parser = null;
        } catch (Exception e) {
            LOGGER.error("Unable to close the DB instance due to {}", GlassViewerUtils.parse(e));
        }
    }
}
