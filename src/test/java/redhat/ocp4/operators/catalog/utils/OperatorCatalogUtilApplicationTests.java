package redhat.ocp4.operators.catalog.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;

import org.assertj.core.util.Arrays;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.Resource;

@SpringBootTest
class OperatorCatalogUtilApplicationTests {

	@Value("classpath:operatorhub-5-30-20.tar.gz")
	Resource operatorHubArchive;
	
	@Test
	void contextLoads() {
		assertTrue(operatorHubArchive.exists());
		try {
			String[] archiveEntries = GtarUtil.listEntriesInGtarArchive(operatorHubArchive.getInputStream());
			assertEquals(2948, archiveEntries.length);
		} catch (IOException e) {
			fail(e.getMessage());
		}
	}

}
