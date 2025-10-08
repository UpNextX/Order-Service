package org.upnext.orderservice.Events;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.upnext.sharedlibrary.Enums.OrderStatus;
import org.upnext.sharedlibrary.Enums.PaymentStatus;

@AllArgsConstructor
@Data
@NoArgsConstructor
public class PaymentEvent {
    Long orderId;
    PaymentStatus paymentStatus;
    OrderStatus orderStatus;
}
