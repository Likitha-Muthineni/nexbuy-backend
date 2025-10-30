package com.nexbuy.controller;

import com.nexbuy.dto.CartItemDTO;
import com.nexbuy.dto.MessageResponse;
import com.nexbuy.model.*;
import com.nexbuy.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/cart")
@CrossOrigin(origins = "*")
public class CartController {
    
    @Autowired private CartRepository cartRepository;
    @Autowired private CartItemRepository cartItemRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private ProductRepository productRepository;
    
    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<?> getCart(Authentication authentication) {
        try {
            User user = getUserFromAuth(authentication);
            Cart cart = cartRepository.findByUser(user).orElseGet(() -> createCart(user));
            
            // Force load cart items
            int size = cart.getCartItems().size();
            
            List<CartItemDTO> items = new ArrayList<>();
            for (CartItem cartItem : cart.getCartItems()) {
                items.add(convertToDTO(cartItem));
            }
            
            System.out.println("✅ Cart loaded: " + items.size() + " items");
            return ResponseEntity.ok(items);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(new MessageResponse("Error: " + e.getMessage()));
        }
    }
    
    @PostMapping("/add/{productId}")
    @Transactional
    public ResponseEntity<?> addToCart(@PathVariable Long productId, 
                                       @RequestParam(defaultValue = "1") Integer quantity,
                                       Authentication authentication) {
        try {
            User user = getUserFromAuth(authentication);
            Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));
            
            if (product.getStock() < quantity) {
                return ResponseEntity.badRequest()
                    .body(new MessageResponse("Insufficient stock"));
            }
            
            Cart cart = cartRepository.findByUser(user)
                .orElseGet(() -> createCart(user));
            
            // Check if product already in cart
            CartItem existingItem = null;
            for (CartItem item : cart.getCartItems()) {
                if (item.getProduct().getId().equals(productId)) {
                    existingItem = item;
                    break;
                }
            }
            
            if (existingItem != null) {
                existingItem.setQuantity(existingItem.getQuantity() + quantity);
                cartItemRepository.save(existingItem);
                System.out.println("✅ Updated quantity");
            } else {
                CartItem cartItem = new CartItem();
                cartItem.setCart(cart);
                cartItem.setProduct(product);
                cartItem.setQuantity(quantity);
                cartItemRepository.save(cartItem);
                System.out.println("✅ Added new item");
            }
            
            return ResponseEntity.ok(new MessageResponse("Product added to cart"));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest()
                .body(new MessageResponse("Failed: " + e.getMessage()));
        }
    }
    
    @DeleteMapping("/remove/{cartItemId}")
    @Transactional
    public ResponseEntity<?> removeFromCart(@PathVariable Long cartItemId) {
        try {
            CartItem cartItem = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new RuntimeException("Cart item not found"));
            cartItemRepository.delete(cartItem);
            System.out.println("✅ Item removed");
            return ResponseEntity.ok(new MessageResponse("Item removed"));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest()
                .body(new MessageResponse("Error: " + e.getMessage()));
        }
    }
    
    @DeleteMapping("/clear")
    @Transactional
    public ResponseEntity<?> clearCart(Authentication authentication) {
        try {
            User user = getUserFromAuth(authentication);
            Cart cart = cartRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Cart not found"));
            
            List<CartItem> items = new ArrayList<>(cart.getCartItems());
            for (CartItem item : items) {
                cartItemRepository.delete(item);
            }
            
            return ResponseEntity.ok(new MessageResponse("Cart cleared"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(new MessageResponse("Error: " + e.getMessage()));
        }
    }
    
    private User getUserFromAuth(Authentication authentication) {
        String email = authentication.getName();
        return userRepository.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("User not found"));
    }
    
    @Transactional
    private Cart createCart(User user) {
        Cart cart = new Cart();
        cart.setUser(user);
        return cartRepository.save(cart);
    }
    
    private CartItemDTO convertToDTO(CartItem item) {
        Product product = item.getProduct();
        return CartItemDTO.builder()
            .id(item.getId())
            .productId(product.getId())
            .productName(product.getName())
            .price(product.getPrice())
            .imageUrl(product.getImageUrl())
            .quantity(item.getQuantity())
            .subtotal(product.getPrice() * item.getQuantity())
            .build();
    }
}