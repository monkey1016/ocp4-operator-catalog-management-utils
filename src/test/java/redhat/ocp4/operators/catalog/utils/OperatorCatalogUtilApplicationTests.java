package redhat.ocp4.operators.catalog.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.assertj.core.util.Arrays;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.Resource;

@SpringBootTest
class OperatorCatalogUtilApplicationTests {

	@Value("classpath:operatorhub-manifests-5-31-20.tar.gz")
	Resource operatorHubArchive;
	
	@Value("classpath:operatorhub-mapping-5-31-20.txt")
	Resource operatorMappingFile;
	
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
	void testImagesInGtarArchive() {
		try {
			String mappings = GtarUtil.createMirrorMappings(operatorHubArchive.getInputStream(), "test.io");
			StringBuffer buf = new StringBuffer();
			IOUtils.readLines(operatorMappingFile.getInputStream()).stream().distinct().sorted().forEach(s -> buf.append(s).append(System.lineSeparator()));
			assertEquals(buf.toString(), mappings);
		} catch (IOException e) {
			fail(e.getMessage());
		}
	}
	
	

}
