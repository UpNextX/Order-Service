package org.upnext.orderservice.Exceptions;

public class ProductStockException extends RuntimeException {
    public ProductStockException(String message) {
        super(message);
    }
}
