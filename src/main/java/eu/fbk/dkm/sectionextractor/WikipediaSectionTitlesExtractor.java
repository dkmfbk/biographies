/*
 * Copyright (2013) Fondazione Bruno Kessler (http://www.fbk.eu/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package eu.fbk.dkm.sectionextractor;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import de.tudarmstadt.ukp.wikipedia.parser.ParsedPage;
import de.tudarmstadt.ukp.wikipedia.parser.Section;
import de.tudarmstadt.ukp.wikipedia.parser.SectionContainer;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.fbk.cit.hlt.thewikimachine.ExtractorParameters;
import org.fbk.cit.hlt.thewikimachine.util.CommandLineWithLogger;
import org.fbk.cit.hlt.thewikimachine.xmldump.AbstractWikipediaExtractor;
import org.fbk.cit.hlt.thewikimachine.xmldump.AbstractWikipediaXmlDumpParser;
import org.fbk.cit.hlt.thewikimachine.xmldump.WikipediaExtractor;
import org.fbk.cit.hlt.thewikimachine.xmldump.util.WikiMarkupParser;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class WikipediaSectionTitlesExtractor extends AbstractWikipediaExtractor implements WikipediaExtractor {

	private static final int MAX_DEPTH = 10;
	static Logger logger = Logger.getLogger(WikipediaSectionTitlesExtractor.class.getName());
	BufferedWriter writer;
	Integer configuredDepth = null;
	Integer maxNum = 0;
	boolean printTitles = false;
	HashSet<String> pagesToConsider = null;

//	private static HashSet<String> SKIP_SECTIONS = new HashSet<>();

//	static {
//		SKIP_SECTIONS.add("references");
//		SKIP_SECTIONS.add("related pages");
//		SKIP_SECTIONS.add("other websites");
//	}

	public WikipediaSectionTitlesExtractor(int numThreads, int numPages, Locale locale, File outFile, Integer configuredDepth, int maxNum, boolean printTitles, HashSet<String> pagesToConsider) {
		super(numThreads, numPages, locale);

		logger.info("Locale: " + locale);
		logger.info("Page to consider: " + pagesToConsider.size());
		logger.info("Configured depth: " + configuredDepth);
		logger.info("Max number of sections: " + maxNum);
		logger.info("Print titles: " + Boolean.toString(printTitles));
		logger.info("Output file: " + outFile);

		this.configuredDepth = configuredDepth;
		this.maxNum = maxNum;
		this.printTitles = printTitles;
		this.pagesToConsider = pagesToConsider;

		try {
			writer = new BufferedWriter(new FileWriter(outFile));
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
	}

	@Override
	public void start(ExtractorParameters extractorParameters) {
		startProcess(extractorParameters.getWikipediaXmlFileName());
	}

	@Override
	public void filePage(String text, String title, int wikiID) {
		//To change body of implemented methods use File | Settings | File Templates.
	}

	private void printSections(Collection<Section> sections, String prefix, int maxNum) throws IOException {
		printSections(sections, prefix, maxNum, 0);
	}

	private void printSections(Collection<Section> sections, String prefix, int maxNum, int depth) throws IOException {

		if (depth > MAX_DEPTH) {
			return;
		}

//		System.out.println("C: " + configuredDepth);
//		System.out.println("D: " + depth);

		if (configuredDepth != null && depth >= configuredDepth) {
			return;
		}

		int i = 0;
		for (Section section : sections) {

			String sectionTitle = section.getTitle();

			if (sectionTitle != null) {
				if (maxNum > 0 && ++i > maxNum) {
					continue;
				}

				sectionTitle = sectionTitle.replaceAll("\\s+", " ");

				StringBuffer newPrefix = new StringBuffer();
				newPrefix.append(prefix).append("\t").append(sectionTitle);
				writer.append(newPrefix).append("\n");
				if (section.getClass() == SectionContainer.class) {
					printSections(((SectionContainer) section).getSubSections(), newPrefix.toString(), maxNum, depth + 1);
				}
			}
		}

	}

	@Override
	public void contentPage(String text, String title, int wikiID) {

		if (pagesToConsider != null && !pagesToConsider.contains(title)) {
			return;
		}

		try {

			WikiMarkupParser wikiMarkupParser = WikiMarkupParser.getInstance();
			String[] prefixes = {imagePrefix, filePrefix};
			ParsedPage parsedPage = wikiMarkupParser.parsePage(text, prefixes);

			if (printTitles) {
				writer.append(title).append("\n");
			}

			synchronized (this) {
				printSections(parsedPage.getSections(), title, maxNum);
			}
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("Error processing page " + title + " (" + wikiID + ")");
		}
	}

	@Override
	public void disambiguationPage(String text, String title, int wikiID) {
		//To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public void categoryPage(String text, String title, int wikiID) {
		//To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public void templatePage(String text, String title, int wikiID) {
		//To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public void redirectPage(String text, String title, int wikiID) {
		//To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public void portalPage(String text, String title, int wikiID) {
		//To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public void projectPage(String text, String title, int wikiID) {
		//To change body of implemented methods use File | Settings | File Templates.
	}

	public void endProcess() {
		super.endProcess();
		try {
			writer.close();
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
	}

	public static void main(String args[]) throws IOException {

		CommandLineWithLogger commandLineWithLogger = new CommandLineWithLogger();
		commandLineWithLogger.addOption(OptionBuilder.withArgName("file").hasArg().withDescription("wikipedia xml dump file").isRequired().withLongOpt("wikipedia-dump").create("d"));
		commandLineWithLogger.addOption(OptionBuilder.withArgName("file").hasArg().withDescription("Filter file").withLongOpt("filter").create("f"));
		commandLineWithLogger.addOption(OptionBuilder.withArgName("dir").hasArg().withDescription("output file").isRequired().withLongOpt("output-file").create("o"));
		commandLineWithLogger.addOption(OptionBuilder.withArgName("int").hasArg().withDescription("max depth (default " + MAX_DEPTH + ")").withLongOpt("max-depth").create("m"));
		commandLineWithLogger.addOption(OptionBuilder.withArgName("int").hasArg().withDescription("max num of sections").withLongOpt("max-num").create("n"));
		commandLineWithLogger.addOption(new Option("l", "print titles"));

		commandLineWithLogger.addOption(OptionBuilder.withArgName("int").hasArg().withDescription("number of threads (default " + AbstractWikipediaXmlDumpParser.DEFAULT_THREADS_NUMBER + ")").withLongOpt("num-threads").create("t"));
		commandLineWithLogger.addOption(OptionBuilder.withArgName("int").hasArg().withDescription("number of pages to process (default all)").withLongOpt("num-pages").create("p"));
		commandLineWithLogger.addOption(OptionBuilder.withArgName("int").hasArg().withDescription("receive notification every n pages (default " + AbstractWikipediaExtractor.DEFAULT_NOTIFICATION_POINT + ")").withLongOpt("notification-point").create("b"));

		CommandLine commandLine = null;
		try {
			commandLine = commandLineWithLogger.getCommandLine(args);
			PropertyConfigurator.configure(commandLineWithLogger.getLoggerProps());
		} catch (Exception e) {
			System.exit(1);
		}

		int numThreads = Integer.parseInt(commandLine.getOptionValue("num-threads", Integer.toString(AbstractWikipediaXmlDumpParser.DEFAULT_THREADS_NUMBER)));
		int numPages = Integer.parseInt(commandLine.getOptionValue("num-pages", Integer.toString(AbstractWikipediaExtractor.DEFAULT_NUM_PAGES)));
		int notificationPoint = Integer.parseInt(commandLine.getOptionValue("notification-point", Integer.toString(AbstractWikipediaExtractor.DEFAULT_NOTIFICATION_POINT)));

		int configuredDepth = Integer.parseInt(commandLine.getOptionValue("max-depth", Integer.toString(MAX_DEPTH)));
		int maxNum = Integer.parseInt(commandLine.getOptionValue("max-num", "0"));
		boolean printTitles = commandLine.hasOption("l");

		HashSet<String> pagesToConsider = null;
		String filterFileName = commandLine.getOptionValue("filter");
		if (filterFileName != null) {
			File filterFile = new File(filterFileName);
			if (filterFile.exists()) {
				pagesToConsider = new HashSet<>();
				List<String> lines = Files.readLines(filterFile, Charsets.UTF_8);
				for (String line : lines) {
					line = line.trim();
					if (line.length() == 0) {
						continue;
					}

					line = line.replaceAll("\\s+", "_");

					pagesToConsider.add(line);
				}
			}
		}

		File outputFile = new File(commandLine.getOptionValue("output-file"));
		ExtractorParameters extractorParameters = new ExtractorParameters(commandLine.getOptionValue("wikipedia-dump"), outputFile.getAbsolutePath());

		WikipediaExtractor wikipediaPageParser = new WikipediaSectionTitlesExtractor(numThreads, numPages, extractorParameters.getLocale(), outputFile, configuredDepth, maxNum, printTitles, pagesToConsider);
		wikipediaPageParser.setNotificationPoint(notificationPoint);
		wikipediaPageParser.start(extractorParameters);

		logger.info("extraction ended " + new Date());

	}

}