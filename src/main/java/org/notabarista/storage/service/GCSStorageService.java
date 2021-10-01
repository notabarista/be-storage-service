package org.notabarista.storage.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.log4j.Log4j2;
import org.notabarista.entity.response.Response;
import org.notabarista.exception.AbstractNotabaristaException;
import org.notabarista.kafka.MediaEvent;
import org.notabarista.kafka.MediaEventType;
import org.notabarista.service.util.IBackendRequestService;
import org.notabarista.service.util.enums.MicroService;
import org.notabarista.storage.exception.MediaStorageException;
import org.notabarista.storage.kafka.producer.MediaEventProducer;
import org.notabarista.util.NABConstants;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@Log4j2
public class GCSStorageService implements StorageService {

    private final IBackendRequestService backendRequestService;
    private final MediaEventProducer mediaEventProducer;

    public GCSStorageService(IBackendRequestService backendRequestService, MediaEventProducer mediaEventProducer) {
        this.backendRequestService = backendRequestService;
        this.mediaEventProducer = mediaEventProducer;
    }

    @Override
    public List<String> store(String itemID, MultipartFile[] files, String userID) throws AbstractNotabaristaException, JsonProcessingException {
        checkItemID(itemID, userID);

        List<String> mediaURLs = new ArrayList<>();
        for (MultipartFile file : files) {
            // TODO add logic to store and retrieve a public URL
            log.info("Uploaded media file '{}', content type '{}'", file.getResource().getFilename(), file.getContentType());
            mediaURLs.add(file.getResource().getFilename());
        }

        sendMediaEvent(MediaEventType.ADD, itemID, userID, mediaURLs);

        return mediaURLs;
    }

    @Override
    public void delete(String itemID, List<String> mediaURLs, String userID) throws JsonProcessingException, AbstractNotabaristaException {
        checkItemID(itemID, userID);

        mediaURLs.forEach(mediaURL -> {
            // TODO add logic to delete from storage
            log.info("Deleted media file with URL '{}'", mediaURL);
        });

        sendMediaEvent(MediaEventType.DELETE, itemID, userID, mediaURLs);
    }

    private void checkItemID(String itemID, String  userID) throws AbstractNotabaristaException {
        Response<Map<String, Object>> response = backendRequestService.executeGet(MicroService.BE_EC_CATALOG_SERVICE, "/item/" + itemID,
                null, new ParameterizedTypeReference<>() {
                }, Map.of(NABConstants.UID_HEADER_NAME, userID));
        if (response == null) {
            throw new MediaStorageException("Item not found");
        }
    }

    private void sendMediaEvent(MediaEventType mediaEventType, String itemID, String userID, List<String> mediaURLs) throws JsonProcessingException {
        mediaEventProducer.sendMediaEvent(MediaEvent.builder()
                                                    .mediaEventType(mediaEventType)
                                                    .itemID(itemID)
                                                    .mediaURLs(mediaURLs)
                                                    .userID(userID)
                                                    .build());
    }
}
