//package de.vemaeg.util;
//
//import java.io.File;
//import java.io.IOException;
//import java.io.StringWriter;
//import java.util.Map;
//
//import org.apache.log4j.Logger;
//import org.apache.velocity.VelocityContext;
//import org.apache.velocity.app.Velocity;
//import org.apache.velocity.app.VelocityEngine;
//import org.apache.velocity.exception.MethodInvocationException;
//import org.apache.velocity.exception.ParseErrorException;
//import org.apache.velocity.exception.ResourceNotFoundException;
//
//import de.vemaeg.common.util.ReadWriteTextFile;
//
//public class VelocityRenderer {
//	
//	private static final Logger LOGGER = Logger.getLogger(VelocityRenderer.class);
//	
//	private VelocityEngine ve;
//	private static VelocityRenderer instance = null;
//	
//	private VelocityRenderer() {
//		this(null);
//	}
//	
//	private VelocityRenderer(String fileLoaderPath) {
//		ve = new VelocityEngine();
//		
//		ve.setProperty(Velocity.COUNTER_NAME, "listenNummer");
//		ve.setProperty(Velocity.MAX_NUMBER_LOOPS, 100);
//		ve.setProperty(Velocity.RUNTIME_LOG_LOGSYSTEM_CLASS, "org.apache.velocity.runtime.log.NullLogSystem");
//		
//		ve.setProperty(Velocity.INPUT_ENCODING, "UTF-8");
//		ve.setProperty(Velocity.OUTPUT_ENCODING, "UTF-8");
//		
//		ve.setProperty(Velocity.FILE_RESOURCE_LOADER_CACHE, "false");
//		if (fileLoaderPath != null) {
//			ve.setProperty(Velocity.FILE_RESOURCE_LOADER_PATH, fileLoaderPath);
//		}
//		
//		/*
//		ve.setProperty("layout.template.cache.enabled", "false");
//		ve.setProperty(Velocity.VM_LIBRARY_AUTORELOAD, "true");		
//		*/
//		
//        try {        	
//        	ve.init();			
//		} catch (Exception e) {
//			LOGGER.error("Velocity Init Error", e);
//		}
//	}
//	
//	public static VelocityRenderer getInstance() {
//		return getInstance(null);
//	}
//	
//	public static VelocityRenderer getInstance(String fileLoaderPath) {
//		// FIXME: caching v. Makros...
//		return new VelocityRenderer(fileLoaderPath);
//		/*
//		if (instance == null) {
//			instance = new VelocityRenderer(fileLoaderPath);
//		}
//		return instance;
//		*/
//	}
//	
//	public String renderTemplate(File f, Map<String, Object> daten, boolean preProcess) {
//		String template = ReadWriteTextFile.getContents(f);
//		return renderTemplateSafe(template, daten, preProcess);
//	}
//
//	public String renderTemplate(String template, Map<String, Object> daten, boolean preProcess) throws ParseErrorException, MethodInvocationException, ResourceNotFoundException, IOException {
//		VelocityContext context = new VelocityContext();
//		if (daten != null) {
//			for (String key : daten.keySet()) {
//				context.put(key, daten.get(key));
//			}
//		}
//		
//		if (preProcess) {
//			template = preprocessTemplate(template);
//		}
//		
//        StringWriter w = new StringWriter();
//        
//		ve.evaluate(context, w, "renderTemplate", template);
//		String ret = w.toString();
//		//System.err.println("RENDER OUTPUT: " + w);
//        return ret;		
//	}
//	
//	public String renderTemplateSafe(String template, Map<String, Object> daten, boolean preProcess) {
//		try {
//			return renderTemplate(template, daten, preProcess);
//		} catch (ParseErrorException e) {
//			LOGGER.error("Velocity Parse Error", e);
//			System.err.println(template);
//		} catch (MethodInvocationException e) {
//			LOGGER.error("Velocity Parse Error ", e);
//		} catch (ResourceNotFoundException e) {
//			LOGGER.error("Velocity Parse Error", e);
//		} catch (IOException e) {
//			LOGGER.error("Velocity Parse Error", e);
//		}
//		return "Fehler beim Erstellen der Inhalte.";
//	}
//	
//	private String preprocessTemplate(String input) {
//		input = input.trim();
//		
//		// Formatierungen
//		input = input.replaceAll("&nbsp;", " ");
//		input = input.replaceAll("#([^<]*)<br/>", "#$1");
//		input = input.replaceAll("#([^<]*)<br />", "#$1");
//		input = input.replaceAll("<p>#([^<]*)</p>" , "#$1");
//		
//		//System.err.println("RENDER INPUT: " + input);		
//		return input;
//	}
//}
