package org.notabarista.storage.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.notabarista.exception.AbstractNotabaristaException;
import org.notabarista.kafka.MediaEvent;
import org.notabarista.kafka.MediaEventType;
import org.notabarista.storage.exception.MediaStorageException;
import org.notabarista.storage.kafka.producer.MediaEventProducer;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;

@Service
@Log4j2
public class MediaServiceImpl implements MediaService {

    private final ItemService itemService;
    private final MediaEventProducer mediaEventProducer;

    public MediaServiceImpl(ItemService itemService, MediaEventProducer mediaEventProducer) {
        this.itemService = itemService;
        this.mediaEventProducer = mediaEventProducer;
    }

    @Override
    public void addMedia(String itemID, String userID, List<String> mediaURLs) throws JsonProcessingException, AbstractNotabaristaException {
        if (!itemService.itemExists(itemID, userID)) {
            throw new MediaStorageException("Item not found");
        }
        sendMediaEvent(MediaEventType.ADD, itemID, userID, mediaURLs);
    }

    @Override
    public void deleteMedia(String itemID, String userID, List<String> mediaURLs) throws JsonProcessingException, AbstractNotabaristaException {
        if (!itemService.itemExists(itemID, userID)) {
            throw new MediaStorageException("Item not found");
        }
        sendMediaEvent(MediaEventType.DELETE, itemID, userID, mediaURLs);
    }

    private void sendMediaEvent(MediaEventType mediaEventType, String itemID, String userID, List<String> mediaURLs) throws JsonProcessingException {
        if (StringUtils.isBlank(itemID) || CollectionUtils.isEmpty(mediaURLs)) {
            throw new IllegalArgumentException("Invalid media event parameters.");
        }

        mediaEventProducer.sendMediaEvent(MediaEvent.builder()
                                                    .mediaEventType(mediaEventType)
                                                    .itemID(itemID)
                                                    .mediaURLs(mediaURLs)
                                                    .userID(userID)
                                                    .build());
    }
}
