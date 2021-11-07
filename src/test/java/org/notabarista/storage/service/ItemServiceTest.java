package org.notabarista.storage.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.notabarista.entity.response.Response;
import org.notabarista.entity.response.ResponseStatus;
import org.notabarista.exception.AbstractNotabaristaException;
import org.notabarista.service.util.IBackendRequestService;
import org.notabarista.service.util.enums.MicroService;
import org.springframework.core.ParameterizedTypeReference;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;

@ExtendWith(MockitoExtension.class)
public class ItemServiceTest {

    @Mock
    private IBackendRequestService backendRequestService;

    private ItemService itemService;

    @BeforeEach
    void setUp() {
        itemService = new ItemServiceImpl(backendRequestService);
    }

    @Test
    void itemExistsTrue() throws AbstractNotabaristaException {
        Response<Map<String, Object>> response = new Response<>();
        response.setStatus(ResponseStatus.SUCCESS);
        Mockito.when(backendRequestService.executeGet(any(MicroService.class), anyString(),
                any(), any(ParameterizedTypeReference.class), anyMap())).thenReturn(response);

        assertTrue(itemService.itemExists("mockItemID", "mockUserID"));
    }

    @Test
    void itemExistsFalse() throws AbstractNotabaristaException {
        Response<Map<String, Object>> response = new Response<>();
        Mockito.when(backendRequestService.executeGet(any(MicroService.class), anyString(),
                any(), any(ParameterizedTypeReference.class), anyMap())).thenReturn(null);

        assertFalse(itemService.itemExists("mockItemID", "mockUserID"));
    }

}
