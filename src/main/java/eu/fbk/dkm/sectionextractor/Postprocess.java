package eu.fbk.dkm.sectionextractor;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import eu.fbk.dkm.utils.CommandLine;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.*;

/**
 * Created by alessio on 13/06/15.
 */

public class Postprocess {

	private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(Postprocess.class);
	private static HashSet<String> labelsToContinue = new HashSet<>();
	private static HashSet<String> labelEndsToContinue = new HashSet<>();
	private static HashSet<String> labelAttachedContinue = new HashSet<>();
	private static HashSet<String> labelsToAvoid = new HashSet<>();

	static {
		labelsToContinue.add("Death".toLowerCase());

		labelAttachedContinue.add("Personal life".toLowerCase());

//		labelEndsToContinue.add("Career".toLowerCase());

		labelsToAvoid.add("References".toLowerCase());
		labelsToAvoid.add("See also".toLowerCase());
		labelsToAvoid.add("External links".toLowerCase());
	}

	public static void main(String[] args) {
		try {
			final CommandLine cmd = CommandLine
					.parser()
					.withName("wikipedia-text-parser")
					.withHeader("Analyze text dumps from Wikipedia")
					.withOption("i", "input", "input file", "FILE", CommandLine.Type.FILE_EXISTING, true, false, true)
					.withOption("r", "results", "results file", "FILE", CommandLine.Type.FILE_EXISTING, true, false, true)
					.withOption("o", "output", "output file", "FILE", CommandLine.Type.FILE, true, false, true)
					.withLogger(LoggerFactory.getLogger("eu.fbk")).parse(args);

			final File inputPath = cmd.getOptionValue("input", File.class);
			final File outputPath = cmd.getOptionValue("output", File.class);
			final File resultsPath = cmd.getOptionValue("results", File.class);

			List<String> lines = Files.readLines(inputPath, Charsets.UTF_8);
			LOGGER.info("Input lines: {}", lines.size());

			String last = null;
			LinkedHashMap<String, LinkedHashSet<String>> sections = new LinkedHashMap<>();
			ArrayList<ArrayList<String>> features = new ArrayList<>();

			int rowIndex = 0;
			int parsedLines = 0;
			for (String line : lines) {
				line = line.trim();
				String[] parts = line.split("\t");
				if (parts.length < 2) {
					LOGGER.warn("Row contains less than 2 tokens");
					continue;
				}

				String page = parts[0];
				String firstLevel = parts[1];
				String secondLevel = null;
				if (parts.length >= 3) {
					secondLevel = parts[2];
				}

				if (last != null && !page.equals(last)) {
					ArrayList<String> featureSets = new ArrayList<>(sections.keySet());
					parsedLines += sections.size();
					LOGGER.debug("" + sections.size() + " " + last);
					features.add(featureSets);
					sections = new LinkedHashMap<>();
				}

				last = page;

				if (sections.get(firstLevel) == null) {
					sections.put(firstLevel, new LinkedHashSet<>());
				}
				if (secondLevel != null) {
					sections.get(firstLevel).add(secondLevel);
				}

				rowIndex++;
			}

			ArrayList<String> featureSets = new ArrayList<>(sections.keySet());
			parsedLines += sections.size();
			LOGGER.debug("" + sections.size() + " " + last);
			features.add(featureSets);

			LOGGER.info("Row index: {}", rowIndex);
			LOGGER.info("Parsed lines: {}", parsedLines);

			List<String> tmpResults = Files.readLines(resultsPath, Charsets.UTF_8);

			List<String> results = new ArrayList<>();
			List<String> golds = new ArrayList<>();
			for (String tmpResult : tmpResults) {
				tmpResult = tmpResult.trim();
				if (tmpResult.length() == 0) {
					continue;
				}

				String[] parts = tmpResult.split("\\s+");
				if (parts.length < 2) {
					continue;
				}

				results.add(parts[1]);
				golds.add(parts[0]);
			}

			LOGGER.info("Results lines: {}", results.size());
			LOGGER.info("Gold lines: {}", golds.size());

			LOGGER.info("Applying rules");

			BufferedWriter writer = new BufferedWriter(new FileWriter(outputPath));

			int count = 0;
			for (ArrayList<String> sectionList : features) {
				List<String> thisSectionResults = new ArrayList<>();
				List<String> thisSectionGolds = new ArrayList<>();

				Integer lastPositiveResult = null;
				Integer firstPositiveResult = null;

				for (int i = 0; i < sectionList.size(); i++) {
					String result = results.get(count);
					thisSectionResults.add(result);
					String gold = golds.get(count);
					thisSectionGolds.add(gold);
					count++;

					if (!result.equals("O")) {
						lastPositiveResult = i;
						if (firstPositiveResult == null) {
							firstPositiveResult = i;
						}
					}
				}

				if (firstPositiveResult != null) {
					for (int i = firstPositiveResult + 1; i <= lastPositiveResult; i++) {
						thisSectionResults.set(i, "I-SEC");
					}
				}

				// Add stuff
				for (String label : labelsToContinue) {
					Integer found = null;

					for (int i = 0; i < sectionList.size(); i++) {
						String section = sectionList.get(i);
						section = section.toLowerCase();
						if (section.equals(label)) {
							found = i;
						}
					}

					if (found != null) {
						LOGGER.debug("Found! {} {}", found, label);
						if (lastPositiveResult != null) {
							for (int i = lastPositiveResult + 1; i <= found; i++) {
								if (thisSectionResults.get(i).equals("O")) {
									thisSectionResults.set(i, "I-SEC");
								}
							}
						}
						else {
							thisSectionResults.set(found, "B-SEC");
						}
					}
				}
				for (String label : labelAttachedContinue) {
					Integer found = null;

					for (int i = 0; i < sectionList.size(); i++) {
						String section = sectionList.get(i);
						section = section.toLowerCase();
						if (section.equals(label)) {
							found = i;
						}
					}

					if (found != null) {
						LOGGER.debug("Found! {} {}", found, label);
						if (lastPositiveResult != null) {
							if (found == lastPositiveResult + 1) {
								if (thisSectionResults.get(found).equals("O")) {
									thisSectionResults.set(found, "I-SEC");
								}
							}
						}
						else {
							thisSectionResults.set(found, "B-SEC");
						}
					}
				}
				for (String label : labelEndsToContinue) {
					Integer found = null;

					for (int i = 0; i < sectionList.size(); i++) {
						String section = sectionList.get(i);
						section = section.toLowerCase();
						if (section.endsWith(label)) {
							found = i;
						}
					}

					if (found != null) {
						LOGGER.debug("Found! {} {}", found, label);
						if (lastPositiveResult != null) {
							for (int i = lastPositiveResult + 1; i <= found; i++) {
								if (thisSectionResults.get(i).equals("O")) {
									thisSectionResults.set(i, "I-SEC");
								}
							}
						}
						else {
							thisSectionResults.set(found, "B-SEC");
						}
					}
				}

				// Remove stuff
				for (String label : labelsToAvoid) {
					Integer found = null;

					for (int i = 0; i < sectionList.size(); i++) {
						String section = sectionList.get(i);
						section = section.toLowerCase();
						if (section.equals(label)) {
							found = i;
						}
					}

					if (found != null) {
						for (int i = found; i < thisSectionResults.size(); i++) {
							thisSectionResults.set(i, "O");
						}
					}
				}

				for (int i = 0; i < thisSectionResults.size(); i++) {
					writer.append(thisSectionGolds.get(i)).append("\t").append(thisSectionResults.get(i)).append("\n");
				}
//				System.out.println(sectionList);
//				System.out.println(thisSectionResults);
//				System.out.println(lastPositiveResult);
//				System.out.println();

			}


//			for (ArrayList<String> sectionList : features) {
//				System.out.println(sectionList);
//			}

			writer.close();

		} catch (final Throwable ex) {
			CommandLine.fail(ex);
		}
	}
}
