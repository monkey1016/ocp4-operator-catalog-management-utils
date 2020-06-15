package redhat.ocp4.operators.catalog.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.assertj.core.util.Arrays;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.Resource;
import org.yaml.snakeyaml.Yaml;

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
	void testReadImageTagFromString() {
		String test = " image: quay.io/path/myimage:v4.0.0  ";
		assertEquals("quay.io/path/myimage:v4.0.0", GtarUtil.readImageTagFromString(test).get());
		
		test = " ### ";
		assertFalse(GtarUtil.readImageTagFromString(test).isPresent());
		
		test = " # image: quay.io/path/myimage:v4.0.0  ";
		assertFalse(GtarUtil.readImageTagFromString(test).isPresent());
		
		test = " dockerImage: quay.io/path/myimage:v4.0.0  ";
		assertEquals("quay.io/path/myimage:v4.0.0", GtarUtil.readImageTagFromString(test).get());

		test = " imageNot: quay.io/path/myimage:v4.0.0 ";
		assertFalse(GtarUtil.readImageTagFromString(test).isPresent());

	}
	
	@Test
	void testOcImageMirrorInput() {
		try {
			String mappings = GtarUtil.createMirrorMappings(operatorHubArchive.getInputStream(), "test.io");
			StringBuffer buf = new StringBuffer();
			IOUtils.readLines(operatorHubMappingFile.getInputStream()).stream().distinct().sorted().forEach(s -> buf.append(s).append(System.lineSeparator()));
			//the output of the api method should not equal what "oc adm catalog mirror" produces, because "oc adm catalog mirror"
			//actually misses some images that need to be mirrors. We test here to ensure that the api at least
			//includes all of the images that "oc adm catalog mirror" produces
			assertNotEquals(buf.toString(), mappings);
			TreeSet<String> ocAdmCatalogMirrorLines = new TreeSet<String>(IOUtils.readLines(new StringReader(buf.toString())));
			TreeSet<String> apiMirrorLines = new TreeSet<String>(IOUtils.readLines(new StringReader(mappings)));
			assertTrue(apiMirrorLines.size() > ocAdmCatalogMirrorLines.size());
			//ensure that the api result contains everything that "oc adm catalog mirror" produces
			assertTrue(apiMirrorLines.containsAll(ocAdmCatalogMirrorLines));
		} catch (IOException e) {
			fail(e.getMessage());
		}
	}
	
	@Test
	void yamlTest() throws IOException {
		Yaml yaml = new Yaml();
		Object obj = yaml.load(new FileInputStream(new File("/home/lshulman/dev/ocp-operator-catalog-pipeline/tmp/manifests/couchbase-enterprise-certified/couchbase-enterprise-certified-qj3qfmee/2.0.0/couchbase-v2.0.0.clusterserviceversion.yaml")));
		assertTrue(obj instanceof Map);
		String out = GtarUtil.applyImageMirrors(new HashMap<String,String>(), IOUtils.toString(new FileInputStream(new File("/home/lshulman/dev/ocp-operator-catalog-pipeline/tmp/manifests/couchbase-enterprise-certified/couchbase-enterprise-certified-qj3qfmee/2.0.0/couchbase-v2.0.0.clusterserviceversion.yaml"))));
		IOUtils.write(out, new FileWriter(new File("/home/lshulman/dev/ocp-operator-catalog-pipeline/tmp/manifests/couchbase-enterprise-certified/couchbase-enterprise-certified-qj3qfmee/2.0.0/couchbase-v2.0.0.clusterserviceversiontest.yaml")));
	}
	


}
