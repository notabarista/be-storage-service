package org.notabarista.storage.service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ObjectMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.notabarista.entity.response.Response;
import org.notabarista.exception.AbstractNotabaristaException;
import org.notabarista.kafka.MediaEvent;
import org.notabarista.service.util.IBackendRequestService;
import org.notabarista.service.util.enums.MicroService;
import org.notabarista.storage.exception.MediaStorageException;
import org.notabarista.storage.kafka.producer.MediaEventProducer;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class FilebaseStorageServiceTest {

    private static final String MOCK_BUCKET_NAME = "mock";

    @Mock
    private IBackendRequestService backendRequestService;

    @Mock
    private MediaEventProducer mediaEventProducer;

    @Mock
    private AmazonS3 bucket;

    @Mock
    private AmazonS3Client s3Client;

    private StorageService storageService;

    @BeforeEach
    void setUp() {
        storageService = new FilebaseStorageService(backendRequestService, mediaEventProducer, bucket, MOCK_BUCKET_NAME, s3Client);
    }

    @Test
    public void verifyStore() throws IOException, AbstractNotabaristaException {
        MockMultipartFile firstFile = new MockMultipartFile("files", "image1.jpg", "image/jpg", "mock data".getBytes());
        MockMultipartFile secondFile = new MockMultipartFile("files", "image2.png", "image/png", "mock data".getBytes());
        Response<Map<String, Object>> response = new Response<>();
        when(backendRequestService.executeGet(any(MicroService.class), anyString(),
                any(), any(ParameterizedTypeReference.class), anyMap())).thenReturn(response);

        storageService.store("mock", new MultipartFile[] {firstFile, secondFile}, "mock");

        verify(bucket, times(2)).putObject(any(String.class), anyString(), any(InputStream.class), any(ObjectMetadata.class));
        verify(mediaEventProducer, times(1)).sendMediaEvent(any(MediaEvent.class));
    }

    @Test
    public void verifyStoreItemNotFound() throws IOException, AbstractNotabaristaException {
        MockMultipartFile firstFile = new MockMultipartFile("files", "image1.jpg", "image/jpg", "mock data".getBytes());
        MockMultipartFile secondFile = new MockMultipartFile("files", "image2.png", "image/png", "mock data".getBytes());
        when(backendRequestService.executeGet(any(MicroService.class), anyString(),
                any(), any(ParameterizedTypeReference.class), anyMap())).thenReturn(null);

        assertThrows(
                MediaStorageException.class,
                () -> storageService.store("mock", new MultipartFile[] {firstFile, secondFile}, "mock"),
                "Expected store() to throw MediaStorageException, but it didn't"
        );

        verify(bucket, never()).putObject(any(String.class), anyString(), any(InputStream.class), any(ObjectMetadata.class));
        verify(mediaEventProducer, never()).sendMediaEvent(any(MediaEvent.class));
    }

    @Test
    public void verifyDelete() throws IOException, AbstractNotabaristaException {
        String fileName1 = "image1.png", fileName2 = "image2.png";
        Response<Map<String, Object>> response = new Response<>();
        when(backendRequestService.executeGet(any(MicroService.class), anyString(),
                any(), any(ParameterizedTypeReference.class), anyMap())).thenReturn(response);
        when(bucket.doesObjectExist(anyString(), anyString())).thenReturn(true);

        storageService.delete("mock", List.of("http://localhost/" + fileName1, "http://localhost/" + fileName2), "mock");

        verify(bucket, times(1)).deleteObject(MOCK_BUCKET_NAME, fileName1);
        verify(bucket, times(1)).deleteObject(MOCK_BUCKET_NAME, fileName2);
        verify(mediaEventProducer, times(1)).sendMediaEvent(any(MediaEvent.class));
    }

    @Test
    public void verifyDeleteItemNotFound() throws IOException, AbstractNotabaristaException {
        String fileName1 = "image1.png", fileName2 = "image2.png";
        when(backendRequestService.executeGet(any(MicroService.class), anyString(),
                any(), any(ParameterizedTypeReference.class), anyMap())).thenReturn(null);

        assertThrows(
                MediaStorageException.class,
                () -> storageService.delete("mock", List.of("http://localhost/" + fileName1, "http://localhost/" + fileName2), "mock"),
                "Expected store() to throw MediaStorageException, but it didn't"
        );

        verify(bucket, never()).deleteObject(MOCK_BUCKET_NAME, fileName1);
        verify(bucket, never()).deleteObject(MOCK_BUCKET_NAME, fileName2);
        verify(mediaEventProducer, never()).sendMediaEvent(any(MediaEvent.class));
    }

    @Test
    public void verifyDeleteMediaFileNotFound() throws IOException, AbstractNotabaristaException {
        String fileName1 = "image1.png", fileName2 = "image2.png";
        Response<Map<String, Object>> response = new Response<>();
        when(backendRequestService.executeGet(any(MicroService.class), anyString(),
                any(), any(ParameterizedTypeReference.class), anyMap())).thenReturn(response);
        when(bucket.doesObjectExist(anyString(), anyString())).thenReturn(false);

        assertThrows(
                MediaStorageException.class,
                () -> storageService.delete("mock", List.of("http://localhost/" + fileName1, "http://localhost/" + fileName2), "mock"),
                "Expected store() to throw MediaStorageException, but it didn't"
        );

        verify(bucket, never()).deleteObject(MOCK_BUCKET_NAME, fileName1);
        verify(bucket, never()).deleteObject(MOCK_BUCKET_NAME, fileName2);
        verify(mediaEventProducer, never()).sendMediaEvent(any(MediaEvent.class));
    }

    @Test
    public void verifyDeleteInvalidURLs() throws IOException, AbstractNotabaristaException {
        String fileName1 = "image1.png", fileName2 = "image2.png";
        Response<Map<String, Object>> response = new Response<>();
        when(backendRequestService.executeGet(any(MicroService.class), anyString(),
                any(), any(ParameterizedTypeReference.class), anyMap())).thenReturn(response);

        assertThrows(
                MalformedURLException.class,
                () -> storageService.delete("mock", List.of(fileName1, fileName2), "mock"),
                "Expected store() to throw MalformedURLException, but it didn't"
        );

        verify(bucket, never()).deleteObject(MOCK_BUCKET_NAME, fileName1);
        verify(bucket, never()).deleteObject(MOCK_BUCKET_NAME, fileName2);
        verify(mediaEventProducer, never()).sendMediaEvent(any(MediaEvent.class));
    }

}
