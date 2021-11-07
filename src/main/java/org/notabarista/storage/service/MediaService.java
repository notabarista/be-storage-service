package org.notabarista.storage.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.notabarista.exception.AbstractNotabaristaException;

import java.util.List;

public interface MediaService {
    void addMedia(String itemID, String userID, List<String> mediaURLs) throws JsonProcessingException, AbstractNotabaristaException;
    void deleteMedia(String itemID, String userID, List<String> mediaURLs) throws JsonProcessingException, AbstractNotabaristaException;
}
