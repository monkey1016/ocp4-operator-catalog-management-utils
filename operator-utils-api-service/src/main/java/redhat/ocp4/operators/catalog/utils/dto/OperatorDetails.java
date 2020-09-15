package redhat.ocp4.operators.catalog.utils.dto;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@Builder
@EqualsAndHashCode
@ToString
public class OperatorDetails {

	private final String operatorName;
	private final String version;
}
