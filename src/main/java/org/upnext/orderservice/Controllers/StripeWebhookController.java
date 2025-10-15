package org.upnext.orderservice.Controllers;

import com.stripe.model.Event;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.upnext.sharedlibrary.Dtos.SuccessfulPaymentEvent;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

import static org.upnext.orderservice.Configurations.PaymentRabbitMqConfig.*;

@RestController
@RequestMapping(("/stripe"))
@RequiredArgsConstructor
public class StripeWebhookController {

    public final AmqpTemplate paymentEventPublisher;
    @Value("${stripe.webhook.secret}")
    private String webhookKey;

    @PostMapping("/webhook")
    public ResponseEntity<String> handleStripeEvent(HttpServletRequest request) {
        System.out.println("Tm payment");
        try {
            String payload = new BufferedReader(new InputStreamReader(request.getInputStream()))
                    .lines().collect(Collectors.joining("\n"));

            String sigHeader = request.getHeader("Stripe-Signature");

            Event event = Webhook.constructEvent(payload, sigHeader, webhookKey);

            if (event.getType().equals("checkout.session.completed")) {
                handleCheckoutCompleted(event);
            }else if(event.getType().equals("payment_intent.payment_failed")) {
                handleChckoutFailed(event);
            }

            return ResponseEntity.ok("Success");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    private void handleChckoutFailed(Event event) {
        Session session = (Session) event.getDataObjectDeserializer().getObject().orElse(null);
        if(session == null) {
            System.out.println("Session is null");
            return;
        }
        System.out.println("Session is not null");
        Long orderId = Long.parseLong(session.getMetadata().get("orderId"));
        Long userId = Long.parseLong(session.getMetadata().get("userId"));
        SuccessfulPaymentEvent successfulPaymentEvent = SuccessfulPaymentEvent.builder()
                        .userId(userId)
                        .orderId(orderId)
                .build();


        paymentEventPublisher.convertAndSend(EXCHANGE, FAILURE_ROUTING_KEY, successfulPaymentEvent);
    }

    private void handleCheckoutCompleted(Event event) {
        Session session = (Session) event.getDataObjectDeserializer().getObject().orElse(null);
        if(session == null) {
            System.out.println("Session is null");
            return;
        }

        Long orderId = Long.parseLong(session.getMetadata().get("orderId"));
        System.out.println(orderId);
        Long userId = Long.parseLong(session.getMetadata().get("userId"));
        System.out.println(userId);
        SuccessfulPaymentEvent successfulPaymentEvent = SuccessfulPaymentEvent.builder()
                .userId(userId)
                .orderId(orderId)
                .build();
        System.out.println(successfulPaymentEvent);
        paymentEventPublisher.convertAndSend(EXCHANGE, SUCCESS_ROUTING_KEY, successfulPaymentEvent);

    }

}
