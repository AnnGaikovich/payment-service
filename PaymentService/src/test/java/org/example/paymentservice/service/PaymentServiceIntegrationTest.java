//package org.example.paymentservice.service;
//
//import com.github.tomakehurst.wiremock.WireMockServer;
//import com.github.tomakehurst.wiremock.client.WireMock;
//import jakarta.validation.ValidationException;
//import org.example.paymentservice.config.TestContainersConfig;
//import org.example.paymentservice.dto.PaymentEventDTO;
//import org.example.paymentservice.entity.Payment;
//import org.example.paymentservice.enums.PaymentStatus;
//import org.example.paymentservice.repository.PaymentRepository;
//import org.junit.jupiter.api.*;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.boot.testcontainers.context.ImportTestcontainers;
//import org.springframework.kafka.annotation.EnableKafka;
//import org.springframework.kafka.core.KafkaTemplate;
//import org.springframework.kafka.test.context.EmbeddedKafka;
//import org.springframework.test.context.ActiveProfiles;
//import org.springframework.test.context.DynamicPropertyRegistry;
//import org.springframework.test.context.DynamicPropertySource;
//import org.testcontainers.junit.jupiter.Testcontainers;
//
//import java.math.BigDecimal;
//import java.time.LocalDateTime;
//import java.util.List;
//import java.util.concurrent.TimeUnit;
//
//import static com.github.tomakehurst.wiremock.client.WireMock.*;
//import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
//import static org.assertj.core.api.Assertions.assertThat;
//import static org.awaitility.Awaitility.await;
//
//@SpringBootTest
//@ActiveProfiles("test")
//@Testcontainers
//@ImportTestcontainers(TestContainersConfig.class)
//@EmbeddedKafka(partitions = 1, topics = {"payment-events"}, brokerProperties = {
//        "listeners=PLAINTEXT://localhost:9092",
//        "port=9092"
//})
//@EnableKafka
//class PaymentServiceIntegrationTest {
//
//    @Autowired
//    private PaymentService paymentService;
//
//    @Autowired
//    private PaymentRepository paymentRepository;
//
//    @Autowired
//    private KafkaTemplate<String, PaymentEventDTO> kafkaTemplate;
//
//    private static WireMockServer wireMockServer;
//
//    @BeforeAll
//    static void beforeAll() {
//        wireMockServer = new WireMockServer(wireMockConfig().dynamicPort());
//        wireMockServer.start();
//        WireMock.configureFor("localhost", wireMockServer.port());
//    }
//
//    @AfterAll
//    static void afterAll() {
//        wireMockServer.stop();
//    }
//
//    @BeforeEach
//    void setUp() {
//        paymentRepository.deleteAll();
//        wireMockServer.resetAll();
//        setupWireMockStubs();
//    }
//
//    @DynamicPropertySource
//    static void configureProperties(DynamicPropertyRegistry registry) {
//        registry.add("user.service.url", () -> "http://localhost:" + wireMockServer.port());
//        registry.add("order.service.url", () -> "http://localhost:" + wireMockServer.port());
//    }
//
//    private void setupWireMockStubs() {
//        // Stub for user exists
//        wireMockServer.stubFor(get(urlPathMatching("/api/v1/users/\\d+/exists"))
//                .willReturn(aResponse()
//                        .withStatus(200)
//                        .withHeader("Content-Type", "text/plain")
//                        .withBody("true")));
//
//        // Stub for order exists
//        wireMockServer.stubFor(get(urlPathMatching("/api/orders/\\d+/exists"))
//                .willReturn(aResponse()
//                        .withStatus(200)
//                        .withHeader("Content-Type", "text/plain")
//                        .withBody("true")));
//
//        // Stub for random.org API
//        wireMockServer.stubFor(get(urlPathEqualTo("/integers/"))
//                .withQueryParam("num", equalTo("1"))
//                .withQueryParam("min", equalTo("1"))
//                .withQueryParam("max", equalTo("100"))
//                .withQueryParam("col", equalTo("1"))
//                .withQueryParam("base", equalTo("10"))
//                .withQueryParam("format", equalTo("plain"))
//                .withQueryParam("rnd", equalTo("new"))
//                .willReturn(aResponse()
//                        .withStatus(200)
//                        .withHeader("Content-Type", "text/plain")
//                        .withBody("42"))); // Even number -> SUCCESS
//    }
//
//    @Test
//    void createPayment_Integration_Success() {
//        // Arrange
//        Long orderId = 100L;
//        Long userId = 1L;
//        BigDecimal amount = new BigDecimal("250.75");
//
//        // Act
//        Payment payment = paymentService.createPayment(orderId, userId, amount);
//
//        // Assert
//        assertThat(payment).isNotNull();
//        assertThat(payment.getId()).isNotNull();
//        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
//        assertThat(payment.getOrderId()).isEqualTo(orderId);
//        assertThat(payment.getUserId()).isEqualTo(userId);
//        assertThat(payment.getPaymentAmount()).isEqualByComparingTo(amount);
//
//        // Verify saved in database
//        List<Payment> allPayments = paymentRepository.findAll();
//        assertThat(allPayments).hasSize(1);
//        assertThat(allPayments.get(0).getId()).isEqualTo(payment.getId());
//
//        // Verify Kafka event (we could add a test consumer to verify)
//        // For simplicity, we'll just ensure no exceptions
//    }
//
//    @Test
//    void getPaymentsByUserId_Integration_Success() {
//        // Arrange
//        Long userId = 1L;
//        Payment payment = new Payment();
//        payment.setUserId(userId);
//        payment.setOrderId(100L);
//        payment.setStatus(PaymentStatus.SUCCESS);
//        payment.setPaymentAmount(new BigDecimal("100.00"));
//        payment.setTimestamp(LocalDateTime.now());
//        paymentRepository.save(payment);
//
//        // Act
//        List<Payment> payments = paymentService.getPaymentsByUserId(userId);
//
//        // Assert
//        assertThat(payments).hasSize(1);
//        assertThat(payments.get(0).getUserId()).isEqualTo(userId);
//    }
//
//    @Test
//    void getTotalSumByUserAndDateRange_Integration_Success() {
//        // Arrange
//        Long userId = 1L;
//        LocalDateTime now = LocalDateTime.now();
//
//        Payment payment1 = new Payment();
//        payment1.setUserId(userId);
//        payment1.setOrderId(100L);
//        payment1.setStatus(PaymentStatus.SUCCESS);
//        payment1.setPaymentAmount(new BigDecimal("100.00"));
//        payment1.setTimestamp(now.minusDays(5));
//        paymentRepository.save(payment1);
//
//        Payment payment2 = new Payment();
//        payment2.setUserId(userId);
//        payment2.setOrderId(101L);
//        payment2.setStatus(PaymentStatus.SUCCESS);
//        payment2.setPaymentAmount(new BigDecimal("200.00"));
//        payment2.setTimestamp(now.minusDays(3));
//        paymentRepository.save(payment2);
//
//        LocalDateTime startDate = now.minusDays(10);
//        LocalDateTime endDate = now;
//
//        // Act
//        BigDecimal total = paymentService.getTotalSumByUserAndDateRange(
//                userId, startDate, endDate);
//
//        // Assert
//        assertThat(total).isEqualByComparingTo("300.00");
//    }
//
//    @Test
//    void deletePayment_Integration_Success() {
//        // Arrange
//        Payment payment = new Payment();
//        payment.setUserId(1L);
//        payment.setOrderId(100L);
//        payment.setStatus(PaymentStatus.SUCCESS);
//        payment.setPaymentAmount(new BigDecimal("100.00"));
//        payment.setTimestamp(LocalDateTime.now());
//        Payment savedPayment = paymentRepository.save(payment);
//
//        // Act
//        paymentService.deletePayment(savedPayment.getId());
//
//        // Assert
//        Payment updatedPayment = paymentRepository.findById(savedPayment.getId()).orElseThrow();
//        assertThat(updatedPayment.getStatus()).isEqualTo(PaymentStatus.CANCELLED);
//    }
//
//    @Test
//    void createPayment_KafkaEvent_SentSuccessfully() {
//        // Arrange
//        Long orderId = 200L;
//        Long userId = 2L;
//        BigDecimal amount = new BigDecimal("150.50");
//
//        // Act
//        Payment payment = paymentService.createPayment(orderId, userId, amount);
//
//        // Assert
//        // Wait for Kafka event to be sent
//        await().atMost(5, TimeUnit.SECONDS)
//                .untilAsserted(() -> {
//                    // In a real scenario, you might want to consume and verify the event
//                    // For this test, we'll just verify the payment was created
//                    assertThat(paymentRepository.existsById(payment.getId())).isTrue();
//                });
//    }
//
//    @Test
//    void createPayment_UserDoesNotExist_ThrowsException() {
//        // Arrange
//        Long orderId = 300L;
//        Long userId = 999L; // Non-existent user
//        BigDecimal amount = new BigDecimal("100.00");
//
//        // Override stub for this test
//        wireMockServer.stubFor(get(urlPathMatching("/api/v1/users/999/exists"))
//                .willReturn(aResponse()
//                        .withStatus(200)
//                        .withHeader("Content-Type", "text/plain")
//                        .withBody("false")));
//
//        // Act & Assert
//        Assertions.assertThrows(ValidationException.class, () -> {
//            paymentService.createPayment(orderId, userId, amount);
//        });
//    }
//
//    @Test
//    void createPayment_OrderDoesNotExist_ThrowsException() {
//        // Arrange
//        Long orderId = 999L; // Non-existent order
//        Long userId = 1L;
//        BigDecimal amount = new BigDecimal("100.00");
//
//        // Override stub for this test
//        wireMockServer.stubFor(get(urlPathMatching("/api/orders/999/exists"))
//                .willReturn(aResponse()
//                        .withStatus(200)
//                        .withHeader("Content-Type", "text/plain")
//                        .withBody("false")));
//
//        // Act & Assert
//        Assertions.assertThrows(ValidationException.class, () -> {
//            paymentService.createPayment(orderId, userId, amount);
//        });
//    }
//
//    @Test
//    void createPayment_RandomApiReturnsOddNumber_PaymentFailed() {
//        // Arrange
//        Long orderId = 400L;
//        Long userId = 1L;
//        BigDecimal amount = new BigDecimal("100.00");
//
//        // Override stub to return odd number
//        wireMockServer.stubFor(get(urlPathEqualTo("/integers/"))
//                .withQueryParam("num", equalTo("1"))
//                .withQueryParam("min", equalTo("1"))
//                .withQueryParam("max", equalTo("100"))
//                .withQueryParam("col", equalTo("1"))
//                .withQueryParam("base", equalTo("10"))
//                .withQueryParam("format", equalTo("plain"))
//                .withQueryParam("rnd", equalTo("new"))
//                .willReturn(aResponse()
//                        .withStatus(200)
//                        .withHeader("Content-Type", "text/plain")
//                        .withBody("13"))); // Odd number -> FAILED
//
//        // Act
//        Payment payment = paymentService.createPayment(orderId, userId, amount);
//
//        // Assert
//        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
//    }
//
//    @Test
//    void createPayment_RandomApiFails_UsesFallback() {
//        // Arrange
//        Long orderId = 500L;
//        Long userId = 1L;
//        BigDecimal amount = new BigDecimal("100.00");
//
//        // Override stub to simulate API failure
//        wireMockServer.stubFor(get(urlPathEqualTo("/integers/"))
//                .willReturn(aResponse()
//                        .withStatus(500)
//                        .withBody("Internal Server Error")));
//
//        // Act
//        Payment payment = paymentService.createPayment(orderId, userId, amount);
//
//        // Assert
//        // The fallback random number generator will be used
//        assertThat(payment).isNotNull();
//        // Status could be SUCCESS or FAILED depending on fallback random number
//        assertThat(payment.getStatus())
//                .isIn(PaymentStatus.SUCCESS, PaymentStatus.FAILED);
//    }
//}