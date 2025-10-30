// OrderController.java
// Location: src/main/java/com/nexbuy/controller/OrderController.java

package com.nexbuy.controller;

import com.nexbuy.dto.CheckoutRequest;
import com.nexbuy.dto.MessageResponse;
import com.nexbuy.dto.OrderDTO;
import com.nexbuy.dto.OrderItemDTO;
import com.nexbuy.model.Cart;
import com.nexbuy.model.CartItem;
import com.nexbuy.model.Order;
import com.nexbuy.model.OrderItem;
import com.nexbuy.model.OrderStatus;
import com.nexbuy.model.Product;
import com.nexbuy.model.Role;
import com.nexbuy.model.User;
import com.nexbuy.repository.CartRepository;
import com.nexbuy.repository.OrderItemRepository;
import com.nexbuy.repository.OrderRepository;
import com.nexbuy.repository.ProductRepository;
import com.nexbuy.repository.UserRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/orders")
@CrossOrigin(origins = "*")
public class OrderController {
    
    @Autowired
    private OrderRepository orderRepository;
    
    @Autowired
    private OrderItemRepository orderItemRepository;
    
    @Autowired
    private CartRepository cartRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private ProductRepository productRepository;
    
    @PostMapping("/checkout")
    public ResponseEntity<?> checkout(@Valid @RequestBody CheckoutRequest request,
                                     Authentication authentication) {
        User user = getUserFromAuth(authentication);
        Cart cart = cartRepository.findByUser(user)
            .orElseThrow(() -> new RuntimeException("Cart is empty"));
        
        if (cart.getCartItems().isEmpty()) {
            return ResponseEntity.badRequest()
                .body(new MessageResponse("Cart is empty"));
        }
        
        double totalAmount = 0.0;
        Order order = Order.builder()
            .user(user)
            .shippingAddress(request.getShippingAddress())
            .status(OrderStatus.PENDING)
            .build();
        
        order = orderRepository.save(order);
        
        for (CartItem cartItem : cart.getCartItems()) {
            Product product = cartItem.getProduct();
            
            if (product.getStock() < cartItem.getQuantity()) {
                return ResponseEntity.badRequest()
                    .body(new MessageResponse("Insufficient stock for " + product.getName()));
            }
            
            OrderItem orderItem = OrderItem.builder()
                .order(order)
                .product(product)
                .quantity(cartItem.getQuantity())
                .price(product.getPrice())
                .build();
            
            orderItemRepository.save(orderItem);
            
            product.setStock(product.getStock() - cartItem.getQuantity());
            productRepository.save(product);
            
            totalAmount += product.getPrice() * cartItem.getQuantity();
        }
        
        order.setTotalAmount(totalAmount);
        orderRepository.save(order);
        
        cart.getCartItems().clear();
        cartRepository.save(cart);
        
        return ResponseEntity.ok(convertToDTO(order));
    }
    
    @GetMapping
    public ResponseEntity<List<OrderDTO>> getMyOrders(Authentication authentication) {
        User user = getUserFromAuth(authentication);
        List<OrderDTO> orders = orderRepository.findByUserOrderByCreatedAtDesc(user).stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
        
        return ResponseEntity.ok(orders);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<OrderDTO> getOrderById(@PathVariable Long id,
                                                 Authentication authentication) {
        User user = getUserFromAuth(authentication);
        Order order = orderRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Order not found"));
        
        if (!order.getUser().getId().equals(user.getId()) && 
            !user.getRole().equals(Role.ADMIN)) {
            return ResponseEntity.status(403).build();
        }
        
        return ResponseEntity.ok(convertToDTO(order));
    }
    
    private User getUserFromAuth(Authentication authentication) {
        String email = authentication.getName();
        return userRepository.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("User not found"));
    }
    
    private OrderDTO convertToDTO(Order order) {
        List<OrderItemDTO> items = order.getOrderItems().stream()
            .map(item -> OrderItemDTO.builder()
                .id(item.getId())
                .productId(item.getProduct().getId())
                .productName(item.getProduct().getName())
                .quantity(item.getQuantity())
                .price(item.getPrice())
                .build())
            .collect(Collectors.toList());
        
        return OrderDTO.builder()
            .id(order.getId())
            .userId(order.getUser().getId())
            .userName(order.getUser().getName())
            .totalAmount(order.getTotalAmount())
            .status(order.getStatus().name())
            .shippingAddress(order.getShippingAddress())
            .createdAt(order.getCreatedAt())
            .items(items)
            .build();
    }
}