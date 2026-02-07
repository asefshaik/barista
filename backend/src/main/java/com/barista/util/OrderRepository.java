package com.barista.util;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.barista.model.Order;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
}
