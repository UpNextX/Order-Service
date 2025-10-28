package org.upnext.orderservice.Clients;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import org.upnext.orderservice.Configurations.FeignClientConfiguration;
import org.upnext.sharedlibrary.Dtos.ProductDto;
import org.upnext.sharedlibrary.Dtos.StockUpdateRequest;

@FeignClient(name = "product-service", configuration =  FeignClientConfiguration.class)
public interface ProductClient {
    @GetMapping("/products/{productId}")
    ProductDto getProduct(@PathVariable Long productId);

    @PutMapping("/products/{id}/stock")
    void updateStock(@PathVariable Long id, @RequestBody StockUpdateRequest stockUpdateRequest);
}
