package eu.fbk.dkm.sectionextractor.itapos;

import eu.fbk.dkm.sectionextractor.FeatureSet;
import eu.fbk.dkm.utils.CommandLine;
import org.apache.log4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by alessio on 17/07/15.
 */

public class CreateTraining {

	static Logger logger = Logger.getLogger(CreateTraining.class.getName());

	static final Integer LAST_CHARS = 5;
	static final Integer MINUS_LAST_CHARS = 5;

	static final String fstanCommand = "/Users/alessio/Documents/scripts/textpro/modules/MorphoPro/bin/fstan/x86_64/fstan /Users/alessio/Documents/scripts/textpro/modules/MorphoPro/models/italian-utf8.fsa";

	static HashMap<String, Pattern> patterns = new HashMap<>();
	private static final Integer DEFAULT_WINDOW_SIZE = 0;
	private static HashSet<String> noWindowFeatures = new HashSet<>();

	static {
		patterns.put("NUMERIC", Pattern.compile("^[0-9]+$"));
//		patterns.put("UPPERCASE", Pattern.compile("^[A-Z]+$"));
//		patterns.put("SUPPERCASE", Pattern.compile("^[A-Z]"));
		noWindowFeatures.add("EOS");
		noWindowFeatures.add("BOS");
		noWindowFeatures.add("LAST");
	}

	private static class Token {
		private String form, pos;

		public Token(String form, @Nullable String pos) {
			this.form = form;
			this.pos = pos;
		}

		public String getForm() {
			return form;
		}

		public void setForm(String form) {
			this.form = form;
		}

		public String getPos() {
			return pos;
		}

		public void setPos(String pos) {
			this.pos = pos;
		}

		@Override
		public String toString() {
			return "Token{" +
					"form='" + form + '\'' +
					", pos='" + pos + '\'' +
					'}';
		}
	}

	private static void writeFeatures(BufferedWriter writer, ArrayList<FeatureSet> features, Integer windowSize) throws IOException {
		for (int i = 0; i < features.size(); i++) {
			FeatureSet featureSet = new FeatureSet();
			featureSet.features.addAll(features.get(i).features);
			featureSet.value = features.get(i).value;

			if (i == 0) {
				featureSet.addFeature("[BOS]");
			}
			if (i == features.size() - 1) {
				featureSet.addFeature("[EOS]");
			}

			for (int j = i - windowSize; j <= i + windowSize; j++) {
				if (j == i || j >= features.size() || j < 0) {
					continue;
				}

				FeatureSet thisFS = features.get(j);
				featureLoop:
				for (String feature : thisFS.features) {
					for (String noWindowFeature : noWindowFeatures) {
						if (feature.startsWith(noWindowFeature)) {
							continue featureLoop;
						}
					}

					featureSet.addFeature("[" + (j - i) + "]" + feature);
				}
			}

			writer.append(featureSet.toString()).append("\n");
		}

		writer.append("\n");
	}

	public static void main(String[] args) {
		try {
			final CommandLine cmd = CommandLine
					.parser()
					.withName("wikipedia-text-parser")
					.withHeader("Analyze text dumps from Wikipedia")
					.withOption("i", "input", "input file", "FILE", CommandLine.Type.FILE_EXISTING, true, false, true)
					.withOption("o", "output", "output file", "FILE", CommandLine.Type.FILE, true, false, true)
					.withOption("s", "size", "window size", "NUM", CommandLine.Type.NON_NEGATIVE_INTEGER, true, false, false)
					.withLogger(LoggerFactory.getLogger("eu.fbk")).parse(args);

			final File inputPath = cmd.getOptionValue("input", File.class);
			final File outputPath = cmd.getOptionValue("output", File.class);

			Integer windowSize = cmd.getOptionValue("size", Integer.class, DEFAULT_WINDOW_SIZE);

			ArrayList<Token> tokens = new ArrayList<>();
			List<String> lines = Files.readAllLines(inputPath.toPath());
			for (String line : lines) {
				line = line.trim();
				if (line.length() == 0) {
					continue;
				}

				String[] parts = line.split("\\s+");

				String form = parts[0];
				String pos = null;
				if (parts.length > 1) {
					pos = parts[1];
				}

				Token token = new Token(form, pos);
				tokens.add(token);
			}

			FstanRunner runner = new FstanRunner(fstanCommand);
			HashMap<String, HashSet<String[]>> runnerCache = new HashMap<>();

			BufferedWriter writer = new BufferedWriter(new FileWriter(outputPath));

			ArrayList<FeatureSet> features = new ArrayList<>();
			for (int i = 0; i < tokens.size(); i++) {
				Token token = tokens.get(i);
				String form = token.getForm();
				String lform = form.toLowerCase();

				if (token.pos == null) {
//					System.out.println(features);

//					for (FeatureSet featureSet : features) {
//						System.out.println(featureSet);
//					}
//					System.out.println();

					writeFeatures(writer, features, windowSize);
					features = new ArrayList<>();
					continue;
				}

				FeatureSet set = new FeatureSet();
				set.addFeature("FORM." + form);
				set.addFeature("LFORM." + lform);

				HashSet<String[]> types = runnerCache.get(lform);
				if (types == null) {
					types = runner.getParts(lform);
					runnerCache.put(lform, types);
				}

				boolean unknown = true;
				for (String[] info : types) {
					set.addFeature("TYPE." + info[1]);
					unknown = false;
					for (int j = 2; j < info.length; j++) {
						set.addFeature("ADD." + info[1] + "." + j + "." + info[j]);
					}
				}
				if (unknown) {
					set.addFeature("TYPE.UNKNOWN");
				}

//				for (String type : types) {
//					set.addFeature("TYPE." + type);
//				}

				for (String key : patterns.keySet()) {
					Pattern pattern = patterns.get(key);
					Matcher matcher = pattern.matcher(form);
					set.addFeature(key + "." + (matcher.find() ? "1" : "0"));
				}

				String firstChar = form.substring(0, 1);
				if (firstChar.toUpperCase().equals(firstChar)) {
					set.addFeature("UPPERCASE1.1");
				}
				else {
					set.addFeature("UPPERCASE1.0");
				}

//				String numbers = form.replaceAll("[^0-9]", "");
//				if (numbers.length() > 0) {
//					set.addFeature("HASNUM.1");
//				}
//				else {
//					set.addFeature("HASNUM.0");
//				}

				if (form.toUpperCase().equals(form)) {
					set.addFeature("UPPERCASE.1");
				}
				else {
					set.addFeature("UPPERCASE.0");
				}

				for (int j = 1; j <= Math.min(LAST_CHARS, form.length()); j++) {
					String part = form.substring(form.length() - j);
					set.addFeature("LAST" + j + "." + part.toLowerCase());
				}

				for (int j = 1; j <= Math.min(MINUS_LAST_CHARS, form.length()); j++) {
					String part = form.substring(0, form.length() - j);
					if (part.length() > 0) {
						set.addFeature("FIRST" + j + "." + part.toLowerCase());
					}
				}

				set.value = token.pos;

				features.add(set);
			}

			writer.close();

		} catch (Throwable ex) {
			CommandLine.fail(ex);
		}
	}
}
