package com.nexbuy.repository;

import com.nexbuy.model.User;
import com.nexbuy.model.Wishlist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface WishlistRepository extends JpaRepository<Wishlist, Long> {
    List<Wishlist> findByUser(User user);
    Optional<Wishlist> findByUserAndProductId(User user, Long productId);
    Boolean existsByUserAndProductId(User user, Long productId);
}