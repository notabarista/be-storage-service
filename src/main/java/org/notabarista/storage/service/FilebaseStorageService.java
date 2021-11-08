package org.notabarista.storage.service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.log4j.Log4j2;
import org.notabarista.exception.AbstractNotabaristaException;
import org.notabarista.storage.exception.MediaStorageException;
import org.springframework.beans.factory.annotation.Value;
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

    private final ItemService itemService;
    private final MediaService mediaService;
    private final AmazonS3 bucket;
    private final String mediaStorageBucketName;
    private final AmazonS3Client s3Client;

    public FilebaseStorageService(ItemService itemService, MediaService mediaService,
                                  AmazonS3 bucket, @Value("${filebase.bucket}") String mediaStorageBucketName, AmazonS3Client s3Client) {
        this.itemService = itemService;
        this.mediaService = mediaService;
        this.bucket = bucket;
        this.mediaStorageBucketName = mediaStorageBucketName;
        this.s3Client = s3Client;
    }

    @Override
    public List<String> store(String itemID, MultipartFile[] files, String userID) throws AbstractNotabaristaException, IOException {
        if (!itemService.itemExists(itemID, userID)) {
            throw new MediaStorageException("Item not found");
        }

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

        mediaService.addMedia(itemID, userID, mediaURLs);

        return mediaURLs;
    }

    @Override
    public void delete(String itemID, List<String> mediaURLs, String userID) throws MalformedURLException, JsonProcessingException, AbstractNotabaristaException {
        if (!itemService.itemExists(itemID, userID)) {
            throw new MediaStorageException("Item not found");
        }

        for (String mediaURL : mediaURLs) {
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

        mediaService.deleteMedia(itemID, userID, mediaURLs);
    }
}
