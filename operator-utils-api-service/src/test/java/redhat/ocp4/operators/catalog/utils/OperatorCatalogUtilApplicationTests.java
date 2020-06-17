package redhat.ocp4.operators.catalog.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.apache.commons.io.IOUtils;
import org.assertj.core.util.Arrays;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.Resource;
import org.yaml.snakeyaml.Yaml;

import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
class OperatorCatalogUtilApplicationTests {

	@Value("classpath:operatorhub-manifests-5-31-20.tar.gz")
	Resource operatorHubArchive;

	@Value("classpath:operatorhub-mapping-5-31-20.txt")
	Resource operatorHubMappingFile;

	@Value("classpath:operatorHubImageContentSourcePoicy.yaml")
	Resource operatorHubICSPYaml;

	@Test
	void testListEntriesInGtarArchive() {
		assertTrue(operatorHubArchive.exists());
		try {
			String[] archiveEntries = GtarUtil.listEntriesInGtarArchive(operatorHubArchive.getInputStream());
			assertEquals(2958, archiveEntries.length);
		} catch (IOException e) {
			fail(e.getMessage());
		}
	}

	@Test
	void testOcImageMirrorInput() {
		try {
			String mappings = GtarUtil.createMirrorMappings(operatorHubArchive.getInputStream(), "test.io");
			StringBuffer buf = new StringBuffer();
			IOUtils.readLines(operatorHubMappingFile.getInputStream()).stream().distinct().sorted()
					.forEach(s -> buf.append(s).append(System.lineSeparator()));
			// the output of the api method should not equal what "oc adm catalog mirror"
			// produces, because "oc adm catalog mirror"
			// actually misses some images that need to be mirrors. We test here to ensure
			// that the api at least
			// includes all of the images that "oc adm catalog mirror" produces
			assertNotEquals(buf.toString(), mappings);
			TreeSet<String> ocAdmCatalogMirrorLines = new TreeSet<String>(
					IOUtils.readLines(new StringReader(buf.toString())));
			TreeSet<String> apiMirrorLines = new TreeSet<String>(IOUtils.readLines(new StringReader(mappings)));
			assertTrue(apiMirrorLines.size() > ocAdmCatalogMirrorLines.size());
			// ensure that the api result contains everything that "oc adm catalog mirror"
			// produces
			assertTrue(apiMirrorLines.containsAll(ocAdmCatalogMirrorLines));
		} catch (IOException e) {
			fail(e.getMessage());
		}
	}

	@Test
	void testApplyImageMirrors() {
		try {
			//get list of image registries in our input tar.gz. 
			List<String> imageRegistries = GtarUtil.registriesinGtarArchive(operatorHubArchive.getInputStream());
			assertEquals(new TreeSet<Object>(imageRegistries),
					new TreeSet<Object>(Arrays.asList(new String[] { "docker.io", "gcr.io", "k8s.gcr.io",
							"lightbend-docker-registry.bintray.io", "quay.io", "registry.access.redhat.com",
							"registry.connect.redhat.com", "registry.redhat.io", "registry.svc.ci.openshift.org" })));
			//we'll create a simple mirror map, which just prepends "mirror." for each registry, so that "docker.io" -> "mirror.docker.io"
			Map<String,String> mirrors = imageRegistries.stream().collect(Collectors.toMap(s -> s, s -> "mirror." + s));
			
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			GtarUtil.applyImageMirrors(mirrors,
					new TarArchiveInputStream(new GzipCompressorInputStream(operatorHubArchive.getInputStream())),
					new TarArchiveOutputStream(new GzipCompressorOutputStream(baos)));
			baos.flush();
			baos.close();
			//get list of registries from the result tar.gz. We expect them all to start with "mirror."
			List<String> mirroredRegistries = GtarUtil.registriesinGtarArchive(new ByteArrayInputStream(baos.toByteArray()));
			assertEquals(imageRegistries.size(), mirroredRegistries.size());
			assertEquals(mirroredRegistries, imageRegistries.stream().map(s -> "mirror." + s).collect(Collectors.toList()));
		} catch (IOException e) {
			fail(e.getMessage());
		}
	}

}
