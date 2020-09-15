package redhat.ocp4.operators.catalog.utils.cache;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.Yaml;
import redhat.ocp4.operators.catalog.utils.GtarUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class OperatorCatalogCache {
    private static final Logger logger = LoggerFactory.getLogger(OperatorCatalogCache.class);

    @Autowired
    private HazelcastInstance hazelcastInstance;

    private static final String OPERATOR_CACHE = "operators";
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

            List<OperatorDetails> operatorDetailsList = extractOperatorsFromCatalog(in);
            IMap<String,OperatorDetails> operatorCache = hazelcastInstance.getMap(OPERATOR_CACHE);
            IMap<String, List<Map<String,String>>> imagesToOperatorsCache =
                    hazelcastInstance.getMap(IMAGE_OPERATORS_CACHE);
            logger.debug("Clearing existing caches...");
            operatorCache.clear();
            imagesToOperatorsCache.clear();
            logger.debug("Caches cleared!");
            for(OperatorDetails operator : operatorDetailsList) {
                String name = operator.getMetadata().getName();
                operatorCache.put(name, operator);
                for(String imageName : operator.getImages()) {
                    if(imagesToOperatorsCache.get(imageName) == null) {
                        List<Map<String,String>> operatorsList = new ArrayList<>();
                        operatorsList.add(getNameAndVersionMap(operator.getMetadata().getName()));
                        imagesToOperatorsCache.put(imageName, operatorsList);
                    } else {
                        imagesToOperatorsCache.get(imageName).add(getNameAndVersionMap(operator.getMetadata().getName()));
                    }
                }
            }
            logger.info("Successfully populated cache with " + operatorCache.size() + " operators!");
        } catch (MalformedURLException e) {
            logger.error("Invalid URL: " + archiveDownloadUrl);
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public List<Map<String,String>> getOperatorsForImage(String imageName) {
        IMap<String, List<Map<String,String>>> imagesToOperatorsCache = hazelcastInstance.getMap(IMAGE_OPERATORS_CACHE);
        return imagesToOperatorsCache.get(imageName);
    }

    public Map<String, List<Map<String,String>>> getOperatorsForImages(List<String> imageNames) {
        HashMap<String, List<Map<String,String>>> imagesToOperatorsMap = new HashMap<>();
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

    /**
     * Extracts details of all operators and versions found in the catalog archive and returns a list of these operators
     * @param in the InputStream representation of the Operator Catalog archive
     * @return a list of OperatorDetails extracted from the archive
     * @throws IOException an error if there are problems extracting the archive
     */
    private List<OperatorDetails> extractOperatorsFromCatalog(InputStream in) throws IOException {
        List<OperatorDetails> operatorDetailsList = new ArrayList<>();
        try(TarArchiveInputStream archiveInputStream = new TarArchiveInputStream(new GzipCompressorInputStream(in))) {
            TarArchiveEntry entry;
            while((entry = archiveInputStream.getNextTarEntry()) != null) {
                if(entry.isDirectory())
                    continue;
                if(isClusterServiceYaml(entry)) {
                    Yaml yaml = new Yaml();
                    HashMap<String, Object> details = yaml.load(archiveInputStream);
                    OperatorDetails operator = new OperatorDetails(details);
                    operatorDetailsList.add(operator);
                    logger.debug("Found operator: " + operator);
                }
            }
        }
        return operatorDetailsList;
    }


    private boolean isClusterServiceYaml(TarArchiveEntry entry) {
        return entry.isFile() &&
                (entry.getName().endsWith(".yaml") || entry.getName().endsWith(".yml")) &&
                entry.getName().contains(".clusterserviceversion.");
    }

    private Map<String, String> getNameAndVersionMap(String nameAndVersion) {
        String[] operatorNameVersion = nameAndVersion.split("\\.", 2);
        HashMap<String, String> nameAndVersionMap = new HashMap<>();
        nameAndVersionMap.put("name", operatorNameVersion[0]);
        nameAndVersionMap.put("version", operatorNameVersion[1]);
        return nameAndVersionMap;
    }
}
