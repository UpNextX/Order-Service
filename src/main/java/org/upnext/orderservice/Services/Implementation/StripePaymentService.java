package org.upnext.orderservice.Services.Implementation;

import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.upnext.orderservice.Models.Order;
import org.upnext.orderservice.Models.OrderItem;

@Service
public class StripePaymentService {
    @Value("${stripe.success-url}")
    private String successUrl;

    @Value("${stripe.failure-url}")
    private String failureUrl;

    public Session createSession(Order order) throws StripeException {
        SessionCreateParams.Builder builder = SessionCreateParams.builder();
        builder.setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl(successUrl)
                .setCancelUrl(failureUrl)
                .putMetadata("orderId", order.getId().toString())
                        .putMetadata("userId", order.getUserId().toString());
        System.out.println("Create Session");
        for (OrderItem item : order.getItems()) {
            builder.addLineItem(
                    SessionCreateParams.LineItem.builder()
                            .setQuantity(item.getQuantity().longValue())
                            .setPriceData(
                                    SessionCreateParams.LineItem.PriceData.builder()
                                            .setCurrency("usd")
                                            .setUnitAmount((long) (item.getPrice() * 100))
                                            .setProductData(
                                                    SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                            .setName("Product #" + item.getProductId())
                                                            .build()
                                            )
                                            .build()
                            )
                            .build()
            );
        }
        System.out.println("Session Created");
        SessionCreateParams sessionCreateParams = builder.build();
        return Session.create(sessionCreateParams);

    }
}
