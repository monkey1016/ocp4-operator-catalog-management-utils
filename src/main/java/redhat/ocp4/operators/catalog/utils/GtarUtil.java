package redhat.ocp4.operators.catalog.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;

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
}
