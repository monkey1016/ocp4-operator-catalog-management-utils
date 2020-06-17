package redhat.ocp4.operators.catalog.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.io.IOUtils;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.scanner.ScannerException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Static methods that perform the needed API processing on tar.gz archive files
 * @author lshulman
 *
 */
public class GtarUtil {

	/**
	 * List all files/directories the argument tar/gz input stream
	 * @param in a tar/gz input stream
	 * @return
	 * @throws IOException
	 */
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
		return results.stream().distinct().sorted().collect(Collectors.toList()).toArray(new String[] {});
	}


	/**
	 * return a unique list of container images in the argument package manifest tar/gz input stream
	 * @param in
	 * @return
	 * @throws IOException
	 */
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
			if (j instanceof String
					&& Arrays.asList("image", "containerImage", "baseImage").stream().anyMatch(s -> s.equals(("" + j).trim()))
					&& k instanceof String) {
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
					if (image.startsWith("index.docker.io")) {
						image = image.replaceAll("index.docker.io", "docker.io");
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

	/**
	 * produces output like that produced by a 'oc adm catalog mirror' command on the package manifest tar/gz archive.  See https://docs.openshift.com/container-platform/4.3/operators/olm-restricted-networks.html#olm-restricted-networks-operatorhub_olm-restricted-networks"
	 * 
	 * @param inputStream a tar/gz package manifest archive
	 * @param mirror the mirror registry to apply to all image references in the Yaml
	 * @return
	 * @throws IOException
	 */
	public static String createMirrorMappings(InputStream inputStream, String mirror) throws IOException {
		String[] images = GtarUtil.imagesInGtarArchive(inputStream);
		List<String> mappings = Arrays.asList(images).stream().map(s -> {
			String mirrorImage = s.substring(s.indexOf("/"));
			mirrorImage = mirrorImage.contains("@sha256") ? mirrorImage.substring(0, mirrorImage.indexOf("@sha256"))
					: mirrorImage;
			return s + "=" + mirror + mirrorImage;
		}).collect(Collectors.toList());
		StringBuffer strBuf = new StringBuffer();
		mappings.stream().distinct().sorted().forEach(mapping -> strBuf.append(mapping + System.lineSeparator()));
		return strBuf.toString();
	}

	/**
	 * creates an ImageContentSourcePolicy resource in Yaml from a tar.gz catalog manifests file. The result is exactly as produced by a 'oc adm catalog mirror' command. See https://docs.openshift.com/container-platform/4.3/operators/olm-restricted-networks.html#olm-restricted-networks-operatorhub_olm-restricted-networks"
	 * 
	 * @param inputStream the package manifest tar/gz file
	 * @param mirror the mirror registry to apply to all image references in the Yaml
	 * @param name will be set as the name of the ImageContentSourcePolicy resource
	 * @return
	 * @throws IOException
	 */
	public static String createImageContentSourcePolicy(InputStream inputStream, String mirror, String name)
			throws IOException {
		List<String> imagesWithoutVersionOrHash = Arrays.asList(GtarUtil.imagesInGtarArchive(inputStream)).stream()
				.map(s -> {
					String result = s;
					if (result.contains(":")) {
						result = result.substring(0, result.indexOf(":"));
					}
					if (result.contains("@")) {
						result = result.substring(0, result.indexOf("@"));
					}
					return result;
				}).distinct().sorted().collect(Collectors.toList());

		ImageContentSourcePolicyYaml yaml = new ImageContentSourcePolicyYaml();
		yaml.getMetadata().put("name", name);
		List<ImageContentSourcePolicyYaml.Mirror> mirrors = new ArrayList<ImageContentSourcePolicyYaml.Mirror>();

		imagesWithoutVersionOrHash.forEach(image -> {
			ImageContentSourcePolicyYaml.Mirror mirrorYaml = new ImageContentSourcePolicyYaml.Mirror();
			mirrorYaml.setSource(image);
			mirrorYaml.setMirrors(new String[] { mirror + image.substring(image.indexOf("/")) });
			mirrors.add(mirrorYaml);
		});
		yaml.getSpec().setRepositoryDigestMirrors(mirrors.toArray(new ImageContentSourcePolicyYaml.Mirror[] {}));
		return new Yaml().dump(yaml);
	}


	/**
	 * 
	 * @param mirrors
	 * @param input
	 * @return
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static String applyImageMirrors(Map<String, String> mirrors, String input){
		Yaml yaml = new Yaml();
		Object yamlObj = yaml.load(input.replaceAll("\t", "  "));

		if (yamlObj instanceof Map) {
			applyMirrorSubstitutionsToYamlMap((Map) yamlObj, mirrors);

		}
		if (yamlObj instanceof List) {
			List data = (List) yamlObj;
			data.forEach(yamlMap -> {
				if (yamlMap instanceof Map) {
					applyMirrorSubstitutionsToYamlMap((Map) yamlMap, mirrors);
				}
			});
		}
		return yaml.dump(yamlObj);
	}

	public static String convertYamlToJson(String yamlStr) throws JsonProcessingException {
		Yaml yaml = new Yaml();
		Object object = yaml.load(yamlStr);
		return new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(object);
	}

	public static void applyImageMirrors(Map<String, String> mirrors, TarArchiveInputStream fin,
			TarArchiveOutputStream output) throws IOException {
		output.setLongFileMode(TarArchiveOutputStream.LONGFILE_GNU);
		output.setAddPaxHeadersForNonAsciiNames(true);
		TarArchiveEntry entry;
		while ((entry = fin.getNextTarEntry()) != null) {

			if (entry.isFile() && (entry.getName().endsWith(".yaml") || entry.getName().endsWith(".yml"))) {
				Yaml yaml = new Yaml();
				Map<String, Object> data = yaml.load(fin);
				applyMirrorSubstitutionsToYamlMap(data, mirrors);
				byte[] yamlString = yaml.dump(data).getBytes();
				entry.setSize(yamlString.length);
				output.putArchiveEntry(entry);
				IOUtils.write(yamlString, output);
				output.closeArchiveEntry();
			} else {
				output.putArchiveEntry(entry);
				IOUtils.copy(fin, output);
				output.closeArchiveEntry();
			}

		}
		fin.close();
		output.finish();
		output.close();

	}

	public static List<String> registriesinGtarArchive(InputStream inputStream) throws IOException {
		return Arrays.asList(GtarUtil.imagesInGtarArchive(inputStream)).stream()
				.map(s -> s.substring(0, s.indexOf("/"))).distinct().sorted().collect(Collectors.toList());
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static void applyMirrorSubstitutionsToYamlMap(Map yamlData, Map<String, String> mirrors) {
		yamlData.forEach((j, k) -> {
			if (j instanceof String
					&& Arrays.asList("image", "containerImage", "baseImage").stream().anyMatch(s -> s.equals(("" + j).trim()))
					&& k instanceof String) {
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
					if (image.startsWith("index.docker.io")) {
						image = image.replaceAll("index.docker.io", "docker.io");
					}
					String imageRepo = image.split("/")[0];
					if (mirrors.containsKey(imageRepo)) {
						image = mirrors.get(imageRepo) + image.substring(image.indexOf("/"));
					}
					yamlData.put(j, image);
				}
			}

			if (j instanceof String && Arrays.asList("alm-examples").stream().anyMatch(s -> s.equals(("" + j).trim()))
					&& k instanceof String) {
				try {
					yamlData.put(j, convertYamlToJson(applyImageMirrors(mirrors, (String) k)));
				} catch (JsonProcessingException | ScannerException e) {
					Logger.getLogger(GtarUtil.class.getName()).warning(
							"could not convert alm-examples Yaml to Json. did not apply mirrors to alm-examples. Error: " + e.getMessage());
				}
			}
			
			if(k instanceof String && mirrors.keySet().stream().anyMatch(s -> ("" + k).startsWith(s))) {
				String match = mirrors.keySet().stream().filter(s -> ("" + k).startsWith(s)).findFirst().get();
				yamlData.put(j, ("" + k).replace(match, mirrors.get(match)));
			}
			
			if (k instanceof Map) {
				applyMirrorSubstitutionsToYamlMap((Map) k, mirrors);
			}
			if (k instanceof List) {
				List<?> l = (List<?>) k;
				l.forEach(item -> {
					if (item instanceof Map) {
						applyMirrorSubstitutionsToYamlMap((Map) item, mirrors);
					}
				});
			}
			if (Arrays.asList("command", "args", "value").stream().anyMatch(s -> s.equals(("" + j).trim()))) {
				yamlData.put(j, applyMirrorsToContainerCommand(k, mirrors));
			}
		});
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static Object applyMirrorsToContainerCommand(Object commandData, Map<String, String> mirrors) {
		if (commandData instanceof String && commandData != null) {
			String s = (String) commandData;
			for (String registry : mirrors.keySet()) {
				s = s.replaceAll(registry, mirrors.get(registry));
			}
			return s;
		}

		if (commandData instanceof List && commandData != null) {
			List commands = (List) commandData;
			for (int i = 0; i < commands.size(); i++) {
				if (commands.get(i) instanceof String) {
					for (String reg : mirrors.keySet()) {
						String s = (String) commands.get(i);
						commands.set(i, s.replaceAll(reg, mirrors.get(reg)));
					}
				}
			}
			return commands;
		}
		return commandData;
	}
}
