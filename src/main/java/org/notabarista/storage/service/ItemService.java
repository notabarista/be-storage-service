package org.notabarista.storage.service;

import org.notabarista.exception.AbstractNotabaristaException;

public interface ItemService {
    boolean itemExists(String itemID, String userID) throws AbstractNotabaristaException;
}
