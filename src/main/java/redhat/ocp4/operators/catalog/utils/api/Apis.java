package redhat.ocp4.operators.catalog.utils.api;

import java.io.IOException;

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
}
