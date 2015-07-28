package eu.fbk.dkm.sectionextractor.itapos;

import javax.annotation.Nullable;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;

/**
 * Created by alessio on 20/07/15.
 */

public class FstanRunner {

	private String command = "/Users/alessio/Documents/scripts/textpro/modules/MorphoPro/bin/fstan/x86_64/fstan /Users/alessio/Documents/scripts/textpro/modules/MorphoPro/models/italian-utf8.fsa";

	public FstanRunner(String command) {
		this.command = command;
	}

	public ArrayList<String[]> run(ArrayList<String> requests) throws IOException {

		ArrayList<String[]> ret = new ArrayList<>();

		Process process = Runtime.getRuntime().exec(command);
		OutputStream out = process.getOutputStream();

		Iterator<String> it = requests.iterator();

		while (it.hasNext()) {
			out.write((it.next() + "\n").getBytes());
		}

		out.close();

		BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), "UTF-8"));
		String line;
		while ((line = reader.readLine()) != null) {
			String[] parts = line.trim().split("[ /]");
			ret.add(parts);
		}
		reader.close();

		return ret;
	}

	public HashSet<String> getTypes(String word) {
		ArrayList<String> requests = new ArrayList<>();
		requests.add(word);

		ArrayList<String[]> strings = new ArrayList<>();
		try {
			strings = run(requests);
		} catch (IOException e) {
			e.printStackTrace();
		}

		HashSet<String> types = new HashSet<>();
		for (String[] parts : strings) {
			for (String part : parts) {
				String[] subparts = part.trim().split("\\+");
				if (subparts.length > 1) {
					types.add(subparts[1]);
				}
			}

		}

		return types;
	}

	public HashSet<String[]> getParts(String word) {
		ArrayList<String> requests = new ArrayList<>();
		requests.add(word);

		ArrayList<String[]> strings = new ArrayList<>();
		try {
			strings = run(requests);
		} catch (IOException e) {
			e.printStackTrace();
		}

		HashSet<String[]> types = new HashSet<>();
		for (String[] parts : strings) {
			for (String part : parts) {
				String[] subparts = part.trim().split("\\+");
				if (subparts.length > 1) {
					types.add(subparts);
				}
			}

		}

		return types;
	}

	public ArrayList<String> get(String word, @Nullable String type) {
		//String command = "/home/aprosio/TANA/textpro1.5/MorphoPro/bin/fstan /home/aprosio/TANA/textpro1.5/MorphoPro/models/italian-utf8.fsa";
		// String word = "andare";
		// String type = "v";

		ArrayList<String> requests = new ArrayList<>();
		requests.add(word);

		HashSet<String> words = new HashSet<>();

		if (type == null) {
			type = "";
		}
		else {
			if (type.equals("a")) {
				type = "adj";
			}
			if (type.equals("r")) {
				type = "adv";
			}
			if (type.equals("NOUN")) {
				type = "n";
			}
			if (type.equals("VERB")) {
				type = "v";
			}
			if (type.equals("ADJECTIVE")) {
				type = "adj";
			}
			if (type.equals("ADVERB")) {
				type = "adv";
			}
			if (type.equals("PREPOSITION")) {
				type = "prep";
			}
			if (type.equals("DETERMINER")) {
				type = "art";
			}
			if (type.equals("PRONOUN")) {
				type = "pron";
			}
			if (type.equals("PUNCTUATION")) {
				type = "punc";
			}
			if (type.equals("OTHER")) {
				type = "";
			}
		}

		try {
			ArrayList<String[]> ret = run(requests);

			for (String[] parts : ret) {
				for (int i = 1; i < parts.length; i++) {
					if (parts[i].length() > 0) {
						String[] subparts = parts[i].trim().split("\\+");
						if (type.length() > 0) {
							try {
								if (subparts[1].equals(type)) {
									String[] subsubparts = subparts[0].trim().split("~");
									words.add(subsubparts[subsubparts.length - 1]);
								}
							} catch (Exception e) {
								words.add(subparts[0]);
								// System.out.println(Arrays.toString(subparts));
							}
						}
						else {
							for (@SuppressWarnings("unused") String part : subparts) {
								String[] subsubparts = subparts[0].trim().split("~"); //TODO: shouldn't this be part instead of subparts?
								words.add(subsubparts[subsubparts.length - 1]);
							}
						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace(System.out);
		}

		ArrayList<String> ret = new ArrayList<String>();
		ret.addAll(words);
		if (ret.size() < 1) {
			ret.add(word);
		}
		return ret;
	}

	public static void main(String[] args) {
		String command = "/Users/alessio/Documents/scripts/textpro/modules/MorphoPro/bin/fstan/x86_64/fstan /Users/alessio/Documents/scripts/textpro/modules/MorphoPro/models/italian-utf8.fsa";
		FstanRunner runner = new FstanRunner(command);
//		ArrayList<String> strings = runner.get("spariamo", "v");
//		System.out.println(runner.getTypes("cacca"));
	}

}
