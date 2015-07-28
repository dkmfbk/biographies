package eu.fbk.dkm.sectionextractor.cliques;

import eu.fbk.dkm.utils.CommandLine;
import eu.fbk.dkm.utils.FrequencyHashSet;
import org.fbk.cit.hlt.thewikimachine.index.FormPageSearcher;
import org.fbk.cit.hlt.thewikimachine.index.PageCategorySearcher;
import org.fbk.cit.hlt.thewikimachine.index.util.FreqSetSearcher;
import org.fbk.cit.hlt.thewikimachine.util.DBpediaOntology;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

/**
 * Created by alessio on 13/07/15.
 */

public class ReadCliqueFile {

	static final Integer MIN_CLIQUE_SIZE = 3;

	public static void main(String[] args) {
		try {
			final CommandLine cmd = CommandLine
					.parser()
					.withName("wikipedia-text-parser")
					.withHeader("Analyze text dumps from Wikipedia")
					.withOption("i", "input", "input file", "FILE", CommandLine.Type.FILE_EXISTING, true, false, true)
					.withOption("p", "pages", "form-page index", "FILE", CommandLine.Type.FILE_EXISTING, true, false, true)
					.withOption("c", "categories", "page-category index", "FILE", CommandLine.Type.FILE_EXISTING, true, false, true)
					.withOption("a", "airpedia", "airpedia CSV", "FILE", CommandLine.Type.FILE_EXISTING, true, false, true)
					.withOption("o", "ontology", "DBpedia ontology", "FILE", CommandLine.Type.FILE_EXISTING, true, false, true)
					.withLogger(LoggerFactory.getLogger("eu.fbk")).parse(args);

			File input = cmd.getOptionValue("input", File.class);
			File formPage = cmd.getOptionValue("pages", File.class);
			File pageCategory = cmd.getOptionValue("categories", File.class);

			File dbpOntology = cmd.getOptionValue("ontology", File.class);
			DBpediaOntology ontology = new DBpediaOntology(dbpOntology.getAbsolutePath());

			File airpediaClasses = cmd.getOptionValue("airpedia", File.class);

			List<String> airpediaLines = Files.readAllLines(airpediaClasses.toPath());
			HashMap<String, HashSet<String>> classes = new HashMap<>();

			for (String line : airpediaLines) {
				line = line.trim();
				String[] parts = line.split("\\s+");

				if (parts.length == 0) {
					continue;
				}

				String thisClass = parts[0];
				if (classes.get(thisClass) == null) {
					classes.put(thisClass, new HashSet<>());
				}

				for (int i = 1; i < parts.length; i += 2) {
					classes.get(thisClass).add(parts[i]);
				}
			}


			FormPageSearcher formPageSearcher = new FormPageSearcher(formPage.getAbsolutePath());
			PageCategorySearcher pageCategorySearcher = new PageCategorySearcher(pageCategory.getAbsolutePath());

			List<String> lines = Files.readAllLines(input.toPath());

			for (String line : lines) {
				line = line.trim();
				if (!line.startsWith("[1]")) {
					continue;
				}

				String[] parts = line.split("\\s+");
				if (parts.length <= MIN_CLIQUE_SIZE) { // There is also [1]
					continue;
				}

				FrequencyHashSet<String> catFreq = new FrequencyHashSet();
				FrequencyHashSet<String> classFreq = new FrequencyHashSet();

				for (int i = 1; i < parts.length; i++) {
					String name = parts[i].replaceAll("_", " ");
					FreqSetSearcher.Entry[] entries = formPageSearcher.search(name);
					if (entries.length == 0) {
						continue;
					}
					String page = entries[0].getValue();
					String[] categories = pageCategorySearcher.search(page);
					if (categories.length == 0) {
						continue;
					}

					for (String category : categories) {
						catFreq.add(category);
					}

					HashSet<String> pageClasses = classes.get(page);
					if (pageClasses.contains("Agent")) {
						for (String pageClass : pageClasses) {
							try {
								int depth = ontology.getDepth(pageClass);
								classFreq.add(pageClass, depth);
							} catch (Exception e) {
								// ignored
							}
						}
					}


//					System.out.println(name);
//					System.out.println("--- " + page);
				}
				System.out.println(line);
				System.out.println(catFreq.getSorted());
				System.out.println(classFreq.getSorted());
//				System.out.println(catFreq.mostFrequent());
				System.out.println();
			}

			formPageSearcher.close();
			pageCategorySearcher.close();

		} catch (final Throwable ex) {
			CommandLine.fail(ex);
		}
	}
}
