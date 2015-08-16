package eu.fbk.dkm.sectionextractor.itapos;

import eu.fbk.dkm.utils.CommandLine;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

/**
 * Created by alessio on 17/07/15.
 */

public class CreateTrainingForYamcha {

	private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(CreateTrainingForYamcha.class);

	static final Integer LAST_CHARS = 5;
	static final Integer MINUS_LAST_CHARS = 5;

	static ArrayList<String> allowedTypes = new ArrayList<>();

	static {
		allowedTypes.add("adv");
		allowedTypes.add("adj");
		allowedTypes.add("v");
		allowedTypes.add("n");
		allowedTypes.add("prep");
		allowedTypes.add("pron");
		allowedTypes.add("conj");
		allowedTypes.add("inter");
	}

	public static void main(String[] args) {
		try {
			final CommandLine cmd = CommandLine
					.parser()
					.withName("wikipedia-text-parser")
					.withHeader("Analyze text dumps from Wikipedia")
					.withOption("i", "input", "input file (in CRFsuite format)", "FILE", CommandLine.Type.FILE_EXISTING, true, false, true)
					.withOption("o", "output", "output file", "FILE", CommandLine.Type.FILE, true, false, true)
//					.withOption("s", "size", "window size", "NUM", CommandLine.Type.NON_NEGATIVE_INTEGER, true, false, false)
					.withLogger(LoggerFactory.getLogger("eu.fbk")).parse(args);

			final File inputPath = cmd.getOptionValue("input", File.class);
			final File outputPath = cmd.getOptionValue("output", File.class);

			List<String> lines = Files.readAllLines(inputPath.toPath());

			BufferedWriter writer = new BufferedWriter(new FileWriter(outputPath));

			for (String line : lines) {
				line = line.trim();
				if (line.length() == 0) {
					writer.append("\n");
					continue;
				}

				HashSet<String> features = new HashSet<>();
				String[] parts = line.split("\\s+");
				String form = null;

				String pos = parts[0];

				HashMap<Integer, String> adds = new HashMap<>();
				for (int i = 1; i < parts.length; i++) {
					String part = parts[i];
					if (part.startsWith("[")) {
						continue;
					}

					features.add(part);
					if (part.startsWith("FORM.")) {
						form = part.substring(5);
					}
					if (part.startsWith("ADD.")) {
						String[] addParts = part.split("\\.");
						adds.put(Integer.parseInt(addParts[2]), addParts[3]);
					}
				}

				if (form == null) {
					LOGGER.warn("Form is null");
					continue;
				}

				String lform = form.toLowerCase();

				writer.append(form).append("\t");
				writer.append(lform).append("\t");

				addBooleanFeature("UPPERCASE.1", writer, features);
				addBooleanFeature("UPPERCASE1.1", writer, features);
				addBooleanFeature("NUMERIC.1", writer, features);

				for (int j = 1; j <= LAST_CHARS; j++) {
					try {
						String part = form.substring(form.length() - j);
						writer.append(part).append("\t");
					} catch (Exception e) {
						writer.append("__nil__").append("\t");
					}
				}

				for (int j = 1; j <= MINUS_LAST_CHARS; j++) {
					try {
						String part = form.substring(0, form.length() - j);
						if (part.length() == 0) {
							part = "__nil__";
						}
						writer.append(part).append("\t");
					} catch (Exception e) {
						writer.append("__nil__").append("\t");
					}
				}

				for (String type : allowedTypes) {
					addBooleanFeature("TYPE." + type, writer, features);
				}

				for (int i = 1; i < 7; i++) {
					if (adds.containsKey(i)) {
						writer.append(adds.get(i)).append("\t");
					}
					else {
						writer.append("__nil__").append("\t");
					}
				}

				addBooleanFeatureFromStart("JOB", writer, features);
				addBooleanFeatureFromStart("ORG", writer, features);
				addBooleanFeatureFromStart("LOC", writer, features);
				addBooleanFeatureFromStart("NAM", writer, features);
				addBooleanFeatureFromStart("SUB", writer, features);

				writer.append(pos).append("\n");
//				System.out.println(form);
//				System.out.println(pos);
//				System.out.println(features);
//				System.out.println();
			}

			writer.close();

//			ArrayList<Token> tokens = new ArrayList<>();
//			List<String> lines = Files.readAllLines(inputPath.toPath());
//			for (String line : lines) {
//				line = line.trim();
//				if (line.length() == 0) {
//					continue;
//				}
//
//				String[] parts = line.split("\\s+");
//
//				String form = parts[0];
//				String pos = null;
//				if (parts.length > 1) {
//					pos = parts[1];
//				}
//
//				Token token = new Token(form, pos);
//				tokens.add(token);
//			}
//
//			BufferedWriter writer = new BufferedWriter(new FileWriter(outputPath));
//
//			FstanRunner runner = new FstanRunner(fstanCommand);
//			HashMap<String, HashSet<String[]>> runnerCache = new HashMap<>();
//
//			for (int i = 0; i < tokens.size(); i++) {
//				Token token = tokens.get(i);
//				String form = token.getForm();
//				String lform = form.toLowerCase();
//
//				if (token.pos == null) {
//					writer.append("\n");
//					continue;
//				}
//
//				writer.append(form).append("\t");
//				writer.append(lform).append("\t");
//
//				HashSet<String[]> props = runnerCache.get(lform);
//				if (props == null) {
//					props = runner.getParts(lform);
//					runnerCache.put(lform, props);
//				}
//
//				loopTypes:
//				for (String type : allowedTypes) {
//					for (String[] info : props) {
//						if (info[1].equals(type)) {
//							writer.append("Y").append("\t");
//							continue loopTypes;
//						}
//					}
//					writer.append("N").append("\t");
//				}
//
//				for (String key : patterns.keySet()) {
//					Pattern pattern = patterns.get(key);
//					Matcher matcher = pattern.matcher(form);
//					writer.append((matcher.find() ? "Y" : "N")).append("\t");
//				}
//
//				String firstChar = form.substring(0, 1);
//				writer.append((firstChar.toUpperCase().equals(firstChar) ? "Y" : "N")).append("\t");
//
//				writer.append((form.toUpperCase().equals(form) ? "Y" : "N")).append("\t");
//
//				for (int j = 1; j <= LAST_CHARS; j++) {
//					try {
//						String part = form.substring(form.length() - j);
//						writer.append(part).append("\t");
//					} catch (Exception e) {
//						writer.append("__nil__").append("\t");
//					}
//				}
//
//				for (int j = 1; j <= MINUS_LAST_CHARS; j++) {
//					try {
//						String part = form.substring(0, form.length() - j);
//						if (part.length() == 0) {
//							part = "__nil__";
//						}
//						writer.append(part).append("\t");
//					} catch (Exception e) {
//						writer.append("__nil__").append("\t");
//					}
//				}
//
//				writer.append(token.pos).append("\n");
//			}
//
//			writer.close();

		} catch (Throwable ex) {
			CommandLine.fail(ex);
		}
	}

	private static void addBooleanFeatureFromStart(String s, BufferedWriter writer, HashSet<String> features) throws IOException {
		boolean contained = false;
		for (String feature : features) {
			if (feature.startsWith(s)) {
				contained = true;
				break;
			}
		}

		if (contained) {
			writer.append("Y").append("\t");
		}
		else {
			writer.append("N").append("\t");
		}
	}

	private static void addBooleanFeature(String s, BufferedWriter writer, HashSet<String> features) throws IOException {
		if (features.contains(s)) {
			writer.append("Y").append("\t");
		}
		else {
			writer.append("N").append("\t");
		}
	}

}
