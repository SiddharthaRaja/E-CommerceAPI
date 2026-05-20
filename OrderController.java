package com.sid.commerceapi.controller;

import com.sid.commerceapi.entity.*;

import com.sid.commerceapi.repository.*;

import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.security.core.Authentication;

import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/orders")
public class OrderController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private ProductRepository productRepository;

    // Checkout cart into order
    @PostMapping("/checkout")
    public Order checkout(Authentication authentication) {

        String username = authentication.getName();

        User user = userRepository.findByUsername(username)
                .orElseThrow(() ->
                        new RuntimeException("User not found"));

        Cart cart = cartRepository.findByUser(user)
                .orElseThrow(() ->
                        new RuntimeException("Cart not found"));

        // Validate cart not empty
        if (cart.getCartItems().isEmpty()) {
            throw new RuntimeException("Cart is empty");
        }

        Order order = new Order();

        order.setUser(user);

        List<OrderItem> orderItems = new ArrayList<>();

        double totalAmount = 0;

        // Convert cart items -> order items
        for (CartItem cartItem : cart.getCartItems()) {

            Product product = cartItem.getProduct();

            // Inventory validation
            if (product.getStockQty() < cartItem.getQuantity()) {

                throw new RuntimeException(
                        "Insufficient stock for product: "
                                + product.getName()
                );
            }

            // Reduce inventory
            product.setStockQty(
                    product.getStockQty() - cartItem.getQuantity()
            );

            productRepository.save(product);

            OrderItem orderItem = new OrderItem();

            orderItem.setOrder(order);

            orderItem.setProduct(product);

            orderItem.setQuantity(cartItem.getQuantity());

            // Snapshot purchase price
            orderItem.setPriceAtPurchase(product.getPrice());

            totalAmount +=
                    product.getPrice() * cartItem.getQuantity();

            orderItems.add(orderItem);
        }

        order.setTotalAmount(totalAmount);

        order.setOrderItems(orderItems);

        // Save order first
        Order savedOrder = orderRepository.save(order);

        // Save order items
        for (OrderItem orderItem : orderItems) {

            orderItem.setOrder(savedOrder);

            orderItemRepository.save(orderItem);
        }

        // Delete cart items from database
        cartItemRepository.deleteAll(cart.getCartItems());

        // Clear in-memory cart collection
        cart.getCartItems().clear();

        // Save updated cart
        cartRepository.save(cart);

        return savedOrder;
    }

    // Get logged-in user's orders
    @GetMapping
    public List<Order> getMyOrders(Authentication authentication) {

        String username = authentication.getName();

        User user = userRepository.findByUsername(username)
                .orElseThrow(() ->
                        new RuntimeException("User not found"));

        return orderRepository.findByUser(user);
    }
}