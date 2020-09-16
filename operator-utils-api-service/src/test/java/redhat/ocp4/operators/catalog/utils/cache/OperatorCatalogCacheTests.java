package redhat.ocp4.operators.catalog.utils.cache;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import redhat.ocp4.operators.catalog.utils.dto.OperatorDetails;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@TestPropertySource(properties = {
        "operatorhub.tar.gz.file=classpath:operatorhub-manifests-5-31-20.tar.gz"
})
public class OperatorCatalogCacheTests {
    private WireMockServer wireMockServer = new WireMockServer(options().port(8090));
    private static List<OperatorDetails> testOperatorList = new ArrayList<>();

    @Autowired
    OperatorCatalogCache cache;

    public static final String TEST_IMAGE = "registry.access.redhat.com/amqstreams-1/amqstreams10-clusteroperator-openshift:1.0.0";

    @BeforeAll
    public static void init() {
        testOperatorList.add(OperatorDetails.builder().operatorName("amq-streams").version("v1.0.0").build());
    }

    private void assertManifestMatches() {
        // 1358 is the total number of images in the mapping
        assertEquals(cache.getAllImagesToOperatorMappings().size(), 1364);
        assertArrayEquals(cache.getOperatorsForImage(TEST_IMAGE).toArray(), testOperatorList.toArray());
    }

    @Test
    void testFilePopulateCache() throws URISyntaxException {
        cache.populateCacheIfNecessary();
        assertManifestMatches();
    }
}
