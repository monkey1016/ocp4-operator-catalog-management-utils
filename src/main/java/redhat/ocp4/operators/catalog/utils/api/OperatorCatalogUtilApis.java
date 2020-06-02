package redhat.ocp4.operators.catalog.utils.api;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import redhat.ocp4.operators.catalog.utils.GtarUtil;

@RestController
public class OperatorCatalogUtilApis {


	@ApiOperation(value = "lists all of the images referenced in a tar.gz catalog manifests file")
	@PostMapping("/listAllImagesInCatalogManifests")
	public String[] listAllImages(@RequestParam("file") MultipartFile file) throws IOException {
		return GtarUtil.imagesInGtarArchive(file.getInputStream());
	}
	
	@ApiOperation(value = "returns a mapping.txt file from a tar.gz catalog  manifests file. The result is exactly as produced by a 'oc adm catalog mirror' command. See https://docs.openshift.com/container-platform/4.3/operators/olm-restricted-networks.html#olm-restricted-networks-operatorhub_olm-restricted-networks")
	@PostMapping("/mirrorImagesInCatalogManifests")
	public String mirrorImages(@RequestParam("file") MultipartFile file,  @RequestParam("mirrorUrl") String mirror ) throws IOException {
		return GtarUtil.createMirrorMappings(file.getInputStream(), mirror);
	}
	
	@ApiOperation(value = "creates an ImageContentSourcePolicy resource in Yaml from a tar.gz catalog manifests file. The result is exactly as produced by a 'oc adm catalog mirror' command. See https://docs.openshift.com/container-platform/4.3/operators/olm-restricted-networks.html#olm-restricted-networks-operatorhub_olm-restricted-networks")
	@PostMapping("/createImageContentSourcePolicy")
	public String imageContentSourcePolicy(@RequestParam("file") MultipartFile file,  @RequestParam("mirrorUrl") String mirror, @RequestParam("name") String name) throws IOException {
		return GtarUtil.createImageContentSourcePolicy(file.getInputStream(), mirror, name);
	}
}
