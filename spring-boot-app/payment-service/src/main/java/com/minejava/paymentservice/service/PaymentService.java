package com.minejava.paymentservice.service;

import com.minejava.paymentservice.config.VNPayConfig;
import com.minejava.paymentservice.dto.OrderItemDto;
import com.minejava.paymentservice.dto.OrderPaymentItemsResponse;
import com.minejava.paymentservice.dto.PaymentRequest;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ResponseStatusException;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final VNPayConfig vnPayConfig;
    private final WebClient.Builder webClientBuilder;

    public String createPayment(PaymentRequest paymentRequest, HttpServletRequest request) throws UnsupportedEncodingException {
        getPendingOrderItems(paymentRequest.getOrderId());

        String vnp_Version = "2.1.0";
        String vnp_Command = "pay";
        String vnp_OrderInfo = paymentRequest.getOrderInfo();
        String orderType = "other";
        String vnp_TxnRef = paymentRequest.getOrderId();
        String vnp_IpAddr = vnPayConfig.getIpAddress(request);
        String vnp_TmnCode = vnPayConfig.getTmnCode();

        long amount = paymentRequest.getAmount() * 100;
        Map<String, String> vnp_Params = new HashMap<>();
        vnp_Params.put("vnp_Version", vnp_Version);
        vnp_Params.put("vnp_Command", vnp_Command);
        vnp_Params.put("vnp_TmnCode", vnp_TmnCode);
        vnp_Params.put("vnp_Amount", String.valueOf(amount));
        vnp_Params.put("vnp_CurrCode", "VND");
        vnp_Params.put("vnp_TxnRef", vnp_TxnRef);
        vnp_Params.put("vnp_OrderInfo", vnp_OrderInfo);
        vnp_Params.put("vnp_OrderType", orderType);
        vnp_Params.put("vnp_Locale", "vn");
        vnp_Params.put("vnp_ReturnUrl", vnPayConfig.vnp_ReturnUrl);
        vnp_Params.put("vnp_IpAddr", vnp_IpAddr);

        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        formatter.setTimeZone(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
        String vnp_CreateDate = formatter.format(new Date());
        vnp_Params.put("vnp_CreateDate", vnp_CreateDate);

        Calendar cld = Calendar.getInstance(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
        cld.add(Calendar.MINUTE, 15);
        String vnp_ExpireDate = formatter.format(cld.getTime());
        vnp_Params.put("vnp_ExpireDate", vnp_ExpireDate);

        List<String> fieldNames = new ArrayList<>(vnp_Params.keySet());
        Collections.sort(fieldNames);
        
        StringBuilder hashData = new StringBuilder();
        StringBuilder query = new StringBuilder();
        
        Iterator<String> itr = fieldNames.iterator();
        while (itr.hasNext()) {
            String fieldName = itr.next();
            String fieldValue = vnp_Params.get(fieldName);
            if ((fieldValue != null) && (fieldValue.length() > 0)) {
                // Build hash data
                hashData.append(fieldName);
                hashData.append('=');
                hashData.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString()));
                
                // Build query
                query.append(URLEncoder.encode(fieldName, StandardCharsets.US_ASCII.toString()));
                query.append('=');
                query.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString()));
                
                if (itr.hasNext()) {
                    query.append('&');
                    hashData.append('&');
                }
            }
        }
        
        String queryUrl = query.toString();
        String vnp_SecureHash = vnPayConfig.hmacSHA512(vnPayConfig.getHashSecret(), hashData.toString());
        queryUrl += "&vnp_SecureHash=" + vnp_SecureHash;
        
        String paymentUrl = vnPayConfig.vnp_PayUrl + "?" + queryUrl;
        log.info("VNPAY URL GENERATED: {}", paymentUrl);
        return paymentUrl;
    }

    public List<OrderItemDto> resolveItems(PaymentRequest paymentRequest) {
        return getPendingOrderItems(paymentRequest.getOrderId());
    }

    public List<OrderItemDto> getOrderItems(String orderId) {
        OrderPaymentItemsResponse response = getOrderPaymentItems(orderId);
        return response.getItems() != null ? response.getItems() : List.of();
    }

    public List<OrderItemDto> getPendingOrderItems(String orderId) {
        OrderPaymentItemsResponse response = getOrderPaymentItems(orderId);
        if (!"PENDING".equals(response.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Order is not pending payment");
        }
        return response.getItems() != null ? response.getItems() : List.of();
    }

    public boolean isValidVnPayCallback(Map<String, String> queryParams) {
        String receivedHash = queryParams.get("vnp_SecureHash");
        if (receivedHash == null || receivedHash.isBlank()) {
            return false;
        }

        List<String> fieldNames = new ArrayList<>(queryParams.keySet());
        fieldNames.remove("vnp_SecureHash");
        fieldNames.remove("vnp_SecureHashType");
        Collections.sort(fieldNames);

        List<String> encodedPairs = new ArrayList<>();
        for (String fieldName : fieldNames) {
            String fieldValue = queryParams.get(fieldName);
            if (fieldValue != null && !fieldValue.isBlank()) {
                encodedPairs.add(fieldName + "=" + URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII));
            }
        }

        String hashData = String.join("&", encodedPairs);
        String expectedHash = vnPayConfig.hmacSHA512(vnPayConfig.getHashSecret(), hashData.toString());
        return MessageDigest.isEqual(
                expectedHash.getBytes(StandardCharsets.UTF_8),
                receivedHash.getBytes(StandardCharsets.UTF_8));
    }

    private OrderPaymentItemsResponse getOrderPaymentItems(String orderId) {
        if (orderId == null || orderId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing order number");
        }

        try {
            OrderPaymentItemsResponse response = webClientBuilder.build().get()
                    .uri("http://order-service:8080/api/order/internal/{orderId}/payment-items", orderId)
                    .retrieve()
                    .bodyToMono(OrderPaymentItemsResponse.class)
                    .block();

            if (response == null || response.getItems() == null || response.getItems().isEmpty()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Order has no payment items");
            }
            return response;
        } catch (ResponseStatusException e) {
            throw e;
        } catch (WebClientResponseException.Forbidden e) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Order is not allowed for payment");
        } catch (WebClientResponseException.NotFound e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found");
        } catch (WebClientResponseException e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Order service rejected payment lookup");
        } catch (Exception e) {
            log.error("Unable to resolve order items for {}: {}", orderId, e.getMessage());
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Order service is unavailable");
        }
    }
}
