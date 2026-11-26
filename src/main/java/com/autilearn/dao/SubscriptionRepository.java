package com.autilearn.dao;

import com.autilearn.model.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, Integer> {
    List<Subscription> findByParentId(Integer parentId);
    Optional<Subscription> findByRazorpayOrderId(String orderId);
    Optional<Subscription> findTopByParentIdAndPaymentStatusOrderByCreatedAtDesc(
        Integer parentId, String paymentStatus
    );
}
