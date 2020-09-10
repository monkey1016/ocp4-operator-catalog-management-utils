package redhat.ocp4.operators.catalog.utils.cache;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import static redhat.ocp4.operators.catalog.utils.GtarUtil.imagesFromYamlMap;

public class OperatorDetails implements Serializable {
    private String description;
    private String displayName;
    private String version;
    private OperatorMetadata metadata;
    private List<String> images;

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public OperatorMetadata getMetadata() {
        return metadata;
    }

    public void setMetadata(OperatorMetadata metadata) {
        this.metadata = metadata;
    }

    public List<String> getImages() {
        return images;
    }

    public void setImages(List<String> images) {
        this.images = images;
    }


    @Override
    public String toString() {
        return "Operator{" + metadata.getName() + "}";
    }

    @SuppressWarnings("unchecked")
    public OperatorDetails(HashMap<String, Object> yamlData) {
        HashMap<String, Object> spec = (HashMap<String, Object>)yamlData.get("spec");
        this.metadata = new OperatorMetadata((HashMap<String, Object>)yamlData.get("metadata"));
        this.description = (String)spec.get("description");
        this.displayName = (String)spec.get("displayName");
        this.version = (String)spec.get("version");
        this.images = new ArrayList<>(imagesFromYamlMap(yamlData));
    }

    public OperatorDetails(String displayName, String description, String version, OperatorMetadata metadata) {
        this.displayName = displayName;
        this.description = description;
        this.version = version;
        this.metadata = metadata;
    }
}
