package eu.fbk.dkm.sectionextractor;

import eu.fbk.dkm.utils.CommandLine;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Created by alessio on 13/08/15.
 */

public class FeatureIndexer {

	private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(FeatureIndexer.class);
	private static final HashMap<String, Integer> values = new HashMap<>();

	static {
		values.put("B-SEC", 1);
		values.put("I-SEC", 1);
		values.put("O", -1);
	}

	public static void main(String[] args) {

		try {
			final CommandLine cmd = CommandLine
					.parser()
					.withName("wikipedia-text-parser")
					.withHeader("Convert CRFsuite training to SVM")
					.withOption("i", "input", "input file", "FILE", CommandLine.Type.FILE_EXISTING, true, false, true)
					.withOption("t", "test", "test file", "FILE", CommandLine.Type.FILE_EXISTING, true, false, true)
//					.withOption("d", "dates", "dates file", "FILE", CommandLine.Type.FILE_EXISTING, true, false, false)
					.withOption("o", "output", "output file (training)", "FILE", CommandLine.Type.FILE, true, false, true)
					.withOption("u", "output-test", "output file (test)", "FILE", CommandLine.Type.FILE, true, false, true)
					.withOption("f", "features", "features file", "FILE", CommandLine.Type.FILE, true, false, false)
//					.withOption("s", "size", "window size", "NUM", CommandLine.Type.NON_NEGATIVE_INTEGER, true, false, false)
//					.withOption("g", "gold", "gold file (CSV format)", "FILE", CommandLine.Type.FILE_EXISTING, true, false, false)
//					.withOption("t", "test", "is test")
					.withLogger(LoggerFactory.getLogger("eu.fbk")).parse(args);

			final File inputPath = cmd.getOptionValue("input", File.class);
			final File testPath = cmd.getOptionValue("test", File.class);
			final File outputPath = cmd.getOptionValue("output", File.class);
			final File testOutputPath = cmd.getOptionValue("output-test", File.class);
			final File featuresPath = cmd.getOptionValue("features", File.class);

			BufferedWriter writer = new BufferedWriter(new FileWriter(outputPath));
			BufferedWriter testWriter = new BufferedWriter(new FileWriter(testOutputPath));

			TreeMap<String, Integer> features = new TreeMap<>();

			int index = 0;
			List<String> lines;

			lines = Files.readAllLines(inputPath.toPath());
			for (String line : lines) {
				if (line.trim().length() == 0) {
					continue;
				}

				TreeSet<Integer> featureSet = new TreeSet<>();

				String[] parts = line.split("\\s+");
				writer.append(values.get(parts[0]).toString());
				for (int i = 1; i < parts.length; i++) {
					String feature = parts[i];
					if (features.get(feature) == null) {
						features.put(feature, ++index);
					}
					featureSet.add(features.get(feature));
				}

				for (Integer feature : featureSet) {
					writer.append("\t").append(feature.toString()).append(":1");
				}

				writer.append("\n");
			}

			lines = Files.readAllLines(testPath.toPath());
			for (String line : lines) {
				if (line.trim().length() == 0) {
					continue;
				}

				TreeSet<Integer> featureSet = new TreeSet<>();

				String[] parts = line.split("\\s+");
				testWriter.append(values.get(parts[0]).toString());
				for (int i = 1; i < parts.length; i++) {
					String feature = parts[i];
					if (features.get(feature) == null) {
						continue;
					}
					featureSet.add(features.get(feature));
				}

				for (Integer feature : featureSet) {
					testWriter.append("\t").append(feature.toString()).append(":1");
				}

				testWriter.append("\n");
			}

			if (featuresPath != null) {
				BufferedWriter fWriter = new BufferedWriter(new FileWriter(featuresPath));
				for (String key : features.keySet()) {
					Integer value = features.get(key);
					fWriter.append(value.toString()).append("\t").append(key).append("\n");
				}
				fWriter.close();
			}

			testWriter.close();
			writer.close();

		} catch (final Throwable ex) {
			CommandLine.fail(ex);
		}

	}

}
