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
import org.notabarista.storage.exception.MediaStorageException;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class FilebaseStorageServiceTest {

    private static final String MOCK_BUCKET_NAME = "mock";

    @Mock
    private AmazonS3 bucket;

    @Mock
    private AmazonS3Client s3Client;

    @Mock
    private ItemService itemService;

    @Mock
    private MediaService mediaService;

    private StorageService storageService;

    @BeforeEach
    void setUp() {
        storageService = new FilebaseStorageService(itemService, mediaService, bucket, MOCK_BUCKET_NAME, s3Client);
    }

    @Test
    public void verifyStore() throws IOException, AbstractNotabaristaException {
        MockMultipartFile firstFile = new MockMultipartFile("files", "image1.jpg", "image/jpg", "mock data".getBytes());
        MockMultipartFile secondFile = new MockMultipartFile("files", "image2.png", "image/png", "mock data".getBytes());
        Response<Map<String, Object>> response = new Response<>();
        when(itemService.itemExists(anyString(), anyString())).thenReturn(true);

        storageService.store("mock", new MultipartFile[]{firstFile, secondFile}, "mock");

        verify(bucket, times(2)).putObject(anyString(), anyString(), any(InputStream.class), any(ObjectMetadata.class));
        verify(mediaService, times(1)).addMedia(anyString(), anyString(), anyList());
    }

    @Test
    public void verifyStoreItemNotFound() throws IOException, AbstractNotabaristaException {
        MockMultipartFile firstFile = new MockMultipartFile("files", "image1.jpg", "image/jpg", "mock data".getBytes());
        MockMultipartFile secondFile = new MockMultipartFile("files", "image2.png", "image/png", "mock data".getBytes());
        when(itemService.itemExists(anyString(), anyString())).thenReturn(false);

        assertThrows(
                MediaStorageException.class,
                () -> storageService.store("mock", new MultipartFile[]{firstFile, secondFile}, "mock"),
                "Expected store() to throw MediaStorageException, but it didn't"
        );

        verify(bucket, never()).putObject(any(String.class), anyString(), any(InputStream.class), any(ObjectMetadata.class));
        verify(mediaService, never()).addMedia(anyString(), anyString(), anyList());
    }

    @Test
    public void verifyDelete() throws IOException, AbstractNotabaristaException {
        String fileName1 = "image1.png", fileName2 = "image2.png";
        Response<Map<String, Object>> response = new Response<>();
        when(itemService.itemExists(anyString(), anyString())).thenReturn(true);
        when(bucket.doesObjectExist(anyString(), anyString())).thenReturn(true);

        storageService.delete("mock", List.of("http://localhost/" + fileName1, "http://localhost/" + fileName2), "mock");

        verify(bucket, times(1)).deleteObject(MOCK_BUCKET_NAME, fileName1);
        verify(bucket, times(1)).deleteObject(MOCK_BUCKET_NAME, fileName2);
        verify(mediaService, times(1)).deleteMedia(anyString(), anyString(), anyList());
    }

    @Test
    public void verifyDeleteItemNotFound() throws IOException, AbstractNotabaristaException {
        String fileName1 = "image1.png", fileName2 = "image2.png";
        when(itemService.itemExists(anyString(), anyString())).thenReturn(false);

        assertThrows(
                MediaStorageException.class,
                () -> storageService.delete("mock", List.of("http://localhost/" + fileName1, "http://localhost/" + fileName2), "mock"),
                "Expected store() to throw MediaStorageException, but it didn't"
        );

        verify(bucket, never()).deleteObject(MOCK_BUCKET_NAME, fileName1);
        verify(bucket, never()).deleteObject(MOCK_BUCKET_NAME, fileName2);
        verify(mediaService, never()).deleteMedia(anyString(), anyString(), anyList());
    }

    @Test
    public void verifyDeleteMediaFileNotFound() throws IOException, AbstractNotabaristaException {
        String fileName1 = "image1.png", fileName2 = "image2.png";
        Response<Map<String, Object>> response = new Response<>();
        when(itemService.itemExists(anyString(), anyString())).thenReturn(true);
        when(bucket.doesObjectExist(anyString(), anyString())).thenReturn(false);

        assertThrows(
                MediaStorageException.class,
                () -> storageService.delete("mock", List.of("http://localhost/" + fileName1, "http://localhost/" + fileName2), "mock"),
                "Expected store() to throw MediaStorageException, but it didn't"
        );

        verify(bucket, never()).deleteObject(MOCK_BUCKET_NAME, fileName1);
        verify(bucket, never()).deleteObject(MOCK_BUCKET_NAME, fileName2);
        verify(mediaService, never()).deleteMedia(anyString(), anyString(), anyList());
    }

    @Test
    public void verifyDeleteInvalidURLs() throws IOException, AbstractNotabaristaException {
        String fileName1 = "image1.png", fileName2 = "image2.png";
        Response<Map<String, Object>> response = new Response<>();
        when(itemService.itemExists(anyString(), anyString())).thenReturn(true);

        assertThrows(
                MalformedURLException.class,
                () -> storageService.delete("mock", List.of(fileName1, fileName2), "mock"),
                "Expected store() to throw MalformedURLException, but it didn't"
        );

        verify(bucket, never()).deleteObject(MOCK_BUCKET_NAME, fileName1);
        verify(bucket, never()).deleteObject(MOCK_BUCKET_NAME, fileName2);
        verify(mediaService, never()).deleteMedia(anyString(), anyString(), anyList());
    }

}
