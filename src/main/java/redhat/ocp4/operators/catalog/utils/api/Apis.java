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

	@GetMapping("/hello")
	public String hello(@RequestParam(value = "name", defaultValue = "World") String name) {
		return String.format("Hello %s!", name);
	}
	
	@PostMapping("/listArchiveEntries")
	public String[] listArchiveEntries(@RequestParam("file") MultipartFile file) throws IOException {
		return GtarUtil.listEntriesInGtarArchive(file.getInputStream());
	}
	
	@PostMapping("/listAllImages")
	public String[] listAllImages(@RequestParam("file") MultipartFile file) throws IOException {
		return GtarUtil.imagesInGtarArchive(file.getInputStream());
	}
	
	@PostMapping("/mirrorImages")
	public String mirrorImages(@RequestParam("file") MultipartFile file,  @RequestParam("mirror") String mirror ) throws IOException {
		String[] images = GtarUtil.imagesInGtarArchive(file.getInputStream());
		List<String> mappings = Arrays.asList(images).stream().map(s ->s + "=" + mirror + s.substring(s.indexOf("/"))).collect(Collectors.toList());
		StringBuffer strBuf = new StringBuffer();
		mappings.stream().sorted().forEach(mapping -> strBuf.append(mapping + System.lineSeparator()));
		return strBuf.toString();
	}
}
