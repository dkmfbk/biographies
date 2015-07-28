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

public class Baseline {

	private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(Baseline.class);
	private static HashSet<String> labels = new HashSet<>();
	private static HashSet<String> endLabels = new HashSet<>();

	private static enum Strategies {
		EXACT_MATCH, CONTAINS
	}

	private static final Strategies DEFAULT_STRATEGY = Strategies.EXACT_MATCH;

	static {
		labels.add("Biography".toLowerCase());
		labels.add("Life".toLowerCase());
		labels.add("Death".toLowerCase());
		labels.add("Career".toLowerCase());

//		endLabels.add("Career".toLowerCase());
	}

	private static class BaselineSet {
		public String value = null;
		public String gold = null;
		public String text = null;

		public BaselineSet() {

		}

		@Override
		public String toString() {
			return toString(false);
		}

		public String toString(boolean compact) {
			if (compact) {
				return gold + "\t" + value;
			}
			else {
				return "BaselineSet{" +
						"value='" + value + '\'' +
						", gold='" + gold + '\'' +
						", text='" + text + '\'' +
						'}';
			}
		}
	}

	public static ArrayList<BaselineSet> getEmptySet(Collection<String> texts) {
		ArrayList<BaselineSet> ret = new ArrayList<>();

		for (String text : texts) {
			BaselineSet set = new BaselineSet();
			set.text = text;
			set.value = "0";
			set.gold = "0";
			ret.add(set);
		}

		return ret;
	}

	public static ArrayList<ArrayList<BaselineSet>> mergeFeatures(ArrayList<ArrayList<BaselineSet>> features) {
		for (ArrayList<BaselineSet> featureSets : features) {
			Integer first = null;
			Integer last = null;

			for (int i = 0; i < featureSets.size(); i++) {
				BaselineSet featureSet = featureSets.get(i);
				if (featureSet.value.equals("1")) {
					if (first == null) {
						first = i;
					}
					last = i;
				}
			}

			if (first != null) {
				for (int i = 0; i < featureSets.size(); i++) {
					BaselineSet featureSet = featureSets.get(i);
					if (i >= first && i <= last) {
						featureSet.value = "1";
					}
				}
			}
		}

		return features;
	}

	public static void main(String[] args) {
		try {
			final CommandLine cmd = CommandLine
					.parser()
					.withName("wikipedia-text-parser")
					.withHeader("Analyze text dumps from Wikipedia")
					.withOption("i", "input", "input file", "FILE", CommandLine.Type.FILE_EXISTING, true, false, true)
					.withOption("o", "output", "output file", "FILE", CommandLine.Type.FILE, true, false, true)
					.withOption("g", "gold", "gold file (CSV format)", "FILE", CommandLine.Type.FILE_EXISTING, true, false, false)
					.withOption("s", "strategy", String.format("strategy, default %s", DEFAULT_STRATEGY), "TXT", CommandLine.Type.STRING, true, false, false)
					.withLogger(LoggerFactory.getLogger("eu.fbk")).parse(args);

			final File inputPath = cmd.getOptionValue("input", File.class);
			final File outputPath = cmd.getOptionValue("output", File.class);

			final File goldPath = cmd.getOptionValue("gold", File.class);

			Strategies strategy = DEFAULT_STRATEGY;
			if (cmd.hasOption("strategy")) {
				try {
					strategy = Strategies.valueOf(cmd.getOptionValue("strategy", String.class).toUpperCase());
				} catch (Exception e) {
					LOGGER.warn(e.getMessage() + ", using " + DEFAULT_STRATEGY);
				}
			}

			LOGGER.info("Starting features extraction");

			List<String> lines = Files.readLines(inputPath, Charsets.UTF_8);
			LOGGER.info("Input lines: {}", lines.size());

			String last = null;
			LinkedHashMap<String, LinkedHashSet<String>> sections = new LinkedHashMap<>();
			ArrayList<ArrayList<BaselineSet>> features = new ArrayList<>();

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
					ArrayList<BaselineSet> featureSets = getEmptySet(sections.keySet());
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

			ArrayList<BaselineSet> featureSets = getEmptySet(sections.keySet());
			parsedLines += sections.size();
			LOGGER.debug("" + sections.size() + " " + last);
			features.add(featureSets);

			LOGGER.info("Row index: {}", rowIndex);
			LOGGER.info("Parsed lines: {}", parsedLines);

			if (goldPath != null) {
				List<String> goldLines = Files.readLines(goldPath, Charsets.UTF_8);
				LOGGER.info("Gold rows: {}", goldLines.size());

				int count = 0;
				for (ArrayList<BaselineSet> featureList : features) {
					for (BaselineSet featureSet : featureList) {
						Character firstChar = goldLines.get(count++).charAt(0);
						if (firstChar == '1') {
							featureSet.gold = "1";
						}

						if (strategy == Strategies.EXACT_MATCH) {
							if (labels.contains(featureSet.text.toLowerCase())) {
								featureSet.value = "1";
							}
							if (endLabels.contains(featureSet.text.toLowerCase())) {
								featureSet.value = "1";
							}
						}
						else if (strategy == Strategies.CONTAINS) {
							for (String label : labels) {
								if (featureSet.text.toLowerCase().contains(label)) {
									featureSet.value = "1";
								}
							}
							for (String label : endLabels) {
								if (featureSet.text.toLowerCase().endsWith(label)) {
									featureSet.value = "1";
								}
							}

						}

//						System.out.println(featureSet);
					}
				}

				LOGGER.info("Total rows: {}", count);
			}

			LOGGER.info("Merging features");
			features = mergeFeatures(features);

//			for (ArrayList<BaselineSet> baselineSets : features) {
//				for (BaselineSet baselineSet : baselineSets) {
//					System.out.println(baselineSet);
//				}
//				System.out.println();
//			}

			LOGGER.info("Post processing features");

			BufferedWriter writer = new BufferedWriter(new FileWriter(outputPath));

			for (ArrayList<BaselineSet> featureList : features) {
				for (int i = 0; i < featureList.size(); i++) {
					BaselineSet baselineSet = featureList.get(i);

					// Values
					if (baselineSet.value.equals("0")) {
						baselineSet.value = "O";
					}
					if (baselineSet.value.equals("1")) {
						if (i == 0 || featureList.get(i - 1).value.equals("0") || featureList.get(i - 1).value.equals("O")) {
							baselineSet.value = "B-SEC";
						}
						else {
							baselineSet.value = "I-SEC";
						}
					}

					// Gold
					if (baselineSet.gold.equals("0")) {
						baselineSet.gold = "O";
					}
					if (baselineSet.gold.equals("1")) {
						if (i == 0 || featureList.get(i - 1).gold.equals("0") || featureList.get(i - 1).gold.equals("O")) {
							baselineSet.gold = "B-SEC";
						}
						else {
							baselineSet.gold = "I-SEC";
						}
					}

					writer.append(baselineSet.toString(true)).append("\n");
				}

				writer.append("\n");
			}

			writer.close();

		} catch (final Throwable ex) {
			CommandLine.fail(ex);
		}
	}
}
