package org.notabarista.storage.service;

import lombok.extern.log4j.Log4j2;
import org.notabarista.entity.response.Response;
import org.notabarista.entity.response.ResponseStatus;
import org.notabarista.exception.AbstractNotabaristaException;
import org.notabarista.service.util.IBackendRequestService;
import org.notabarista.service.util.enums.MicroService;
import org.notabarista.util.NABConstants;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@Log4j2
public class ItemServiceImpl implements ItemService {

    private final IBackendRequestService backendRequestService;

    public ItemServiceImpl(IBackendRequestService backendRequestService) {
        this.backendRequestService = backendRequestService;
    }

    @Override
    public boolean itemExists(String itemID, String userID) throws AbstractNotabaristaException {
        Response<Map<String, Object>> response = backendRequestService.executeGet(MicroService.BE_EC_CATALOG_SERVICE, "/item/" + itemID,
                null, new ParameterizedTypeReference<>() {
                }, Map.of(NABConstants.UID_HEADER_NAME, userID));
        return response != null && response.getStatus() == ResponseStatus.SUCCESS;
    }
}
