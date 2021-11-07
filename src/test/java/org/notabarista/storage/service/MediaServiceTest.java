package org.notabarista.storage.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.notabarista.exception.AbstractNotabaristaException;
import org.notabarista.kafka.MediaEvent;
import org.notabarista.storage.exception.MediaStorageException;
import org.notabarista.storage.kafka.producer.MediaEventProducer;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class MediaServiceTest {

    @Mock
    private MediaEventProducer mediaEventProducer;

    @Mock
    private ItemService itemService;

    private MediaService mediaService;

    @BeforeEach
    void setUp() {
        mediaService = new MediaServiceImpl(itemService, mediaEventProducer);
    }

    @Test
    public void testAddMedia() throws JsonProcessingException, AbstractNotabaristaException {
        String itemID = "mockItemId", userID = "mockUserId";
        when(itemService.itemExists(itemID, userID)).thenReturn(true);
        when(mediaEventProducer.sendMediaEvent(any(MediaEvent.class))).thenReturn(null);

        mediaService.addMedia(itemID, userID, List.of("url1", "url2"));

        verify(mediaEventProducer, times(1)).sendMediaEvent(any(MediaEvent.class));
    }

    @Test
    public void testAddMediaItemNotFound() throws JsonProcessingException, AbstractNotabaristaException {
        String itemID = "mockItemId", userID = "mockUserId";
        when(itemService.itemExists(itemID, userID)).thenReturn(false);

        assertThrows(MediaStorageException.class, () -> mediaService.addMedia(itemID, userID, List.of("url1", "url2")));

        verify(mediaEventProducer, never()).sendMediaEvent(any(MediaEvent.class));
    }

    @Test
    public void testDeleteMedia() throws JsonProcessingException, AbstractNotabaristaException {
        String itemID = "mockItemId", userID = "mockUserId";
        when(itemService.itemExists(itemID, userID)).thenReturn(true);
        when(mediaEventProducer.sendMediaEvent(any(MediaEvent.class))).thenReturn(null);

        mediaService.deleteMedia(itemID, userID, List.of("url1", "url2"));

        verify(mediaEventProducer, times(1)).sendMediaEvent(any(MediaEvent.class));
    }

    @Test
    public void testDeleteMediaItemNotFound() throws JsonProcessingException, AbstractNotabaristaException {
        String itemID = "mockItemId", userID = "mockUserId";
        when(itemService.itemExists(itemID, userID)).thenReturn(false);

        assertThrows(MediaStorageException.class, () -> mediaService.deleteMedia(itemID, userID, List.of("url1", "url2")));

        verify(mediaEventProducer, never()).sendMediaEvent(any(MediaEvent.class));
    }
}
