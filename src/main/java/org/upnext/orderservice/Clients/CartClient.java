package org.upnext.orderservice.Clients;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.upnext.orderservice.Configurations.FeignClientConfiguration;
import org.upnext.sharedlibrary.Dtos.CartDto;

@FeignClient(name = "cart-service", configuration = FeignClientConfiguration.class)
public interface CartClient {
    @GetMapping("/carts/me")
    CartDto getCart();

    @DeleteMapping("/carts/me")
    void clearCart();
}
