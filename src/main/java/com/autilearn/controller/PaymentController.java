package com.autilearn.controller;

import com.autilearn.dao.SubscriptionRepository;
import com.autilearn.model.Subscription;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/api/payment")
@CrossOrigin(origins = "*")
public class PaymentController {

    @Autowired private SubscriptionRepository subscriptionRepo;

    @Value("${razorpay.key.id}")
    private String razorpayKeyId;

    @Value("${razorpay.key.secret}")
    private String razorpayKeySecret;

    // ── CREATE ORDER ──────────────────────────────────────────
    @PostMapping("/create-order")
    public ResponseEntity<Map<String, Object>> createOrder(@RequestBody Map<String, Object> body) {
        Map<String, Object> res = new HashMap<>();
        try {
            Integer parentId = toInt(body.get("parentId"));
            String  plan     = (String) body.get("plan");
            int     amount   = toInt(body.get("amount"));

            boolean isDemo = razorpayKeyId == null
                          || razorpayKeyId.equals("YOUR_RAZORPAY_KEY_ID")
                          || razorpayKeyId.isEmpty();

            String razorpayOrderId;

            if (!isDemo) {
                // Live Razorpay
                com.razorpay.RazorpayClient client =
                        new com.razorpay.RazorpayClient(razorpayKeyId, razorpayKeySecret);
                org.json.JSONObject req = new org.json.JSONObject();
                req.put("amount",   amount * 100);
                req.put("currency", "INR");
                req.put("receipt",  "order_" + System.currentTimeMillis());
                com.razorpay.Order order = client.orders.create(req);
                razorpayOrderId = order.get("id");
            } else {
                // Demo mode — generate fake order id
                razorpayOrderId = "demo_order_" + System.currentTimeMillis();
            }

            Subscription sub = new Subscription();
            sub.setParentId(parentId);
            sub.setPlanName(plan);
            sub.setAmountPaid(new BigDecimal(amount));
            sub.setCurrency("INR");
            sub.setPaymentStatus("pending");
            sub.setRazorpayOrderId(razorpayOrderId);
            subscriptionRepo.save(sub);

            res.put("success",         true);
            res.put("razorpayOrderId", razorpayOrderId);
            res.put("razorpayKeyId",   isDemo ? "demo" : razorpayKeyId);
            res.put("amount",          amount);
            res.put("currency",        "INR");
            res.put("demoMode",        isDemo);
            return ResponseEntity.ok(res);

        } catch (Exception e) {
            e.printStackTrace();
            res.put("success", false);
            res.put("message", "Payment gateway error: " + e.getMessage());
            return ResponseEntity.status(500).body(res);
        }
    }

    // ── VERIFY PAYMENT ────────────────────────────────────────
    @PostMapping("/verify")
    public ResponseEntity<Map<String, Object>> verifyPayment(@RequestBody Map<String, Object> body) {
        Map<String, Object> res = new HashMap<>();
        try {
            String  paymentId = (String)  body.get("razorpayPaymentId");
            String  orderId   = (String)  body.get("razorpayOrderId");
            String  signature = (String)  body.get("razorpaySignature");
            String  plan      = (String)  body.get("plan");
            boolean demoMode  = Boolean.TRUE.equals(body.get("demoMode"))
                             || (orderId != null && orderId.startsWith("demo_"));

            if (!demoMode) {
                org.json.JSONObject attrs = new org.json.JSONObject();
                attrs.put("razorpay_order_id",   orderId);
                attrs.put("razorpay_payment_id", paymentId);
                attrs.put("razorpay_signature",  signature);
                boolean valid = com.razorpay.Utils.verifyPaymentSignature(attrs, razorpayKeySecret);
                if (!valid) {
                    res.put("success", false);
                    res.put("message", "Payment verification failed. Contact support.");
                    return ResponseEntity.status(400).body(res);
                }
            }

            Optional<Subscription> opt = subscriptionRepo.findByRazorpayOrderId(orderId);
            if (opt.isPresent()) {
                Subscription sub = opt.get();
                sub.setPaymentStatus("success");
                sub.setRazorpayPaymentId(demoMode ? "demo_pay_" + System.currentTimeMillis() : paymentId);
                sub.setStartDate(LocalDate.now());
                sub.setEndDate("yearly".equals(plan)
                        ? LocalDate.now().plusYears(1)
                        : LocalDate.now().plusMonths(1));
                subscriptionRepo.save(sub);
            }

            res.put("success", true);
            res.put("message", "Payment verified! Your plan is now active.");
            return ResponseEntity.ok(res);

        } catch (Exception e) {
            e.printStackTrace();
            res.put("success", false);
            res.put("message", "Verification error: " + e.getMessage());
            return ResponseEntity.status(500).body(res);
        }
    }

    // ── SUBSCRIPTION STATUS ───────────────────────────────────
    @GetMapping("/status/{parentId}")
    public ResponseEntity<Map<String, Object>> getStatus(@PathVariable Integer parentId) {
        Map<String, Object> res = new HashMap<>();
        Optional<Subscription> opt = subscriptionRepo
                .findTopByParentIdAndPaymentStatusOrderByCreatedAtDesc(parentId, "success");
        if (opt.isPresent()) {
            Subscription sub = opt.get();
            boolean isActive = sub.getEndDate() != null && !sub.getEndDate().isBefore(LocalDate.now());
            res.put("success",  true);
            res.put("plan",     sub.getPlanName());
            res.put("isActive", isActive);
            res.put("endDate",  sub.getEndDate());
        } else {
            res.put("success",  true);
            res.put("plan",     "free");
            res.put("isActive", true);
            res.put("endDate",  null);
        }
        return ResponseEntity.ok(res);
    }

    private Integer toInt(Object val) {
        if (val == null) return 0;
        if (val instanceof Integer) return (Integer) val;
        try { return Integer.parseInt(val.toString()); } catch (NumberFormatException e) { return 0; }
    }
}
