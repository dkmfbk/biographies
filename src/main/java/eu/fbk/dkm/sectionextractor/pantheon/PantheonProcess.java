package eu.fbk.dkm.sectionextractor.pantheon;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import eu.fbk.dkm.pikes.naflib.Corpus;
import eu.fbk.dkm.utils.CommandLine;
import ixa.kaflib.*;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by alessio on 27/04/15.
 */

public class PantheonProcess {

	private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(PantheonProcess.class);

	public static void main(String[] args) {
		try {
			final CommandLine cmd = CommandLine
					.parser()
					.withName("wikipedia-text-parser")
					.withHeader("Analyze text dumps from Wikipedia")
					.withOption("i", "input-path", "the corpus path", "DIR", CommandLine.Type.DIRECTORY_EXISTING, true, false, true)
					.withOption("l", "list", "list of pages to consider", "FILE", CommandLine.Type.FILE_EXISTING, true, false, false)
					.withOption("o", "output", "output file", "FILE", CommandLine.Type.FILE, true, false, true)
					.withLogger(LoggerFactory.getLogger("eu.fbk")).parse(args);

			final File inputPath = cmd.getOptionValue("input-path", File.class);
			final File listPath = cmd.getOptionValue("list", File.class);
			final File outputPath = cmd.getOptionValue("output", File.class);

			BufferedWriter writer = new BufferedWriter(new FileWriter(outputPath));

			HashSet<String> titlesToInclude = null;
			if (listPath != null) {
				LOGGER.info("Loading pages list from {}", listPath.getAbsolutePath());
				titlesToInclude = new HashSet<>();
				List<String> list = Files.readLines(listPath, Charsets.UTF_8);

				for (String line : list) {
					line = line.trim();
					if (line.length() == 0) {
						continue;
					}
					line = line.replaceAll("\\s+", "_");

					titlesToInclude.add(line);
				}
				LOGGER.info("Loaded {} pages", titlesToInclude.size());
			}

			Corpus corpus = Corpus.create(true, inputPath);
			int corpusSize = corpus.size();
			LOGGER.info("Corpus size: {}", corpusSize);

			final AtomicInteger i = new AtomicInteger(0);
			final HashSet<String> finalTitlesToInclude = titlesToInclude;

			corpus.parallelStream().forEach(document -> {

				int step = i.incrementAndGet();
				if (step % 100 == 0) {
					synchronized (document) {
						LOGGER.info("Parsed {}/{} documents", i, corpusSize);
					}
				}

				String title = document.getFileDesc().title.replaceAll("\\s+", "_");
				if (finalTitlesToInclude != null && !finalTitlesToInclude.contains(title)) {
					return;
				}

				List<Coref> corefs = document.getCorefs();

				// Male or female?
				int male = 0, female = 0;
				for (Coref coref : corefs) {
					for (Term term : coref.getTerms()) {
						if (term.getLemma().equals("she")) {
							female++;
						}
						if (term.getLemma().equals("he")) {
							male++;
						}
					}
				}

				String pronoun = "he";
				String adjective = "his";
				if (female > male) {
					pronoun = "she";
					adjective = "her";
				}

				// Get coreferences
				ArrayList<WF> coreferences = new ArrayList<>();
				for (Coref coref : corefs) {
					boolean isGoodCoref = false;
					for (Term term : coref.getTerms()) {
						if (term.getLemma().equals(pronoun) || term.getLemma().equals(adjective)) {
							isGoodCoref = true;
						}
					}

					if (!isGoodCoref) {
						continue;
					}

					for (Term term : coref.getTerms()) {
						coreferences.addAll(term.getWFs());
					}
				}

				// Get locations
				List<Entity> entities = document.getEntities();
				ArrayList<WF> locations = new ArrayList<>();
				for (Entity entity : entities) {
					if (entity.getType().equals("LOCATION")) {
						for (Term term : entity.getTerms()) {
							locations.addAll(term.getWFs());
						}
					}
				}

				if (locations.size() == 0) {
					return;
				}

				// Get timex
				ArrayList<WF> timexs = new ArrayList<>();
				for (Timex3 timex3 : document.getTimeExs()) {
					if (timex3.getSpan() == null) {
						continue;
					}
					for (WF wf : timex3.getSpan().getTargets()) {
						timexs.add(wf);
					}
				}

				if (timexs.size() == 0) {
					return;
				}

				// Sentences
				for (List<WF> wfs : document.getSentences()) {
					List<WF> thisSentPronoun = new ArrayList<>();
					List<WF> thisSentTimex = new ArrayList<>();
					List<WF> thisSentLocations = new ArrayList<>();

					for (WF wf : wfs) {
						if (locations.contains(wf)) {
							thisSentLocations.add(wf);
						}
						if (coreferences.contains(wf)) {
							thisSentPronoun.add(wf);
						}
						if (timexs.contains(wf)) {
							thisSentTimex.add(wf);
						}
						if (wf.getForm().toLowerCase().equals(pronoun)) {
							thisSentPronoun.add(wf);
						}
					}

					if (thisSentPronoun.size() * thisSentLocations.size() * thisSentTimex.size() == 0) {
						continue;
					}

					synchronized (document) {
						try {
							writer.append(title).append("\n");
							writer.append(wfs.toString()).append("\n");
							writer.append(thisSentPronoun.toString()).append("\n");
							writer.append(thisSentLocations.toString()).append("\n");
							writer.append(thisSentTimex.toString()).append("\n");
							writer.append("\n");
						} catch (Exception e) {
							LOGGER.error(e.getMessage());
						}
					}
				}
			});

/*			for (KAFDocument document : corpus) {
				i++;
				if (i % 100 == 0) {
					LOGGER.info("Parsed {}/{} documents", i, corpusSize);
				}

				String title = document.getFileDesc().title.replaceAll("\\s+", "_");
				if (titlesToInclude != null && !titlesToInclude.contains(title)) {
					continue;
				}

				List<Coref> corefs = document.getCorefs();

				// Male or female?
				int male = 0, female = 0;
				for (Coref coref : corefs) {
					for (Term term : coref.getTerms()) {
						if (term.getLemma().equals("she")) {
							female++;
						}
						if (term.getLemma().equals("he")) {
							male++;
						}
					}
				}
//				System.out.println(title);
//				System.out.println("Male: " + male);
//				System.out.println("Female: " + female);
//				System.out.println();

				String pronoun = "he";
				String adjective = "his";
				if (female > male) {
					pronoun = "she";
					adjective = "her";
				}

				// Get coreferences
				ArrayList<WF> coreferences = new ArrayList<>();
				for (Coref coref : corefs) {
					boolean isGoodCoref = false;
					for (Term term : coref.getTerms()) {
						if (term.getLemma().equals(pronoun) || term.getLemma().equals(adjective)) {
							isGoodCoref = true;
						}
					}

					if (!isGoodCoref) {
						continue;
					}

					for (Term term : coref.getTerms()) {
						coreferences.addAll(term.getWFs());
					}
				}

				// Get locations
				List<Entity> entities = document.getEntities();
				ArrayList<WF> locations = new ArrayList<>();
				for (Entity entity : entities) {
					if (entity.getType().equals("LOCATION")) {
						for (Term term : entity.getTerms()) {
							locations.addAll(term.getWFs());
						}
					}
				}

				if (locations.size() == 0) {
					continue;
				}

				// Get timex
				ArrayList<WF> timexs = new ArrayList<>();
				for (Timex3 timex3 : document.getTimeExs()) {
					if (timex3.getSpan() == null) {
						continue;
					}
					for (WF wf : timex3.getSpan().getTargets()) {
						timexs.add(wf);
					}
				}

				if (timexs.size() == 0) {
					continue;
				}

				// Sentences
				for (List<WF> wfs : document.getSentences()) {
					List<WF> thisSentPronoun = new ArrayList<>();
					List<WF> thisSentTimex = new ArrayList<>();
					List<WF> thisSentLocations = new ArrayList<>();

					for (WF wf : wfs) {
						if (locations.contains(wf)) {
							thisSentLocations.add(wf);
						}
						if (coreferences.contains(wf)) {
							thisSentPronoun.add(wf);
						}
						if (timexs.contains(wf)) {
							thisSentTimex.add(wf);
						}
						if (wf.getForm().toLowerCase().equals(pronoun)) {
							thisSentPronoun.add(wf);
						}
					}

					if (thisSentPronoun.size() * thisSentLocations.size() * thisSentTimex.size() == 0) {
						continue;
					}

					writer.append(title).append("\n");
					writer.append(wfs.toString()).append("\n");
					writer.append(thisSentPronoun.toString()).append("\n");
					writer.append(thisSentLocations.toString()).append("\n");
					writer.append(thisSentTimex.toString()).append("\n");
					writer.append("\n");
				}
			}*/

			writer.close();

		} catch (final Throwable ex) {
			CommandLine.fail(ex);
		}
	}
}
