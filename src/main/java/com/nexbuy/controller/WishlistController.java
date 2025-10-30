// WishlistController.java
// Location: src/main/java/com/nexbuy/controller/WishlistController.java

package com.nexbuy.controller;

import com.nexbuy.dto.MessageResponse;
import com.nexbuy.dto.ProductDTO;
import com.nexbuy.model.Product;
import com.nexbuy.model.User;
import com.nexbuy.model.Wishlist;
import com.nexbuy.repository.ProductRepository;
import com.nexbuy.repository.UserRepository;
import com.nexbuy.repository.WishlistRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/wishlist")
@CrossOrigin(origins = "*")
public class WishlistController {
    
    @Autowired
    private WishlistRepository wishlistRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private ProductRepository productRepository;
    
    @GetMapping
    public ResponseEntity<List<ProductDTO>> getWishlist(Authentication authentication) {
        User user = getUserFromAuth(authentication);
        
        List<ProductDTO> wishlistProducts = wishlistRepository.findByUser(user).stream()
            .map(wishlist -> convertToDTO(wishlist.getProduct()))
            .collect(Collectors.toList());
        
        return ResponseEntity.ok(wishlistProducts);
    }
    
    @PostMapping("/toggle/{productId}")
    public ResponseEntity<?> toggleWishlist(@PathVariable Long productId,
                                           Authentication authentication) {
        User user = getUserFromAuth(authentication);
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new RuntimeException("Product not found"));
        
        // Check if already in wishlist
        if (wishlistRepository.existsByUserAndProductId(user, productId)) {
            // Remove from wishlist
            Wishlist wishlist = wishlistRepository.findByUserAndProductId(user, productId)
                .orElseThrow(() -> new RuntimeException("Wishlist item not found"));
            wishlistRepository.delete(wishlist);
            
            return ResponseEntity.ok(new WishlistResponse("Removed from wishlist", false));
        } else {
            // Add to wishlist
            Wishlist wishlist = Wishlist.builder()
                .user(user)
                .product(product)
                .build();
            wishlistRepository.save(wishlist);
            
            return ResponseEntity.ok(new WishlistResponse("Added to wishlist", true));
        }
    }
    
    @GetMapping("/check/{productId}")
    public ResponseEntity<Boolean> checkWishlist(@PathVariable Long productId,
                                                 Authentication authentication) {
        User user = getUserFromAuth(authentication);
        boolean isInWishlist = wishlistRepository.existsByUserAndProductId(user, productId);
        return ResponseEntity.ok(isInWishlist);
    }
    
    @DeleteMapping("/{productId}")
    public ResponseEntity<?> removeFromWishlist(@PathVariable Long productId,
                                               Authentication authentication) {
        User user = getUserFromAuth(authentication);
        Wishlist wishlist = wishlistRepository.findByUserAndProductId(user, productId)
            .orElseThrow(() -> new RuntimeException("Product not in wishlist"));
        
        wishlistRepository.delete(wishlist);
        
        return ResponseEntity.ok(new MessageResponse("Removed from wishlist"));
    }
    
    private User getUserFromAuth(Authentication authentication) {
        String email = authentication.getName();
        return userRepository.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("User not found"));
    }
    
    private ProductDTO convertToDTO(Product product) {
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
    
    // Inner class for wishlist response
    static class WishlistResponse {
        private String message;
        private boolean isInWishlist;
        
        public WishlistResponse(String message, boolean isInWishlist) {
            this.message = message;
            this.isInWishlist = isInWishlist;
        }
        
        public String getMessage() { return message; }
        public boolean isInWishlist() { return isInWishlist; }
    }
}