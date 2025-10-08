package org.upnext.orderservice.Errors;

import org.upnext.sharedlibrary.Errors.Error;
public class OrderErrors {
    public static final Error OrderNotFound = new Error("Order.NotFound", "Order Not Found", 404);
    public static final Error ProductStockInSufficient = new Error("Product.LowStock", "Product Stock In Sufficient", 400);
    public static final Error EmptyCart = new Error("EmptyCart", "Empty Cart", 400);
}
