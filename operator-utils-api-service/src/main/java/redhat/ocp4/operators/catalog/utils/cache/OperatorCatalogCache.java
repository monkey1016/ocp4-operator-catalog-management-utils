package redhat.ocp4.operators.catalog.utils.cache;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import redhat.ocp4.operators.catalog.utils.GtarUtil;
import redhat.ocp4.operators.catalog.utils.dto.OperatorDetails;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class OperatorCatalogCache {
    private static final Logger logger = LoggerFactory.getLogger(OperatorCatalogCache.class);

    @Autowired
    private HazelcastInstance hazelcastInstance;

    private static final String IMAGE_OPERATORS_CACHE = "images-to-operators";

    @Value("${operator-catalog.archive.url}")
    private String archiveDownloadUrl;

    @Value("${operatorhub.tar.gz.file:/opt/operatorhub.tar.gz}")
    private String operatorHubFilePath;


    /**
     * After creation of the bean, and every five minutes, refresh the Operator Catalog cache. If the file exists
     * locally, use that file as the source of truth, otherwise download it from Artifactory using the provided URL.
     */
    @Scheduled(fixedRateString = "${operator-catalog.cache-refresh-ms}")
    public void populateCache() {
        logger.info("Populating operator caches...");
        try {
            File operatorHubFile = new File(operatorHubFilePath);
            InputStream in;
            if (operatorHubFile.exists()) {
                logger.debug("Found " + operatorHubFilePath + ", using for local cache.");
                in = new FileInputStream(operatorHubFile);
            } else {
                logger.debug("Did not find " + operatorHubFilePath + ". Attempting download from " + archiveDownloadUrl);
                in = new URL(archiveDownloadUrl).openStream();
            }

            Map<String, List<OperatorDetails>> operatorDetailsList = GtarUtil.imageOperatorInfo(in);
            IMap<String, List<OperatorDetails>> operatorCache = hazelcastInstance.getMap(IMAGE_OPERATORS_CACHE);
            logger.debug("Clearing existing caches...");
            operatorCache.clear();
            logger.debug("Caches cleared!");
            operatorCache.putAll(operatorDetailsList);
            logger.info("Successfully populated cache with " + operatorCache.size() + " operators!");
        } catch (MalformedURLException e) {
            logger.error("Invalid URL: " + archiveDownloadUrl);
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public List<OperatorDetails> getOperatorsForImage(String imageName) {
        IMap<String, List<OperatorDetails>> imagesToOperatorsCache = hazelcastInstance.getMap(IMAGE_OPERATORS_CACHE);
        return imagesToOperatorsCache.get(imageName);
    }

    public Map<String, List<OperatorDetails>> getOperatorsForImages(List<String> imageNames) {
        HashMap<String, List<OperatorDetails>> imagesToOperatorsMap = new HashMap<>();
        for(String imageName : imageNames) {
            imagesToOperatorsMap.put(imageName, getOperatorsForImage(imageName));
        }
        return imagesToOperatorsMap;
    }

    public Map<String, List<Map<String,String>>> getAllImagesToOperatorMappings() {
        return hazelcastInstance.getMap(IMAGE_OPERATORS_CACHE);
    }

    public String getOperatorHubFilePath() {
        return operatorHubFilePath;
    }

    public void setOperatorHubFilePath(String operatorHubFilePath) {
        this.operatorHubFilePath = operatorHubFilePath;
    }

    public String getArchiveDownloadUrl() {
        return archiveDownloadUrl;
    }

    public void setArchiveDownloadUrl(String archiveDownloadUrl) {
        this.archiveDownloadUrl = archiveDownloadUrl;
    }
}
