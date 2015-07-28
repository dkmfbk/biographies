package eu.fbk.dkm.sectionextractor;

import eu.fbk.dkm.utils.CommandLine;
import ixa.kaflib.KAFDocument;
import org.fbk.cit.hlt.thewikimachine.analysis.HardTokenizer;
import org.fbk.cit.hlt.thewikimachine.analysis.Tokenizer;
import org.fbk.cit.hlt.thewikimachine.xmldump.util.ParsedPageTitle;
import org.openrdf.model.impl.URIImpl;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.text.Normalizer;

/**
 * Created by alessio on 27/04/15.
 */

public class CorpusParser {

	private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(CorpusParser.class);
	static String NAMESPACE = "https://simple.wikipedia.org/wiki/";

	public static void main(String[] args) {
		try {
			final CommandLine cmd = CommandLine
					.parser()
					.withName("wikipedia-text-parser")
					.withHeader("Analyze text dumps from Wikipedia")
					.withOption("i", "input-path", "the corpus", "FILE", CommandLine.Type.FILE_EXISTING, true, false, true)
					.withOption("o", "output-path", "output folder", "DIR", CommandLine.Type.DIRECTORY, true, false, true)
					.withLogger(LoggerFactory.getLogger("eu.fbk.fssa")).parse(args);

			final File inputPath = cmd.getOptionValue("i", File.class);

			final File outputPath = cmd.getOptionValue("o", File.class);
			if (outputPath.exists() && !outputPath.isDirectory()) {
				LOGGER.error("Unable to write to destination folder {}", outputPath.getAbsolutePath());
				System.exit(1);
			}
			outputPath.mkdirs();

			Tokenizer tokenizer = HardTokenizer.getInstance();

			BufferedReader reader = new BufferedReader(new FileReader(inputPath));

			String line;
			while ((line = reader.readLine()) != null) {
				String[] parts = line.split("\\s+");
				if (parts.length < 1) {
					continue;
				}

				String pageName = parts[0];
				pageName = pageName.replaceAll("/", "_");

				String pageNameForFile = Normalizer.normalize(pageName, Normalizer.Form.NFD);

				ParsedPageTitle parsedRedirectPage = new ParsedPageTitle(pageName);
				String tokenizedPageName = tokenizer.tokenizedString(parsedRedirectPage.getForm());

				String pageFolder = pageNameForFile.toLowerCase().substring(0, 1);
				File pageFolderFile = new File(outputPath + File.separator + pageFolder);
				if (!pageFolderFile.exists()) {
					pageFolderFile.mkdirs();
				}

				int totalLen = pageName.length() + 1 + tokenizedPageName.length();
				if (line.length() <= totalLen) {
					continue;
				}

				String text = line.substring(totalLen).trim();

				// Fix: remove categories from the end of the text
				int firstCatIndex = text.indexOf("Category :");
				if (firstCatIndex != -1) {
					text = text.substring(0, firstCatIndex).trim();
				}

				String documentURI = NAMESPACE + pageName;
				String nafFileName = pageNameForFile + ".xml";
				File nafFile = new File(outputPath + File.separator + pageFolder + File.separator + nafFileName);

				final KAFDocument document = new KAFDocument("en", "v3");
				document.setRawText(text);
				document.createPublic();
				document.getPublic().publicId = new URIImpl(documentURI).getLocalName();
				document.getPublic().uri = documentURI;
				document.createFileDesc();
				document.getFileDesc().filename = nafFileName;
				document.getFileDesc().title = "-";
				document.save(nafFile.getAbsolutePath());

			}

			reader.close();
		} catch (final Throwable ex) {
			CommandLine.fail(ex);
		}
	}
}
