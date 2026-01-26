package de.jivz.analyticsservice.parser;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

public interface FileParser {
    List<Map<String, String>> parse(MultipartFile file) throws Exception;
    boolean supports(String filename);
}
