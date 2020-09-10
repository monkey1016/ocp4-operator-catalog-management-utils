package redhat.ocp4.operators.catalog.utils.cache;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.hazelcast.core.HazelcastInstance;
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

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

@SpringBootTest
@TestPropertySource(properties = {
        "operator-catalog.archive.url=http://localhost:8090/operatorhub-manifests-5-31-20.tar.gz"
})
public class OperatorCatalogCacheTests {
    private WireMockServer wireMockServer;

    @Autowired
    OperatorCatalogCache cache;

    @BeforeEach
    void setUp() {
        wireMockServer = new WireMockServer(options().port(8090));
        wireMockServer.start();
    }

    @AfterEach
    void tearDown() {
        wireMockServer.stop();
    }

    @Test
    void testPopulateCache() throws IOException {
        cache.populateCache();
        // 1358 is the total number of images in the mapping
        assertEquals(cache.getAllImagesToOperatorMappings().size(), 1358);
    }
}
