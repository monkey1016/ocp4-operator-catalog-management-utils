package redhat.ocp4.operators.catalog.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.TreeSet;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.io.IOUtils;

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
		}
		return results.toArray(new String[] {});
	}

	public static Optional<String> readImageTagFromString(String line) {
		if (line == null) {
			return Optional.empty();
		}
		String trimmed = line.trim();
		if ( trimmed.startsWith("#") || !trimmed.contains(":") || trimmed.indexOf("mage:") > trimmed.indexOf(":")) {
			return Optional.empty();
		}
		String image = trimmed.substring(trimmed.indexOf("mage:") + 5);
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
				if (entry.isFile() && (entry.getFile().getName().endsWith(".yaml")
						|| entry.getFile().getName().endsWith(".yml"))) {
					File file = entry.getFile();
					List<String> lines = IOUtils.readLines(new FileInputStream(file));
				}
			}
		}

		return set.toArray(new String[] {});
	}
}
