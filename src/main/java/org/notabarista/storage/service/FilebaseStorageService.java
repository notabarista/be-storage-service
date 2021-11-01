package org.notabarista.storage.service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ObjectMetadata;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@Log4j2
public class FilebaseStorageService implements StorageService {

    private final IBackendRequestService backendRequestService;
    private final MediaEventProducer mediaEventProducer;
    private final AmazonS3 bucket;
    private final String mediaStorageBucketName;
    private final AmazonS3Client s3Client;

    public FilebaseStorageService(IBackendRequestService backendRequestService, MediaEventProducer mediaEventProducer,
                                  AmazonS3 bucket, @Value("${filebase.bucket}") String mediaStorageBucketName, AmazonS3Client s3Client) {
        this.backendRequestService = backendRequestService;
        this.mediaEventProducer = mediaEventProducer;
        this.bucket = bucket;
        this.mediaStorageBucketName = mediaStorageBucketName;
        this.s3Client = s3Client;
    }

    @Override
    public List<String> store(String itemID, MultipartFile[] files, String userID) throws AbstractNotabaristaException, IOException {
        checkItemID(itemID, userID);

        List<String> mediaURLs = new ArrayList<>();
        for (MultipartFile file : files) {
            log.info("Uploading media file '{}', content type '{}'", file.getResource().getFilename(), file.getContentType());
            ObjectMetadata objectMetadata = new ObjectMetadata();
            objectMetadata.setUserMetadata(Map.of("itemID", itemID));
            objectMetadata.setContentType(file.getContentType());
            bucket.putObject(mediaStorageBucketName, file.getOriginalFilename(), file.getInputStream(), objectMetadata);
            String resourceUrl = s3Client.getResourceUrl(mediaStorageBucketName, file.getOriginalFilename());
            log.info("Uploaded media file '{}', content type '{}', URL '{}'",
                    file.getResource().getFilename(), file.getContentType(), resourceUrl);
            mediaURLs.add(resourceUrl);
        }

        sendMediaEvent(MediaEventType.ADD, itemID, userID, mediaURLs);

        return mediaURLs;
    }

    @Override
    public void delete(String itemID, List<String> mediaURLs, String userID) throws MalformedURLException, JsonProcessingException, AbstractNotabaristaException {
        checkItemID(itemID, userID);

        for (String mediaURL:mediaURLs) {
            // https://{BUCKET}.s3.filebase.com/{OBJECT_KEY}
            URL aURL = new URL(mediaURL);
            String objectKey = aURL.getPath().substring(1);
            if (!bucket.doesObjectExist(mediaStorageBucketName, objectKey)) {
                throw new MediaStorageException("Unknown media file: " + objectKey);
            }

            log.info("Deleting object '{}' in bucket '{}'", objectKey, mediaStorageBucketName);
            bucket.deleteObject(mediaStorageBucketName, objectKey);
            log.info("Deleted media file with URL '{}'", mediaURL);
        }

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
