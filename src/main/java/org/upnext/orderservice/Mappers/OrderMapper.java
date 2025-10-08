package org.upnext.orderservice.Mappers;

import org.mapstruct.*;
import org.upnext.orderservice.Models.Order;
import org.upnext.orderservice.Models.OrderItem;
import org.upnext.sharedlibrary.Dtos.*;

@Mapper(componentModel="spring")
public interface OrderMapper {

    // Order to order Dto
    @Mapping(target = "orderId", source = "id")
    @Mapping(target = "totalCost", expression = "java(order.getTotalCost())" )
    @Mapping(target = "orderItems", source = "items")
    OrderDto toOrderDto(Order order);

    // OrderItem to OrderItem Dto
    @Mapping(target = "product", source = "productId", qualifiedByName = "productDtoFromId")
    OrderItemDto toOrderItemDto(OrderItem item);

    @Named("productDtoFromId")
    default ProductDto fromProductId(Long productId) {
        if(productId == null) {
            return null;
        }
        ProductDto productDto = new ProductDto();
        productDto.setId(productId);
        return productDto;
    }


    // CartDto to Order
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "orderStatus", ignore = true)
    @Mapping(target = "paymentStatus", ignore = true)
    @Mapping(target = "paymentMethod", ignore = true)
    @Mapping(target = "orderDate", ignore = true)
    @Mapping(target = "deliveryDate", ignore = true)
    @Mapping(target = "items", source = "items")
    Order fromCartDto(CartDto cartDto);

    // CartItemDto to OrderItem
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "order", ignore = true)
    @Mapping(target = "productId", source = "product.id")
    @Mapping(target = "price", source = "product.price")
    OrderItem fromCartItemDto(CartItemDto cartItemDto);

    @AfterMapping
    default void linkItems(@MappingTarget Order order) {
        if (order.getItems() != null) {
            order.getItems().forEach(item -> item.setOrder(order));
        }
    }

    // Order to OrderPaymentDto


}
