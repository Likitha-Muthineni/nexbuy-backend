package com.nexbuy.controller;

import com.nexbuy.dto.MessageResponse;
import com.nexbuy.dto.ReviewDTO;
import com.nexbuy.dto.ReviewRequest;
import com.nexbuy.model.Product;
import com.nexbuy.model.Review;
import com.nexbuy.model.User;
import com.nexbuy.repository.ProductRepository;
import com.nexbuy.repository.ReviewRepository;
import com.nexbuy.repository.UserRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/reviews")
@CrossOrigin(origins = "*")
public class ReviewController {
    
    @Autowired
    private ReviewRepository reviewRepository;
    
    @Autowired
    private ProductRepository productRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @PostMapping("/product/{productId}")
    public ResponseEntity<?> addReview(@PathVariable Long productId,
                                      @Valid @RequestBody ReviewRequest request,
                                      Authentication authentication) {
        User user = getUserFromAuth(authentication);
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new RuntimeException("Product not found"));
        
        // Check if user already reviewed
        if (reviewRepository.existsByUserAndProduct(user, product)) {
            return ResponseEntity.badRequest()
                .body(new MessageResponse("You have already reviewed this product"));
        }
        
        Review review = Review.builder()
            .user(user)
            .product(product)
            .rating(request.getRating())
            .comment(request.getComment())
            .build();
        
        reviewRepository.save(review);
        
        // Update product rating
        updateProductRating(product);
        
        return ResponseEntity.ok(new MessageResponse("Review added successfully"));
    }
    
    @GetMapping("/product/{productId}")
    public ResponseEntity<List<ReviewDTO>> getProductReviews(@PathVariable Long productId) {
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new RuntimeException("Product not found"));
        
        List<ReviewDTO> reviews = reviewRepository.findByProductOrderByCreatedAtDesc(product).stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
        
        return ResponseEntity.ok(reviews);
    }
    
    @DeleteMapping("/{reviewId}")
    public ResponseEntity<?> deleteReview(@PathVariable Long reviewId,
                                         Authentication authentication) {
        User user = getUserFromAuth(authentication);
        Review review = reviewRepository.findById(reviewId)
            .orElseThrow(() -> new RuntimeException("Review not found"));
        
        if (!review.getUser().getId().equals(user.getId())) {
            return ResponseEntity.status(403)
                .body(new MessageResponse("You can only delete your own reviews"));
        }
        
        Product product = review.getProduct();
        reviewRepository.delete(review);
        
        // Update product rating
        updateProductRating(product);
        
        return ResponseEntity.ok(new MessageResponse("Review deleted successfully"));
    }
    
    private void updateProductRating(Product product) {
        List<Review> reviews = reviewRepository.findByProductOrderByCreatedAtDesc(product);
        
        if (reviews.isEmpty()) {
            product.setRating(0.0);
            product.setReviewCount(0);
        } else {
            double avgRating = reviews.stream()
                .mapToInt(Review::getRating)
                .average()
                .orElse(0.0);
            
            product.setRating(Math.round(avgRating * 10.0) / 10.0);
            product.setReviewCount(reviews.size());
        }
        
        productRepository.save(product);
    }
    
    private User getUserFromAuth(Authentication authentication) {
        String email = authentication.getName();
        return userRepository.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("User not found"));
    }
    
    private ReviewDTO convertToDTO(Review review) {
        return ReviewDTO.builder()
            .id(review.getId())
            .userId(review.getUser().getId())
            .userName(review.getUser().getName())
            .productId(review.getProduct().getId())
            .rating(review.getRating())
            .comment(review.getComment())
            .createdAt(review.getCreatedAt())
            .build();
    }
}