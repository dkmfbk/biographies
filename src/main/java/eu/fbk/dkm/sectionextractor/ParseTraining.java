package eu.fbk.dkm.sectionextractor;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import eu.fbk.dkm.utils.CommandLine;
import org.fbk.cit.hlt.core.analysis.tokenizer.HardTokenizer;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by alessio on 13/06/15.
 */

public class ParseTraining {

	private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(ParseTraining.class);
	private static HashSet<String> labels = new HashSet<>();
	private static HashSet<String> noWindowFeatures = new HashSet<>();
	private static HardTokenizer tokenizer = HardTokenizer.getInstance();
	private static final Integer DEFAULT_WINDOW_SIZE = 0;

	private static Pattern numbersPattern = Pattern.compile("([0-9]+)");

	static {
		labels.add("Biography");
		labels.add("Life");
//		labels.add("Career");
//		labels.add("Death");

		noWindowFeatures.add("EOS");
		noWindowFeatures.add("BOS");
		noWindowFeatures.add("containsLifeYear");
	}

	private static FeatureSet extractFeatures(String text, HashMap<String, Integer> lifeInfos) {

		FeatureSet ret = new FeatureSet();

		HashSet<Integer> numbers = new HashSet<>();

		if (lifeInfos != null) {
			Integer born = lifeInfos.get("b");
			Integer died = lifeInfos.get("d");

			Matcher matcher = numbersPattern.matcher(text);
			while (matcher.find()) {
				numbers.add(Integer.parseInt(matcher.group(1)));
			}

			if (born == null && died != null) {
				born = died - 100;
			}
			if (born != null && died == null) {
				died = Math.min(born + 100, Calendar.getInstance().get(Calendar.YEAR));
			}

			if (born != null && died != null) {
				for (Integer number : numbers) {
					if (number >= born && number <= died) {
						ret.addFeature("[containsLifeYear]");
					}
				}

			}
		}

		List<String> strings = tokenizer.stringList(text);

		ret.addFeature("[text]" + text.replaceAll("\\s+", "_"));

		for (String string : strings) {
			ret.addFeature("[token]" + string);
			ret.addFeature("[tokenLower]" + string.toLowerCase());
		}

		for (int i = 0; i < strings.size() - 1; i++) {
			String string = strings.get(i) + "-" + strings.get(i + 1);
			ret.addFeature("[bigram]" + string);
		}

		if (strings.size() > 0) {
			ret.addFeature("[firstWord]" + strings.get(0));
//			ret.addFeature("[lastWord]" + strings.get(strings.size() - 1).toLowerCase());
		}

		if (strings.size() > 1) {
			ret.addFeature("[firstBigram]" + strings.get(0) + "-" + strings.get(1));
		}

		return ret;
	}

	private static ArrayList<FeatureSet> parseSections(LinkedHashMap<String, LinkedHashSet<String>> sections,
													   HashMap<String, Integer> lifeInfos,
													   boolean isTest) throws IOException {

		ArrayList<FeatureSet> ret = new ArrayList<>();

		if (isTest) {
			for (String firstLevel : sections.keySet()) {
				FeatureSet featureSet = extractFeatures(firstLevel, lifeInfos);
				featureSet.value = "0";
				ret.add(featureSet);
			}
		}
		else {

			// Filter out pages not having a section included in labels
			HashSet<String> labelsCopy = new HashSet<>(labels);
			labelsCopy.retainAll(sections.keySet());
			if (labelsCopy.size() == 0) {
				return null;
			}

			for (String firstLevel : sections.keySet()) {
				if (labels.contains(firstLevel)) {
					LinkedHashSet<String> subSections = sections.get(firstLevel);
					if (subSections.size() == 0) {
						FeatureSet featureSet = extractFeatures(firstLevel, lifeInfos);
						featureSet.value = "1";
						ret.add(featureSet);
					}
					else {
						for (String secondLevel : subSections) {
							FeatureSet featureSet = extractFeatures(secondLevel, lifeInfos);
							featureSet.value = "1";
							ret.add(featureSet);
						}
					}
				}
				else {
					FeatureSet featureSet = extractFeatures(firstLevel, lifeInfos);
					featureSet.value = "0";
					ret.add(featureSet);
				}
			}
		}

		return ret;
	}

