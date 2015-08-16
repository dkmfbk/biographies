package eu.fbk.dkm.sectionextractor;

import eu.fbk.dkm.utils.CommandLine;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.TreeSet;

/**
 * Created by alessio on 13/08/15.
 */

public class ConvertTrainingToYamcha {

	private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(ConvertTrainingToYamcha.class);
	private static final TreeSet<String> buzzWords = new TreeSet<>();

	static {
		buzzWords.add("Biography");
		buzzWords.add("Life");
		buzzWords.add("Career");
		buzzWords.add("Death");
	}

	public static void main(String[] args) {

		try {
			final CommandLine cmd = CommandLine
					.parser()
					.withName("wikipedia-text-parser")
					.withHeader("Convert CRFsuite training to SVM")
					.withOption("i", "input", "input file", "FILE", CommandLine.Type.FILE_EXISTING, true, false, true)
					.withOption("t", "test", "test file", "FILE", CommandLine.Type.FILE_EXISTING, true, false, true)
					.withOption("o", "output", "output file (training)", "FILE", CommandLine.Type.FILE, true, false, true)
					.withOption("u", "output-test", "output file (test)", "FILE", CommandLine.Type.FILE, true, false, true)
					.withLogger(LoggerFactory.getLogger("eu.fbk")).parse(args);

			final File inputPath = cmd.getOptionValue("input", File.class);
			final File testPath = cmd.getOptionValue("test", File.class);
			final File outputPath = cmd.getOptionValue("output", File.class);
			final File testOutputPath = cmd.getOptionValue("output-test", File.class);

			BufferedWriter writer = new BufferedWriter(new FileWriter(outputPath));
			BufferedWriter testWriter = new BufferedWriter(new FileWriter(testOutputPath));

			List<String> lines;

			lines = Files.readAllLines(inputPath.toPath());
			writeFeatures(lines, writer);

			lines = Files.readAllLines(testPath.toPath());
			writeFeatures(lines, testWriter);

			testWriter.close();
			writer.close();

		} catch (final Throwable ex) {
			CommandLine.fail(ex);
		}

	}

	private static void writeFeatures(List<String> lines, BufferedWriter writer) throws IOException {
		for (String line : lines) {
			if (line.trim().length() == 0) {
				writer.append("\n");
				continue;
			}

			String[] parts = line.split("\\s+");
			String text = null;
			for (int i = 1; i < parts.length; i++) {
				if (parts[i].startsWith("[text]")) {
					text = parts[i].substring(6);
					break;
				}
			}

			if (text == null) {
				LOGGER.warn("Text is null");
				continue;
			}

			String[] tokens = text.split("_");

			writer.append(text).append("\t");
			writer.append(text.toLowerCase()).append("\t");
			writer.append(tokens[0]).append("\t");
			writer.append(tokens[0].toLowerCase()).append("\t");
			writer.append(tokens[tokens.length - 1]).append("\t");
			writer.append(tokens[tokens.length - 1].toLowerCase()).append("\t");

			boolean containsOneBuzzWord = false;

			for (String buzzWord : buzzWords) {
				boolean contains = false;
				for (String token : tokens) {
					if (token.toLowerCase().equals(buzzWord.toLowerCase())) {
						contains = true;
					}
				}
				containsOneBuzzWord = containsOneBuzzWord || contains;

				writer.append(contains ? "Y" : "N").append("\t");
			}

			writer.append(containsOneBuzzWord ? "Y" : "N").append("\t");

			writer.append(parts[0]);

			writer.append("\n");
		}
	}

}
