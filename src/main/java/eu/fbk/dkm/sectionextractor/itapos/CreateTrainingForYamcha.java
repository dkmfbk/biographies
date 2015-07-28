package eu.fbk.dkm.sectionextractor.itapos;

import eu.fbk.dkm.utils.CommandLine;
import org.apache.log4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
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

public class CreateTrainingForYamcha {

	static Logger logger = Logger.getLogger(CreateTrainingForYamcha.class.getName());

	static final Integer LAST_CHARS = 5;
	static final Integer MINUS_LAST_CHARS = 5;

	static final String fstanCommand = "/Users/alessio/Documents/scripts/textpro/modules/MorphoPro/bin/fstan/x86_64/fstan /Users/alessio/Documents/scripts/textpro/modules/MorphoPro/models/italian-utf8.fsa";
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

	public static void main(String[] args) {
		try {
			final CommandLine cmd = CommandLine
					.parser()
					.withName("wikipedia-text-parser")
					.withHeader("Analyze text dumps from Wikipedia")
					.withOption("i", "input", "input file", "FILE", CommandLine.Type.FILE_EXISTING, true, false, true)
					.withOption("o", "output", "output file", "FILE", CommandLine.Type.FILE, true, false, true)
//					.withOption("s", "size", "window size", "NUM", CommandLine.Type.NON_NEGATIVE_INTEGER, true, false, false)
					.withLogger(LoggerFactory.getLogger("eu.fbk")).parse(args);

			final File inputPath = cmd.getOptionValue("input", File.class);
			final File outputPath = cmd.getOptionValue("output", File.class);

//			Integer windowSize = cmd.getOptionValue("size", Integer.class, DEFAULT_WINDOW_SIZE);

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

			BufferedWriter writer = new BufferedWriter(new FileWriter(outputPath));

			FstanRunner runner = new FstanRunner(fstanCommand);
			HashMap<String, HashSet<String[]>> runnerCache = new HashMap<>();

//			ArrayList<FeatureSet> features = new ArrayList<>();
			for (int i = 0; i < tokens.size(); i++) {
				Token token = tokens.get(i);
				String form = token.getForm();
				String lform = form.toLowerCase();

				if (token.pos == null) {
					writer.append("\n");
					continue;
				}

				writer.append(form).append("\t");
				writer.append(lform).append("\t");

				HashSet<String[]> props = runnerCache.get(lform);
				if (props == null) {
					props = runner.getParts(lform);
					runnerCache.put(lform, props);
				}

				loopTypes:
				for (String type : allowedTypes) {
					for (String[] info : props) {
						if (info[1].equals(type)) {
							writer.append("Y").append("\t");
							continue loopTypes;
						}
					}
					writer.append("N").append("\t");
//					writer.append((props.contains(type) ? "Y" : "N")).append("\t");
				}

				for (String key : patterns.keySet()) {
					Pattern pattern = patterns.get(key);
					Matcher matcher = pattern.matcher(form);
					writer.append((matcher.find() ? "Y" : "N")).append("\t");
				}

				String firstChar = form.substring(0, 1);
				writer.append((firstChar.toUpperCase().equals(firstChar) ? "Y" : "N")).append("\t");

				writer.append((form.toUpperCase().equals(form) ? "Y" : "N")).append("\t");

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

				writer.append(token.pos).append("\n");
			}

			writer.close();

		} catch (Throwable ex) {
			CommandLine.fail(ex);
		}
	}
}
