package org.notabarista.storage.api.controller;

import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.Test;
import org.notabarista.service.util.IBackendRequestService;
import org.notabarista.service.util.ICheckAccessService;
import org.notabarista.storage.exception.MediaStorageException;
import org.notabarista.storage.kafka.producer.MediaEventProducer;
import org.notabarista.storage.service.StorageService;
import org.notabarista.util.NABConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.web.multipart.MultipartFile;

import java.net.MalformedURLException;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MediaController.class)
@Log4j2
public class MediaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MediaEventProducer mediaEventProducer;

    @MockBean
    private IBackendRequestService backendRequestService;

    @MockBean
    private ICheckAccessService checkAccessService;

    @MockBean
    private StorageService storageService;

    @Test
    public void storeFiles_validInputShouldReturnValidOutput() throws Exception {
        // given
        String userIDHeader = "mock";
        String itemID = "mock";
        MockMultipartFile firstFile = new MockMultipartFile("files", "image1.jpg", "image/jpg", "mock data".getBytes());
        MockMultipartFile secondFile = new MockMultipartFile("files", "image2.png", "image/png", "mock data".getBytes());
        List<String> mockMediaURLs = List.of("url1", "url2");
        when(storageService.store(itemID, new MultipartFile[] {firstFile, secondFile}, userIDHeader)).thenReturn(mockMediaURLs);

        // when
        this.mockMvc.perform(MockMvcRequestBuilders.multipart("/")
                                                                         .file(firstFile)
                                                                         .file(secondFile)
                                                                         .param("itemID", itemID)
                                                                         .header(NABConstants.UID_HEADER_NAME, userIDHeader))
                                          .andDo(print())
                                          // then
                                          .andExpect(status().isOk())
                                          .andExpect(jsonPath("$", hasSize(2)))
                                          .andExpect(jsonPath("$[0]", is(mockMediaURLs.get(0))))
                                          .andExpect(jsonPath("$[1]", is(mockMediaURLs.get(1))));
    }

    @Test
    public void storeFiles_invalidFileTypeShouldReturnBadRequest() throws Exception {
        // given
        String userIDHeader = "mock";
        String itemID = "mock";
        MockMultipartFile firstFile = new MockMultipartFile("files", "image1.jpg", "image/jpg", "mock data".getBytes());
        MockMultipartFile secondFile = new MockMultipartFile("files", "file.txt", "text/plain", "mock data".getBytes());

        // when
        this.mockMvc.perform(MockMvcRequestBuilders.multipart("/")
                                                   .file(firstFile)
                                                   .file(secondFile)
                                                   .param("itemID", itemID)
                                                   .header(NABConstants.UID_HEADER_NAME, userIDHeader))
                    .andDo(print())
                    // then
                    .andExpect(status().isBadRequest())
                    .andExpect(content().string(containsString("Invalid content type: text/plain")));
    }

    @Test
    public void storeFiles_missingFilesShouldReturnBadRequest() throws Exception {
        // given
        String userIDHeader = "mock";
        String itemID = "mock";

        // when
        this.mockMvc.perform(MockMvcRequestBuilders.multipart("/")
                                                   .param("itemID", itemID)
                                                   .header(NABConstants.UID_HEADER_NAME, userIDHeader))
                    .andDo(print())
                    // then
                    .andExpect(status().isBadRequest());
    }

    @Test
    public void storeFiles_missingItemIDShouldReturnBadRequest() throws Exception {
        // given
        String userIDHeader = "mock";
        MockMultipartFile firstFile = new MockMultipartFile("files", "image1.jpg", "image/jpg", "mock data".getBytes());
        MockMultipartFile secondFile = new MockMultipartFile("files", "image2.png", "image/png", "mock data".getBytes());

        // when
        this.mockMvc.perform(MockMvcRequestBuilders.multipart("/")
                                                   .file(firstFile)
                                                   .file(secondFile)
                                                   .header(NABConstants.UID_HEADER_NAME, userIDHeader))
                    .andDo(print())
                    // then
                    .andExpect(status().isBadRequest());
    }

    @Test
    public void storeFiles_ItemIDNotFoundShouldReturnBadRequest() throws Exception {
        // given
        String userIDHeader = "mock";
        String itemID = "mock";
        MockMultipartFile firstFile = new MockMultipartFile("files", "image1.jpg", "image/jpg", "mock data".getBytes());
        MockMultipartFile secondFile = new MockMultipartFile("files", "image2.png", "image/png", "mock data".getBytes());
        when(storageService.store(itemID, new MultipartFile[] {firstFile, secondFile}, userIDHeader)).thenThrow(MediaStorageException.class);

        // when
        this.mockMvc.perform(MockMvcRequestBuilders.multipart("/")
                                                   .file(firstFile)
                                                   .file(secondFile)
                                                   .header(NABConstants.UID_HEADER_NAME, userIDHeader))
                    .andDo(print())
                    // then
                    .andExpect(status().isBadRequest());
    }

    @Test
    public void deleteFiles_validInputShouldReturnValidOutput() throws Exception {
        // given
        String userIDHeader = "mock";
        String itemID = "mock";
        String url1 = "url1", url2 = "url2";
        List<String> mockMediaURLs = List.of(url1, url2);
        doNothing().when(storageService).delete(itemID, mockMediaURLs, userIDHeader);

        // when
        this.mockMvc.perform(MockMvcRequestBuilders.delete("/")
                                                   .contentType(MediaType.APPLICATION_JSON_VALUE)
                                                   .content("[\"" + url1 + "\", \"" + url2 +"\"]")
                                                   .param("itemID", itemID)
                                                   .header(NABConstants.UID_HEADER_NAME, userIDHeader))
                    .andDo(print())
                    // then
                    .andExpect(status().isOk())
                    .andExpect(content().string("Media files deleted successfully!"));
    }

    @Test
    public void deleteFiles_missingMediaURLsShouldReturnBadRequest() throws Exception {
        // given
        String userIDHeader = "mock";
        String itemID = "mock";
        String url1 = "url1", url2 = "url2";
        List<String> mockMediaURLs = List.of(url1, url2);
        doNothing().when(storageService).delete(itemID, mockMediaURLs, userIDHeader);

        // when
        this.mockMvc.perform(MockMvcRequestBuilders.delete("/")
                                                   .param("itemID", itemID)
                                                   .header(NABConstants.UID_HEADER_NAME, userIDHeader))
                    .andDo(print())
                    // then
                    .andExpect(status().isBadRequest());
    }

    @Test
    public void deleteFiles_missingItemIDShouldReturnBadRequest() throws Exception {
        // given
        String userIDHeader = "mock";
        String itemID = "mock";
        String url1 = "url1", url2 = "url2";
        List<String> mockMediaURLs = List.of(url1, url2);
        doNothing().when(storageService).delete(itemID, mockMediaURLs, userIDHeader);

        // when
        this.mockMvc.perform(MockMvcRequestBuilders.delete("/")
                                                   .contentType(MediaType.APPLICATION_JSON_VALUE)
                                                   .content("[\"" + url1 + "\", \"" + url2 +"\"]")
                                                   .header(NABConstants.UID_HEADER_NAME, userIDHeader))
                    .andDo(print())
                    // then
                    .andExpect(status().isBadRequest());
    }

    @Test
    public void deleteFiles_ItemIDNotFoundShouldReturnBadRequest() throws Exception {
        // given
        String userIDHeader = "mock";
        String itemID = "mock";
        String url1 = "url1", url2 = "url2";
        List<String> mockMediaURLs = List.of(url1, url2);
        doThrow(MediaStorageException.class).when(storageService).delete(itemID, mockMediaURLs, userIDHeader);

        // when
        this.mockMvc.perform(MockMvcRequestBuilders.delete("/")
                                                   .contentType(MediaType.APPLICATION_JSON_VALUE)
                                                   .content("[\"" + url1 + "\", \"" + url2 +"\"]")
                                                   .param("itemID", itemID)
                                                   .header(NABConstants.UID_HEADER_NAME, userIDHeader))
                    .andDo(print())
                    // then
                    .andExpect(status().isBadRequest());
    }

    @Test
    public void deleteFiles_InvalidMediaURLsShouldReturnBadRequest() throws Exception {
        // given
        String userIDHeader = "mock";
        String itemID = "mock";
        String url1 = "url1", url2 = "url2";
        List<String> mockMediaURLs = List.of(url1, url2);
        doThrow(MalformedURLException.class).when(storageService).delete(itemID, mockMediaURLs, userIDHeader);

        // when
        this.mockMvc.perform(MockMvcRequestBuilders.delete("/")
                                                   .contentType(MediaType.APPLICATION_JSON_VALUE)
                                                   .content("[\"" + url1 + "\", \"" + url2 +"\"]")
                                                   .param("itemID", itemID)
                                                   .header(NABConstants.UID_HEADER_NAME, userIDHeader))
                    .andDo(print())
                    // then
                    .andExpect(status().isBadRequest());
    }

}
