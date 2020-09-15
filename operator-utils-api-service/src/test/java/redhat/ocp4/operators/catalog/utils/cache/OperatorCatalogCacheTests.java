package redhat.ocp4.operators.catalog.utils.cache;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.hazelcast.core.HazelcastInstance;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.Resource;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.junit4.SpringRunner;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SpringBootTest
@TestPropertySource(properties = {
        "operator-catalog.archive.url=http://localhost:8090/operatorhub-manifests-5-31-20.tar.gz"
})
public class OperatorCatalogCacheTests {
    private WireMockServer wireMockServer = new WireMockServer(options().port(8090));;
    private static List<Map<String, String>> testOperatorList = new ArrayList<>();

    @Autowired
    OperatorCatalogCache cache;

    public static final String TEST_IMAGE = "registry.access.redhat.com/amqstreams-1/amqstreams10-clusteroperator-openshift:1.0.0";

    @BeforeAll
    public static void init() {
        Map<String, String> testOperatorEntry = new HashMap<>();
        testOperatorEntry.put("name", "amqstreams");
        testOperatorEntry.put("version", "v1.0.0");
        testOperatorList.add(testOperatorEntry);
    }

    @Test
    void testUrlPopulateCache() throws IOException {
        wireMockServer.start();
        cache.setOperatorHubFilePath("/some/path/that/does/not/exist.tar.gz");
        cache.populateCache();
        wireMockServer.stop();

        assertManifestMatches();
    }

    private void assertManifestMatches() {
        // 1358 is the total number of images in the mapping
        assertEquals(cache.getAllImagesToOperatorMappings().size(), 1358);
        assertArrayEquals(cache.getOperatorsForImage(TEST_IMAGE).toArray(), testOperatorList.toArray());
    }

    @Test
    void testFilePopulateCache() throws IOException, URISyntaxException {
        URL resource = getClass().getClassLoader().getResource("operatorhub-manifests-5-31-20.tar.gz");
        cache.setOperatorHubFilePath(Paths.get(resource.toURI()).toString());
        cache.setArchiveDownloadUrl("http://bogus.url:9090");
        cache.populateCache();
        assertManifestMatches();
    }
}
