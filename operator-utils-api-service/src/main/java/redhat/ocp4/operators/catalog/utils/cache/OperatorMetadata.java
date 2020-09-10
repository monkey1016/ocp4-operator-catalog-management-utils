package redhat.ocp4.operators.catalog.utils.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.HashMap;

public class OperatorMetadata implements Serializable {
    private static final Logger logger = LoggerFactory.getLogger(OperatorMetadata.class);

    private String categories;
    private String description;
    private boolean certified;
    private String repository;
    private String name;
    private String namespace;

    public String getCategories() {
        return categories;
    }

    public void setCategories(String categories) {
        this.categories = categories;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isCertified() {
        return certified;
    }

    public void setCertified(boolean certified) {
        this.certified = certified;
    }

    public String getRepository() {
        return repository;
    }

    public void setRepository(String repository) {
        this.repository = repository;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    @SuppressWarnings("unchecked")
    public OperatorMetadata(HashMap<String, Object> metadata) {
        HashMap<String, String> annotations = (HashMap<String, String>)metadata.get("annotations");
        this.name = (String)metadata.get("name");
        this.namespace = (String)metadata.get("namespace");
        this.categories = annotations.get("categories");
        this.description = annotations.get("description");
        this.repository = annotations.get("repository");
        this.certified = Boolean.parseBoolean(annotations.get("certified"));
    }

    public OperatorMetadata(
            String name,
            String description,
            String categories,
            String namespace,
            String repository) {
        this.name = name;
        this.description = description;
        this.categories = categories;
        this.namespace = namespace;
        this.repository = repository;
    }
}
