// AdminController.java
// Location: src/main/java/com/nexbuy/controller/AdminController.java

package com.nexbuy.controller;

import com.nexbuy.dto.MessageResponse;
import com.nexbuy.dto.OrderDTO;
import com.nexbuy.dto.OrderItemDTO;
import com.nexbuy.dto.ProductDTO;
import com.nexbuy.dto.UserDTO;
import com.nexbuy.model.Order;
import com.nexbuy.model.OrderStatus;
import com.nexbuy.model.Product;
import com.nexbuy.model.Role;
import com.nexbuy.model.User;
import com.nexbuy.repository.OrderRepository;
import com.nexbuy.repository.ProductRepository;
import com.nexbuy.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {
    
    @Autowired
    private ProductRepository productRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private OrderRepository orderRepository;
    
    // Product Management
    @PostMapping("/products")
    public ResponseEntity<?> addProduct(@RequestBody ProductDTO productDTO) {
        Product product = Product.builder()
            .name(productDTO.getName())
            .description(productDTO.getDescription())
            .price(productDTO.getPrice())
            .imageUrl(productDTO.getImageUrl())
            .category(productDTO.getCategory())
            .stock(productDTO.getStock())
            .rating(0.0)
            .reviewCount(0)
            .build();
        
        product = productRepository.save(product);
        
        return ResponseEntity.ok(convertProductToDTO(product));
    }
    
    @PutMapping("/products/{id}")
    public ResponseEntity<?> updateProduct(@PathVariable Long id,
                                          @RequestBody ProductDTO productDTO) {
        Product product = productRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Product not found"));
        
        product.setName(productDTO.getName());
        product.setDescription(productDTO.getDescription());
        product.setPrice(productDTO.getPrice());
        product.setImageUrl(productDTO.getImageUrl());
        product.setCategory(productDTO.getCategory());
        product.setStock(productDTO.getStock());
        
        product = productRepository.save(product);
        
        return ResponseEntity.ok(convertProductToDTO(product));
    }
    
    @DeleteMapping("/products/{id}")
    public ResponseEntity<?> deleteProduct(@PathVariable Long id) {
        Product product = productRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Product not found"));
        
        productRepository.delete(product);
        
        return ResponseEntity.ok(new MessageResponse("Product deleted successfully"));
    }
    
    // User Management
    @GetMapping("/users")
    public ResponseEntity<List<UserDTO>> getAllUsers() {
        List<UserDTO> users = userRepository.findAll().stream()
            .map(this::convertUserToDTO)
            .collect(Collectors.toList());
        
        return ResponseEntity.ok(users);
    }
    
    @DeleteMapping("/users/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        if (user.getRole() == Role.ADMIN) {
            return ResponseEntity.badRequest()
                .body(new MessageResponse("Cannot delete admin user"));
        }
        
        userRepository.delete(user);
        
        return ResponseEntity.ok(new MessageResponse("User deleted successfully"));
    }
    
    // Order Management
    @GetMapping("/orders")
    public ResponseEntity<List<OrderDTO>> getAllOrders() {
        List<OrderDTO> orders = orderRepository.findAllByOrderByCreatedAtDesc().stream()
            .map(this::convertOrderToDTO)
            .collect(Collectors.toList());
        
        return ResponseEntity.ok(orders);
    }
    
    @PutMapping("/orders/{id}/status")
    public ResponseEntity<?> updateOrderStatus(@PathVariable Long id,
                                               @RequestParam String status) {
        Order order = orderRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Order not found"));
        
        try {
            OrderStatus orderStatus = OrderStatus.valueOf(status.toUpperCase());
            order.setStatus(orderStatus);
            orderRepository.save(order);
            
            return ResponseEntity.ok(convertOrderToDTO(order));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                .body(new MessageResponse("Invalid order status"));
        }
    }
    
    // Dashboard Stats
    @GetMapping("/stats")
    public ResponseEntity<?> getDashboardStats() {
        long totalUsers = userRepository.count();
        long totalProducts = productRepository.count();
        long totalOrders = orderRepository.count();
        
        double totalRevenue = orderRepository.findAll().stream()
            .mapToDouble(Order::getTotalAmount)
            .sum();
        
        return ResponseEntity.ok(new DashboardStats(totalUsers, totalProducts, totalOrders, totalRevenue));
    }
    
    private ProductDTO convertProductToDTO(Product product) {
        return ProductDTO.builder()
            .id(product.getId())
            .name(product.getName())
            .description(product.getDescription())
            .price(product.getPrice())
            .imageUrl(product.getImageUrl())
            .category(product.getCategory())
            .stock(product.getStock())
            .rating(product.getRating())
            .reviewCount(product.getReviewCount())
            .build();
    }
    
    private UserDTO convertUserToDTO(User user) {
        return UserDTO.builder()
            .id(user.getId())
            .name(user.getName())
            .email(user.getEmail())
            .phone(user.getPhone())
            .address(user.getAddress())
            .role(user.getRole().name())
            .createdAt(user.getCreatedAt())
            .build();
    }
    
    private OrderDTO convertOrderToDTO(Order order) {
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
    
    // Inner class for Dashboard Stats
    static class DashboardStats {
        private long totalUsers;
        private long totalProducts;
        private long totalOrders;
        private double totalRevenue;
        
        public DashboardStats(long totalUsers, long totalProducts, long totalOrders, double totalRevenue) {
            this.totalUsers = totalUsers;
            this.totalProducts = totalProducts;
            this.totalOrders = totalOrders;
            this.totalRevenue = totalRevenue;
        }
        
        public long getTotalUsers() { return totalUsers; }
        public long getTotalProducts() { return totalProducts; }
        public long getTotalOrders() { return totalOrders; }
        public double getTotalRevenue() { return totalRevenue; }
    }
}