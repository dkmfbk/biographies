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

package eu.fbk.dkm.sectionextractor.pantheon;

import com.google.common.base.Charsets;
import com.google.common.base.Splitter;
import com.google.common.io.Files;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.ArrayCoreMap;
import edu.stanford.nlp.util.CoreMap;
import eu.fbk.dkm.sectionextractor.WikipediaText;
import ixa.kaflib.KAFDocument;
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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class WikipediaGoodTextExtractor extends AbstractWikipediaExtractor implements WikipediaExtractor {
	private static final int MAX_DEPTH = 10;
	/**
	 * Define a static logger variable so that it references the
	 * Logger instance named <code>WikipediaTextExtractor</code>.
	 */
	static Logger logger = Logger.getLogger(WikipediaGoodTextExtractor.class.getName());
	static final String eosTag = "<eos>";

	private File baseFolder;
	private boolean NAFformat;
	private boolean useStanford;
	private HashSet<String> pagesToConsider = null;
	private StanfordCoreNLP pipeline = null;
	private HashMap<Integer, String> idCategory;

	private static Integer MAX_FILES_PER_FOLDER = 1000;

	public WikipediaGoodTextExtractor(int numThreads, int numPages, Locale locale, File baseFolder, boolean NAFformat, HashSet<String> pagesToConsider, boolean useStanford, HashMap<Integer, String> idCategory) {
		super(numThreads, numPages, locale);
		logger.info("Category prefix: " + categoryPrefix);
		this.baseFolder = baseFolder;
		this.NAFformat = NAFformat;
		this.useStanford = useStanford;
		this.pagesToConsider = pagesToConsider;
		this.idCategory = idCategory;

		if (useStanford) {
			Properties props = new Properties();
			props.setProperty("annotators", "tokenize, ssplit");
			props.setProperty("ssplit.newlineIsSentenceBreak", "always");
			pipeline = new StanfordCoreNLP(props);
		}
	}

	private static final String[] SUBST_CHARS = {"(", ")", "[", "]", "{", "}"};
	private static final String[] REPLACE_SUBSTS = {"-LRB-", "-RRB-", "-LSB-", "-RSB-", "-LCB-", "-RCB-"};

	public static String parenthesisToCode(String input) {
		for (int i = 0; i < SUBST_CHARS.length; i++) {
			if (input.equals(SUBST_CHARS[i])) {
				return REPLACE_SUBSTS[i];
			}
		}
		return input;
	}

	public static String codeToParenthesis(String input) {
		for (int i = 0; i < REPLACE_SUBSTS.length; i++) {
			if (input.toUpperCase().equals(REPLACE_SUBSTS[i])) {
				return SUBST_CHARS[i];
			}
		}
		return input;
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
	public void contentPage(String text, String title, int wikiID) {

		if (pagesToConsider != null && !pagesToConsider.contains(title)) {
			return;
		}
		if (idCategory != null && !idCategory.keySet().contains(wikiID)) {
			return;
		}

		try {

			String okTitle = title.trim().replace('_', ' ');

			String folderNumberName = Integer.toString(wikiID / MAX_FILES_PER_FOLDER);
			String fileName = Integer.toString(wikiID);

			String folderName = baseFolder.getAbsolutePath() + File.separator + folderNumberName;
			if (idCategory.get(wikiID) != null) {
				folderName = baseFolder.getAbsolutePath() + File.separator + idCategory.get(wikiID) + File.separator + folderNumberName;
			}
			File folder = new File(folderName);
			if (!folder.exists()) {
				folder.mkdirs();
			}

			if (NAFformat) {
				fileName += ".naf";
			}
			else {
				fileName += ".txt";
			}
			String defFileName = folder.getAbsolutePath() + File.separator + fileName;

			File file = new File(defFileName);

			StringBuffer buffer = new StringBuffer();

			WikipediaText wikipediaText = new WikipediaText();
			buffer.append(wikipediaText.parse(text, null));

			String rawText = buffer.toString();
			buffer = new StringBuffer();
			buffer.append(okTitle).append("\n").append("\n");
			List<String> strings = Splitter.on('\n').trimResults()./*omitEmptyStrings().*/splitToList(rawText);
			for (String line : strings) {
				if (line.startsWith(categoryPrefix)) {
					continue;
				}
				if (line.startsWith("new:")) {
					continue;
				}
				buffer.append(line).append("\n");
			}

			rawText = buffer.toString();

			if (useStanford) {
				Annotation myDoc = new Annotation(rawText);
				pipeline.annotate(myDoc);

				StringBuffer tokenizedString = new StringBuffer();

				List<CoreMap> sents = myDoc.get(CoreAnnotations.SentencesAnnotation.class);
				for (CoreMap thisSent : sents) {
					ArrayCoreMap sentenceCoreMap = (ArrayCoreMap) thisSent;
					List<CoreLabel> tokens = sentenceCoreMap.get(CoreAnnotations.TokensAnnotation.class);
					for (CoreLabel token : tokens) {
						tokenizedString.append(codeToParenthesis(token.toString())).append("\n");
					}
					tokenizedString.append(eosTag).append("\n");
				}

				rawText = tokenizedString.toString();
			}

			if (NAFformat) {

				final KAFDocument document = new KAFDocument("en", "v3");

				document.setRawText(rawText);

				document.createPublic();
				document.getPublic().uri = String.format("https://%s.wikipedia.org/wiki/%s", getLocale().getLanguage(), title);

				document.createFileDesc();
				document.getFileDesc().author = "MediaWiki";
				document.getFileDesc().filename = fileName;
				document.getFileDesc().filetype = "Wikipedia article";
				document.getFileDesc().title = okTitle;

				document.save(file.getAbsolutePath());
			}
			else {
				try {
					logger.debug("Writing file " + file.getAbsolutePath());
					BufferedWriter writer = new BufferedWriter(new FileWriter(file));
					writer.write(rawText);
					writer.close();
				} catch (Exception e) {
					logger.error(e.getMessage());
				}
			}

		} catch (Exception e) {
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
	}

	public static void main(String args[]) throws IOException {

		CommandLineWithLogger commandLineWithLogger = new CommandLineWithLogger();
		commandLineWithLogger.addOption(OptionBuilder.withArgName("file").hasArg().withDescription("wikipedia xml dump file").isRequired().withLongOpt("wikipedia-dump").create("d"));
		commandLineWithLogger.addOption(OptionBuilder.withArgName("dir").hasArg().withDescription("output directory in which to store output files").isRequired().withLongOpt("output-dir").create("o"));
		commandLineWithLogger.addOption(OptionBuilder.withDescription("use NAF format").withLongOpt("naf").create("n"));
		commandLineWithLogger.addOption(OptionBuilder.withDescription("tokenize and ssplit with Stanford").withLongOpt("stanford").create("s"));

		commandLineWithLogger.addOption(OptionBuilder.withArgName("file").hasArg().withDescription("Filter file").withLongOpt("filter").create("f"));
		commandLineWithLogger.addOption(OptionBuilder.withArgName("file").hasArg().withDescription("ID and category file").withLongOpt("idcat").create("i"));
		commandLineWithLogger.addOption(OptionBuilder.withArgName("file").hasArg().withDescription("Redirect file").withLongOpt("redirect").create("r"));

		commandLineWithLogger.addOption(OptionBuilder.withArgName("int").hasArg().withDescription("number of threads (default " + AbstractWikipediaXmlDumpParser.DEFAULT_THREADS_NUMBER + ")").withLongOpt("num-threads").create("t"));
		commandLineWithLogger.addOption(OptionBuilder.withArgName("int").hasArg().withDescription("number of pages to process (default all)").withLongOpt("num-pages").create("p"));
		commandLineWithLogger.addOption(OptionBuilder.withArgName("int").hasArg().withDescription("receive notification every n pages (default " + AbstractWikipediaExtractor.DEFAULT_NOTIFICATION_POINT + ")").withLongOpt("notification-point").create("b"));
		commandLineWithLogger.addOption(new Option("n", "NAF format"));

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

		boolean nafFormat = commandLine.hasOption("n");
		boolean useStanford = commandLine.hasOption("s");

		HashMap<Integer, String> idCategory = new HashMap<>();
		String idcatFileName = commandLine.getOptionValue("idcat");
		if (idcatFileName != null) {
			logger.info("Loading categories");
			File idcatFile = new File(idcatFileName);
			if (idcatFile.exists()) {
				List<String> lines = Files.readLines(idcatFile, Charsets.UTF_8);
				for (String line : lines) {
					line = line.trim();
					if (line.length() == 0) {
						continue;
					}

					String[] parts = line.split("\\s+");
					if (parts.length < 3) {
						continue;
					}

					idCategory.put(Integer.parseInt(parts[1]), parts[2]);
				}
			}
		}

		HashMap<String, String> redirects = new HashMap<>();
		String redirectFileName = commandLine.getOptionValue("redirect");
		if (redirectFileName != null) {
			logger.info("Loading redirects");
			File redirectFile = new File(redirectFileName);
			if (redirectFile.exists()) {
				List<String> lines = Files.readLines(redirectFile, Charsets.UTF_8);
				for (String line : lines) {
					line = line.trim();
					if (line.length() == 0) {
						continue;
					}

					String[] parts = line.split("\\t+");
					if (parts.length < 2) {
						continue;
					}

					redirects.put(parts[0], parts[1]);
				}
			}
		}

		HashSet<String> pagesToConsider = null;
		String filterFileName = commandLine.getOptionValue("filter");
		if (filterFileName != null) {
			logger.info("Loading file list");
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

					addRedirects(pagesToConsider, redirects, line, 0);
				}
			}
		}

		ExtractorParameters extractorParameters = new ExtractorParameters(commandLine.getOptionValue("wikipedia-dump"), commandLine.getOptionValue("output-dir"));

		File outputFolder = new File(commandLine.getOptionValue("output-dir"));
		if (!outputFolder.exists()) {
			boolean mkdirs = outputFolder.mkdirs();
			if (!mkdirs) {
				throw new IOException("Unable to create folder " + outputFolder.getAbsolutePath());
			}
		}

		WikipediaExtractor wikipediaPageParser = new WikipediaGoodTextExtractor(numThreads, numPages, extractorParameters.getLocale(), outputFolder, nafFormat, pagesToConsider, useStanford, idCategory);
		wikipediaPageParser.setNotificationPoint(notificationPoint);
		wikipediaPageParser.start(extractorParameters);

		logger.info("extraction ended " + new Date());

	}

	private static void addRedirects(HashSet<String> pagesToConsider, HashMap<String, String> redirects, String line, int depth) {
		if (depth > MAX_DEPTH) {
			return;
		}

		String red = redirects.get(line);
		if (red != null) {
			pagesToConsider.add(red);
			addRedirects(pagesToConsider, redirects, red, depth + 1);
		}
	}

}