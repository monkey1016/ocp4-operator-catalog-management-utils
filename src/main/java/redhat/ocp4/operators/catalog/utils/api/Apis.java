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

import redhat.ocp4.operators.catalog.utils.GtarUtil;

@RestController
public class Apis {


	
	@PostMapping("/listAllImages")
	public String[] listAllImages(@RequestParam("file") MultipartFile file) throws IOException {
		return GtarUtil.imagesInGtarArchive(file.getInputStream());
	}
	
	@PostMapping("/mirrorImages")
	public String mirrorImages(@RequestParam("file") MultipartFile file,  @RequestParam("mirror") String mirror ) throws IOException {
		return GtarUtil.createMirrorMappings(file.getInputStream(), mirror);
	}
	
	@PostMapping("/imageContentSourcePolicy")
	public String imageContentSourcePolicy(@RequestParam("file") MultipartFile file,  @RequestParam("mirror") String mirror, @RequestParam("name") String name) throws IOException {
		return GtarUtil.createImageContentSourcePolicy(file.getInputStream(), mirror, name);
	}
}
