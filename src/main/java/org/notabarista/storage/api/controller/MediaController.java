package org.notabarista.storage.api.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.log4j.Log4j2;
import org.notabarista.exception.AbstractNotabaristaException;
import org.notabarista.storage.api.validator.ContentType;
import org.notabarista.storage.service.StorageService;
import org.notabarista.util.NABConstants;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.List;

@Log4j2
@RestController
@CrossOrigin
@RequestMapping("/")
@Validated
public class MediaController {

    private final StorageService storageService;

    public MediaController(StorageService storageService) {
        this.storageService = storageService;
    }

    @PostMapping
    public ResponseEntity<List<String>> storeFiles(@RequestParam("itemID") @NotBlank String itemID,
                                                   @RequestParam("files") @NotEmpty @ContentType(contentTypes = "image/*") MultipartFile[] files,
                                                   @RequestHeader(NABConstants.UID_HEADER_NAME) String userId) throws AbstractNotabaristaException, IOException {
        List<String> mediaURLs = storageService.store(itemID, files, userId);
        return new ResponseEntity<>(mediaURLs, HttpStatus.OK);
    }

    @DeleteMapping
    public ResponseEntity<String> deleteFiles(@RequestParam("itemID") @NotBlank String itemID, @RequestBody @NotEmpty List<String> mediaURLs,
                                              @RequestHeader(NABConstants.UID_HEADER_NAME) String userId) throws AbstractNotabaristaException, JsonProcessingException, MalformedURLException {
        storageService.delete(itemID, mediaURLs, userId);
        return new ResponseEntity<>("Media files deleted successfully!", HttpStatus.OK);
    }

}
