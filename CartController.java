package com.sid.commerceapi.controller;

import com.sid.commerceapi.entity.Cart;
import com.sid.commerceapi.entity.CartItem;
import com.sid.commerceapi.entity.Product;
import com.sid.commerceapi.entity.User;

import com.sid.commerceapi.repository.CartItemRepository;
import com.sid.commerceapi.repository.CartRepository;
import com.sid.commerceapi.repository.ProductRepository;
import com.sid.commerceapi.repository.UserRepository;

import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.security.core.Authentication;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/cart")
public class CartController {

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private ProductRepository productRepository;

    // Create cart
    @PostMapping
    public Cart createCart(Authentication authentication) {

        String username = authentication.getName();

        User user = userRepository.findByUsername(username)
                .orElseThrow(() ->
                        new RuntimeException("User not found"));

        // Prevent duplicate carts
        if (cartRepository.findByUser(user).isPresent()) {
            throw new RuntimeException("Cart already exists");
        }

        Cart cart = new Cart();

        cart.setUser(user);

        return cartRepository.save(cart);
    }

    // Get logged-in user's cart
    @GetMapping
    public Cart getMyCart(Authentication authentication) {

        String username = authentication.getName();

        User user = userRepository.findByUsername(username)
                .orElseThrow(() ->
                        new RuntimeException("User not found"));

        return cartRepository.findByUser(user)
                .orElseThrow(() ->
                        new RuntimeException("Cart not found"));
    }

    // Add product to cart
    @PostMapping("/add/{productId}")
    public Cart addToCart(
            @PathVariable Long productId,
            Authentication authentication
    ) {

        String username = authentication.getName();

        User user = userRepository.findByUsername(username)
                .orElseThrow(() ->
                        new RuntimeException("User not found"));

        Cart cart = cartRepository.findByUser(user)
                .orElseThrow(() ->
                        new RuntimeException("Cart not found"));

        Product product = productRepository.findById(productId)
                .orElseThrow(() ->
                        new RuntimeException("Product not found"));

        // Check if product already exists in cart
        CartItem cartItem = cartItemRepository
                .findByCartIdAndProductId(
                        cart.getId(),
                        product.getId()
                )
                .orElse(null);

        // Increment quantity if already exists
        if (cartItem != null) {

            cartItem.setQuantity(
                    cartItem.getQuantity() + 1
            );

            cartItemRepository.save(cartItem);

        } else {

            cartItem = new CartItem();

            cartItem.setCart(cart);
            cartItem.setProduct(product);
            cartItem.setQuantity(1);

            cartItemRepository.save(cartItem);
        }

        // Return updated cart
        return cartRepository.findByUser(user)
                .orElseThrow(() ->
                        new RuntimeException("Cart not found"));
    }

    // Remove product from cart
    @PostMapping("/remove/{productId}")
    public Cart removeFromCart(
            @PathVariable Long productId,
            Authentication authentication
    ) {

        String username = authentication.getName();

        User user = userRepository.findByUsername(username)
                .orElseThrow(() ->
                        new RuntimeException("User not found"));

        Cart cart = cartRepository.findByUser(user)
                .orElseThrow(() ->
                        new RuntimeException("Cart not found"));

        CartItem cartItem = cartItemRepository
                .findByCartIdAndProductId(
                        cart.getId(),
                        productId
                )
                .orElseThrow(() ->
                        new RuntimeException("Product not found in cart"));

        // Decrement quantity
        if (cartItem.getQuantity() > 1) {

            cartItem.setQuantity(
                    cartItem.getQuantity() - 1
            );

            cartItemRepository.save(cartItem);

        } else {

            // Remove item completely
            cartItemRepository.delete(cartItem);
        }

        // Return updated cart
        return cartRepository.findByUser(user)
                .orElseThrow(() ->
                        new RuntimeException("Cart not found"));
    }
}