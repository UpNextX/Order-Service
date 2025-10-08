package org.upnext.orderservice.Repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.upnext.orderservice.Models.Order;

import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findAllByUserId(Long userId);
}
