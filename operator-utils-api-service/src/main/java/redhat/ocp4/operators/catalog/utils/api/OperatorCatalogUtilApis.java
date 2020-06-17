package redhat.ocp4.operators.catalog.utils.api;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.micrometer.core.instrument.util.IOUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import redhat.ocp4.operators.catalog.utils.GtarUtil;

@Api(value = "Operator Catalog Management Apis")
@RestController
public class OperatorCatalogUtilApis {

	@ApiOperation(value = "produces a unique list of all of the images referenced in a tar.gz catalog manifests file")
	@PostMapping("/listAllImagesInCatalogManifests")
	public String[] listAllImages(@RequestParam("file") MultipartFile file) throws IOException {
		return GtarUtil.imagesInGtarArchive(file.getInputStream());
	}

	@ApiOperation(value = "returns a mapping.txt file from a tar.gz catalog  manifests file. The result is like that produced by a 'oc adm catalog mirror' command. See https://docs.openshift.com/container-platform/4.3/operators/olm-restricted-networks.html#olm-restricted-networks-operatorhub_olm-restricted-networks")
	@PostMapping("/mirrorImagesInCatalogManifests")
	public String mirrorImages(@RequestParam("file") MultipartFile file, @RequestParam("mirrorUrl") String mirror)
			throws IOException {
		return GtarUtil.createMirrorMappings(file.getInputStream(), mirror);
	}

	@ApiOperation(value = "creates an ImageContentSourcePolicy resource in Yaml from a tar.gz catalog manifests file. The result is exactly as produced by a 'oc adm catalog mirror' command. See https://docs.openshift.com/container-platform/4.3/operators/olm-restricted-networks.html#olm-restricted-networks-operatorhub_olm-restricted-networks")
	@PostMapping("/createImageContentSourcePolicy")
	public String imageContentSourcePolicy(@RequestParam("file") MultipartFile file,
			@RequestParam("mirrorUrl") String mirror, @RequestParam("name") String name) throws IOException {
		return GtarUtil.createImageContentSourcePolicy(file.getInputStream(), mirror, name);
	}

	@ApiOperation(value = "takes a tar.gz package manifest archive, and a map of registry mirrors to apply, and produces the same tar.gz archive, but with the mirror applied to every image reference ")
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@RequestMapping(value = "/applyImageMirrors", method = RequestMethod.POST)
	public void applyImageMirrors(@RequestParam("package-manifest-file") MultipartFile file, 
			@RequestParam("json-mirrors-file") MultipartFile mirrorsFile,
			HttpServletResponse response) throws IOException {
		Map mirrors = new ObjectMapper().readValue(mirrorsFile.getInputStream(), Map.class);
		GtarUtil.applyImageMirrors(mirrors,
				new TarArchiveInputStream(new GzipCompressorInputStream(file.getInputStream())),
				new TarArchiveOutputStream(new GzipCompressorOutputStream(response.getOutputStream())));
		response.flushBuffer();
	}
	
	@ApiOperation(value = "produces a unique list of registry hostnames from all of the images in the tar.gz archive. Useful if you need to figure out which registries  you might need credentials for to apply mirrors")
	@PostMapping("/listRegistriesInCatalogManifests")
	public List<String> listRegistriesInCatalogManifests(@RequestParam("file") MultipartFile file) throws IOException{
		return GtarUtil.registriesinGtarArchive(file.getInputStream());
	}
}
