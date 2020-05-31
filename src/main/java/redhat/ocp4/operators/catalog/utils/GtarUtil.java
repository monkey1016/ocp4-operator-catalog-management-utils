package redhat.ocp4.operators.catalog.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.io.IOUtils;
import org.yaml.snakeyaml.Yaml;

public class GtarUtil {

	public static String[] listEntriesInGtarArchive(InputStream in) throws IOException {
		List<String> results = new ArrayList<String>();
		try (TarArchiveInputStream fin = new TarArchiveInputStream(new GzipCompressorInputStream(in))) {
			TarArchiveEntry entry;
			while ((entry = fin.getNextTarEntry()) != null) {
				if (entry.isDirectory()) {
					continue;
				}
				results.add(entry.getName());
			}
			fin.close();
		}
		return results.toArray(new String[] {});
	}

	public static Optional<String> readImageTagFromString(String line) {
		if (line == null) {
			return Optional.empty();
		}
		String trimmed = line.trim();
		if (trimmed.startsWith("#") || !trimmed.contains("mage:")
				|| (trimmed.contains("mage:") && trimmed.indexOf("mage:") > trimmed.indexOf(":"))) {
			return Optional.empty();
		}
		String image = trimmed.substring(trimmed.indexOf("mage:") + 5).trim();
		String[] tokens = image.split("/");
		if (tokens.length > 1 && !tokens[0].contains(".")) {
			image = "docker.io/" + image;
		}
		return image.isEmpty() ? Optional.empty() : Optional.of(image.trim());
	}

	public static String[] imagesInGtarArchive(InputStream in) throws IOException {
		TreeSet<String> set = new TreeSet<String>();

		try (TarArchiveInputStream fin = new TarArchiveInputStream(new GzipCompressorInputStream(in))) {
			TarArchiveEntry entry;
			while ((entry = fin.getNextTarEntry()) != null) {
				if (entry.isDirectory()) {
					continue;
				}
				if (entry.isFile() && (entry.getName().endsWith(".yaml") || entry.getName().endsWith(".yml"))) {
					Yaml yaml = new Yaml();
					Map<String, Object> data = yaml.load(fin);
					set.addAll(imagesFromYamlMap(data));
//					List<String> lines = IOUtils.readLines(fin);
//					lines.forEach(l -> {
//						GtarUtil.readImageTagFromString(l).ifPresent(image -> set.add(image.replaceAll("\"", "").replaceAll("'", "")));
//					});
				}
			}
			fin.close();
		}

		return set.toArray(new String[] {});
	}

	@SuppressWarnings("unchecked")
	private static Set<String> imagesFromYamlMap(Map yamlData) {
		TreeSet<String> set = new TreeSet<String>();
		yamlData.forEach((j, k) -> {
			if (j instanceof String && ("" + j).equals("image") && k instanceof String) {
				String image = ((String) k).trim();
				if (!image.isEmpty()) {
					if (!image.contains("/")) {
						image = "docker.io/library/" + image;
					} else {
						String[] tokens = image.split("/");
						if (tokens.length > 1 && !tokens[0].contains(".")) {
							image = "docker.io/" + image;
						}
					}
					set.add(image);
				}
			}
			if (k instanceof Map) {
				set.addAll(imagesFromYamlMap((Map) k));
			}
			if (k instanceof List) {
				List<?> l = (List<?>) k;
				l.forEach(item -> {
					if (item instanceof Map) {
						set.addAll(imagesFromYamlMap((Map) item));
					}
				});
			}
		});
		return set;
	}
}
