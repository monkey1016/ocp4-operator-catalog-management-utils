package redhat.ocp4.operators.catalog.utils.cache;

import java.util.List;

public interface OperatorRepository {

    List<OperatorDetails> getOperatorsByImageName(String imageName);
    List<String> getImagesByOperator(String operatorNameAndVersion);
}
