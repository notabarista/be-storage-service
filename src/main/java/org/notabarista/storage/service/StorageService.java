package org.notabarista.storage.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.notabarista.exception.AbstractNotabaristaException;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface StorageService {
    List<String> store(String itemID, MultipartFile[] files, String userID) throws JsonProcessingException, AbstractNotabaristaException;

    void delete(String itemID, List<String> mediaURLs, String userID) throws JsonProcessingException, AbstractNotabaristaException;
}
