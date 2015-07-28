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

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.OptionBuilder;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.fbk.cit.hlt.thewikimachine.ExtractorParameters;
import org.fbk.cit.hlt.thewikimachine.util.CommandLineWithLogger;
import org.fbk.cit.hlt.thewikimachine.xmldump.AbstractWikipediaExtractor;
import org.fbk.cit.hlt.thewikimachine.xmldump.AbstractWikipediaXmlDumpParser;
import org.fbk.cit.hlt.thewikimachine.xmldump.WikipediaExtractor;
import org.fbk.cit.hlt.thewikimachine.xmldump.WikipediaTemplateExtractor;
import org.fbk.cit.hlt.thewikimachine.xmldump.util.WikiTemplate;
import org.fbk.cit.hlt.thewikimachine.xmldump.util.WikiTemplateParser;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WikipediaBirthDeathDateExtractor extends AbstractWikipediaExtractor implements WikipediaExtractor {
	/**
	 * Define a static logger variable so that it references the
	 * Logger instance named <code>WikipediaTemplateExtractor</code>.
	 */
	static Logger logger = Logger.getLogger(WikipediaTemplateExtractor.class.getName());
	private BufferedWriter writer = null;
	private static Pattern aDate = Pattern.compile("([0-9]+)(\\s+BC)?$");
	private static Pattern aDate2 = Pattern.compile("^(-)?([0-9]+)-([0-9]+)-([0-9]+)$");

	public WikipediaBirthDeathDateExtractor(int numThreads, int numPages, Locale locale, File outFile) {
		super(numThreads, numPages, locale);
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

	@Override
	public void disambiguationPage(String text, String title, int wikiID) {
		//To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public void categoryPage(String text, String title, int wikiID) {
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


	@Override
	public void templatePage(String text, String title, int wikiID) {

	}

	synchronized public void writeLine(String text) {
		try {
			writer.append(text);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void extractDate(String date, String title, String prefix) {
		if (date == null) {
			return;
		}

		if (date.trim().length() == 0) {
			return;
		}

		Matcher matcher;

		matcher = aDate2.matcher(date);
		if (matcher.find()) {

			StringBuilder builder = new StringBuilder();
			builder.append(title).append("\t").append(prefix).append("\t");
			if (matcher.group(1) != null) {
				builder.append(matcher.group(1));
			}
			builder.append(matcher.group(2)).append("\n");
			writeLine(builder.toString());
			return;
		}

		matcher = aDate.matcher(date);
		if (matcher.find()) {
			StringBuilder builder = new StringBuilder();
			builder.append(title).append("\t").append(prefix).append("\t");
			if (matcher.group(2) != null) {
				if (matcher.group(2).trim().length() > 0) {
					builder.append("-");
				}
			}
			builder.append(matcher.group(1)).append("\n");
			writeLine(builder.toString());
		}
	}

	@Override
	public void contentPage(String text, String title, int wikiID) {
		ArrayList<WikiTemplate> listOfTemplates = WikiTemplateParser.parse(text, false);

		for (WikiTemplate t : listOfTemplates) {
			HashMap<String, String> parts = t.getHashMapOfParts();
			if (t.getFirstPart() == null) {
				continue;
			}
			String name = t.getFirstPart().toLowerCase();

			if (!name.equals("persondata")) {
				continue;
			}

			extractDate(parts.get("DATE OF BIRTH"), title, "b");
			extractDate(parts.get("DATE OF DEATH"), title, "d");
		}
	}

	@Override
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
		commandLineWithLogger.addOption(OptionBuilder.withArgName("dir").hasArg().withDescription("output file").isRequired().withLongOpt("output-file").create("o"));

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

		File outputFile = new File(commandLine.getOptionValue("output-file"));
		ExtractorParameters extractorParameters = new ExtractorParameters(commandLine.getOptionValue("wikipedia-dump"), outputFile.getAbsolutePath());

		WikipediaExtractor wikipediaPageParser = new WikipediaBirthDeathDateExtractor(numThreads, numPages, extractorParameters.getLocale(), outputFile);
		wikipediaPageParser.setNotificationPoint(notificationPoint);
		wikipediaPageParser.start(extractorParameters);

		logger.info("extraction ended " + new Date());

	}

}