package eu.fbk.dkm.sectionextractor;

import java.util.HashSet;

/**
 * Created by alessio on 17/07/15.
 */
public class FeatureSet {
	public HashSet<String> features = new HashSet<>();
	public String value = null;

	public FeatureSet() {

	}

	public void addFeature(String value) {
		features.add(value);
	}

	@Override
	public String toString() {
		StringBuffer buffer = new StringBuffer();
		buffer.append(value);
		for (String feature : features) {
			buffer.append("\t").append(feature);
		}

		return buffer.toString();
	}
}
