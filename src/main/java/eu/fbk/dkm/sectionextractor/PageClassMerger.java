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
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.OptionBuilder;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.fbk.cit.hlt.thewikimachine.util.CommandLineWithLogger;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

public class PageClassMerger {

	static Logger logger = Logger.getLogger(PageClassMerger.class.getName());

	public static void main(String args[]) throws IOException {

		CommandLineWithLogger commandLineWithLogger = new CommandLineWithLogger();
		commandLineWithLogger.addOption(OptionBuilder.withArgName("file").hasArg().withDescription("WikiData ID file").isRequired().withLongOpt("wikidata-id").create("i"));
		commandLineWithLogger.addOption(OptionBuilder.withArgName("file").hasArg().withDescription("Airpedia Person file").isRequired().withLongOpt("airpedia").create("a"));
		commandLineWithLogger.addOption(OptionBuilder.withArgName("file").hasArg().withDescription("Output file").isRequired().withLongOpt("output").create("o"));
		CommandLine commandLine = null;
		try {
			commandLine = commandLineWithLogger.getCommandLine(args);
			PropertyConfigurator.configure(commandLineWithLogger.getLoggerProps());
		} catch (Exception e) {
			System.exit(1);
		}

		String wikiIDFileName = commandLine.getOptionValue("wikidata-id");
		String airpediaFileName = commandLine.getOptionValue("airpedia");
		String outputFileName = commandLine.getOptionValue("output");

		HashMap<Integer, String> wikiIDs = new HashMap<>();
		HashSet<Integer> airpediaClasses = new HashSet<>();

		List<String> strings;

		logger.info("Loading file " + wikiIDFileName);
		strings = Files.readLines(new File(wikiIDFileName), Charsets.UTF_8);
		for (String line : strings) {
			line = line.trim();
			if (line.length() == 0) {
				continue;
			}
			if (line.startsWith("#")) {
				continue;
			}

			String[] parts = line.split("\t");
			if (parts.length < 2) {
				continue;
			}

			int id;
			try {
				id = Integer.parseInt(parts[0]);
			} catch (Exception e) {
				continue;
			}
			wikiIDs.put(id, parts[1]);
		}

		logger.info("Loading file " + airpediaFileName);
		strings = Files.readLines(new File(airpediaFileName), Charsets.UTF_8);
		for (String line : strings) {
			line = line.trim();
			if (line.length() == 0) {
				continue;
			}
			if (line.startsWith("#")) {
				continue;
			}

			String[] parts = line.split("\t");
			if (parts.length < 2) {
				continue;
			}

			int id;
			try {
				id = Integer.parseInt(parts[0]);
			} catch (Exception e) {
				continue;
			}
			airpediaClasses.add(id);
		}

		logger.info("Saving information");
		BufferedWriter writer = new BufferedWriter(new FileWriter(outputFileName));
		for (int i : wikiIDs.keySet()) {
			if (!airpediaClasses.contains(i)) {
				continue;
			}

			writer.append(wikiIDs.get(i)).append("\n");
		}
		writer.close();
	}

}