	public static ArrayList<ArrayList<FeatureSet>> mergeFeatures(ArrayList<ArrayList<FeatureSet>> features) {
		for (ArrayList<FeatureSet> featureSets : features) {
			Integer first = null;
			Integer last = null;

			for (int i = 0; i < featureSets.size(); i++) {
				FeatureSet featureSet = featureSets.get(i);
				if (featureSet.value.equals("1")) {
					if (first == null) {
						first = i;
					}
					last = i;
				}
			}

			if (first != null) {
				for (int i = 0; i < featureSets.size(); i++) {
					FeatureSet featureSet = featureSets.get(i);
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
					.withOption("d", "dates", "dates file", "FILE", CommandLine.Type.FILE_EXISTING, true, false, false)
					.withOption("o", "output", "output file", "FILE", CommandLine.Type.FILE, true, false, true)
					.withOption("s", "size", "window size", "NUM", CommandLine.Type.NON_NEGATIVE_INTEGER, true, false, false)
					.withOption("g", "gold", "gold file (CSV format)", "FILE", CommandLine.Type.FILE_EXISTING, true, false, false)
					.withOption("t", "test", "is test")
					.withLogger(LoggerFactory.getLogger("eu.fbk")).parse(args);

			final File inputPath = cmd.getOptionValue("input", File.class);
			final File outputPath = cmd.getOptionValue("output", File.class);
			final File datesPath = cmd.getOptionValue("dates", File.class);

			final File goldPath = cmd.getOptionValue("gold", File.class);

			boolean isTest = cmd.hasOption("t");

			Integer windowSize = cmd.getOptionValue("size", Integer.class, DEFAULT_WINDOW_SIZE);

			HashMap<String, HashMap<String, Integer>> lifeInfos = new HashMap<>();
			if (datesPath != null) {
				LOGGER.info("Loading dates file");
				List<String> dateLines = Files.readLines(datesPath, Charsets.UTF_8);
				for (String dateLine : dateLines) {
					dateLine = dateLine.trim();
					if (dateLine.length() == 0) {
						continue;
					}

					String parts[] = dateLine.split("\t");
					if (parts.length < 3) {
						continue;
					}

					String page = parts[0];
					String type = parts[1];

					Integer year;
					try {
						year = Integer.parseInt(parts[2]);
					} catch (Exception e) {
						continue;
					}

					if (lifeInfos.get(page) == null) {
						lifeInfos.put(page, new HashMap<>());
					}
					lifeInfos.get(page).put(type, year);
				}

			}

			LOGGER.info("Starting features extraction");

			ArrayList<ArrayList<FeatureSet>> features = new ArrayList<>();

			List<String> lines = Files.readLines(inputPath, Charsets.UTF_8);
			LOGGER.info("Input lines: {}", lines.size());

			String last = null;
			LinkedHashMap<String, LinkedHashSet<String>> sections = new LinkedHashMap<>();
			HashSet<String> pages = new HashSet<>();

			int rowIndex = 0;
			int parsedLines = 0;
			for (String line : lines) {
				line = line.trim();
				String[] parts = line.split("\t");
				if (parts.length < 2) {
					LOGGER.warn("Row contains less than 2 tokens: {}", line);
					continue;
				}

				String page = parts[0];
				String firstLevel = parts[1];
				String secondLevel = null;
				if (parts.length >= 3) {
					secondLevel = parts[2];
				}

				if (last != null && !page.equals(last)) {
					ArrayList<FeatureSet> featureSets = parseSections(sections, lifeInfos.get(last), isTest);
					parsedLines += sections.size();
					LOGGER.debug("" + sections.size() + " " + last);
					if (featureSets != null) {
						features.add(featureSets);
						pages.add(last);
					}
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

			ArrayList<FeatureSet> featureSets = parseSections(sections, lifeInfos.get(last), isTest);
			parsedLines += sections.size();
			LOGGER.debug("" + sections.size() + " " + last);
			if (featureSets != null) {
				features.add(featureSets);
				pages.add(last);
			}

			LOGGER.info("Row index: {}", rowIndex);
			LOGGER.info("Parsed lines: {}", parsedLines);
			LOGGER.info("Pages: {}", pages.size());

			if (goldPath != null) {
				List<String> goldLines = Files.readLines(goldPath, Charsets.UTF_8);
				LOGGER.info("Gold rows: {}", goldLines.size());

				int count = 0;
				for (ArrayList<FeatureSet> featureList : features) {
					for (FeatureSet featureSet : featureList) {
						Character firstChar = goldLines.get(count++).charAt(0);
						if (firstChar == '1') {
							featureSet.value = "1";
						}
					}
				}

				LOGGER.info("Total rows (gold): {}", count);

			}

			LOGGER.info("Merging features");
			features = mergeFeatures(features);

			LOGGER.info("Post processing features");

			BufferedWriter writer = new BufferedWriter(new FileWriter(outputPath));

			for (ArrayList<FeatureSet> featureList : features) {
				for (int i = 0; i < featureList.size(); i++) {
					FeatureSet featureSet = new FeatureSet();
					featureSet.features.addAll(featureList.get(i).features);
					featureSet.value = featureList.get(i).value;

					if (i == 0) {
						featureSet.addFeature("[BOS]");
					}
					if (i == featureList.size() - 1) {
						featureSet.addFeature("[EOS]");
					}

					if (featureSet.value.equals("0")) {
						featureSet.value = "O";
					}
					if (featureSet.value.equals("1")) {
						if (i == 0 || featureList.get(i - 1).value.equals("0")) {
							featureSet.value = "B-SEC";
						}
//						else if (i == featureList.size() - 1 || featureList.get(i + 1).value.equals("0")) {
//							featureSet.value = "E-SEC";
//						}
						else {
							featureSet.value = "I-SEC";
						}
					}

					for (int j = i - windowSize; j <= i + windowSize; j++) {
						if (j == i || j >= featureList.size() || j < 0) {
							continue;
						}

						FeatureSet thisFS = featureList.get(j);
						featureLoop:
						for (String feature : thisFS.features) {
							for (String noWindowFeature : noWindowFeatures) {
								if (feature.startsWith("[" + noWindowFeature + "]")) {
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

			writer.close();

		} catch (final Throwable ex) {
			CommandLine.fail(ex);
		}
	}
}
