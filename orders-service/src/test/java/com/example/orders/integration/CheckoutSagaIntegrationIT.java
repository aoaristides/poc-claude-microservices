package com.example.orders.integration;

import com.example.orders.application.command.StartCheckoutCommand;
import com.example.orders.domain.model.ClientId;
import com.example.orders.domain.model.Money;
import com.example.orders.domain.model.OrderId;
import com.example.orders.domain.model.OrderItem;
import com.example.orders.domain.model.Sku;
import com.example.orders.domain.port.in.RecoverTimedOutSagaUseCase;
import com.example.orders.domain.port.in.StartCheckoutUseCase;
import com.example.orders.domain.port.out.SagaRepository;
import com.example.orders.domain.saga.SagaState;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.ConfluentKafkaContainer;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Teste de integração ponta a ponta com Postgres + Kafka reais (Testcontainers).
 *
 * <p>Exercita o caminho completo da máquina de saga: outbox -> relay -> Kafka ->
 * listener -> dedup (inbox) -> avanço da saga. Cobre o caminho feliz, a idempotência
 * de resposta duplicada e o timeout (compensação).
 *
 * <p>Roda em {@code mvn verify} (sufixo IT / failsafe), não em {@code mvn test}.
 */
@SpringBootTest
@Testcontainers
class CheckoutSagaIntegrationIT {

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>(DockerImageName.parse("postgres:16"));

    @Container
    static final ConfluentKafkaContainer KAFKA =
            new ConfluentKafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.7.1"));

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.kafka.bootstrap-servers", KAFKA::getBootstrapServers);
        // relay mais rápido para o teste; timeout grande para o scheduler não interferir
        registry.add("orders.outbox.relay-delay-ms", () -> "200");
        registry.add("orders.saga.timeout-ms", () -> "600000");
        registry.add("orders.saga.timeout-check-delay-ms", () -> "600000");
    }

    @Autowired
    StartCheckoutUseCase startCheckout;

    @Autowired
    RecoverTimedOutSagaUseCase recoverTimedOut;

    @Autowired
    SagaRepository sagaRepository;

    @Autowired
    KafkaTemplate<String, String> kafkaTemplate;

    @Value("${orders.topics.inventory-commands}")
    String inventoryCommands;

    @Value("${orders.topics.payment-commands}")
    String paymentCommands;

    @Value("${orders.topics.inventory-replies}")
    String inventoryReplies;

    @Test
    void fluxo_avanca_de_reserva_para_pagamento_e_resposta_duplicada_e_idempotente() throws Exception {
        OrderId orderId = startCheckout();

        // 1. O outbox publicou ReserveStock no tópico de comandos de estoque
        await().atMost(Duration.ofSeconds(30)).untilAsserted(() ->
                assertThat(commandTypes(inventoryCommands, orderId)).contains("ReserveStock"));

        // 2. Participante responde StockReserved
        String messageId = sendReply(inventoryReplies, orderId, "StockReserved", UUID.randomUUID().toString());

        // 3. A saga avançou e publicou AuthorizePayment
        await().atMost(Duration.ofSeconds(30)).untilAsserted(() ->
                assertThat(commandTypes(paymentCommands, orderId)).contains("AuthorizePayment"));

        assertThat(sagaRepository.findByOrderId(orderId)).get()
                .extracting(s -> s.state()).isEqualTo(SagaState.AWAITING_PAYMENT);

        // 4. Idempotência: reenviar a MESMA mensagem (mesmo messageId) não gera novo comando
        sendReply(inventoryReplies, orderId, "StockReserved", messageId);
        Thread.sleep(2000); // janela para um eventual (e indevido) reprocessamento

        long authorizeCount = commandTypes(paymentCommands, orderId).stream()
                .filter("AuthorizePayment"::equals).count();
        assertThat(authorizeCount).isEqualTo(1);
    }

    @Test
    void timeout_dispara_compensacao_de_estoque() {
        OrderId orderId = startCheckout();

        // garante que a saga foi persistida (em AWAITING_STOCK) e o comando saiu
        await().atMost(Duration.ofSeconds(30)).untilAsserted(() ->
                assertThat(commandTypes(inventoryCommands, orderId)).contains("ReserveStock"));

        // dispara a recuperação por timeout diretamente (sem depender do relógio)
        recoverTimedOut.execute(orderId);

        // compensação defensiva: libera a reserva
        await().atMost(Duration.ofSeconds(30)).untilAsserted(() ->
                assertThat(commandTypes(inventoryCommands, orderId)).contains("ReleaseReservation"));

        assertThat(sagaRepository.findByOrderId(orderId)).get()
                .extracting(s -> s.state()).isEqualTo(SagaState.COMPENSATING_STOCK);
    }

    // ----------------------------- helpers -----------------------------

    private OrderId startCheckout() {
        var command = new StartCheckoutCommand(
                new ClientId(UUID.randomUUID()),
                List.of(new OrderItem(new Sku("SKU-1"), 2, Money.of(new BigDecimal("49.90"), "BRL"))));
        return startCheckout.execute(command);
    }

    /** Reenvia/Envia uma resposta de participante com headers messageId e eventType. */
    private String sendReply(String topic, OrderId orderId, String eventType, String messageId) throws Exception {
        var payload = "{\"orderId\":\"" + orderId.asString() + "\"}";
        var record = new ProducerRecord<String, String>(topic, orderId.asString(), payload);
        record.headers().add("messageId", messageId.getBytes(StandardCharsets.UTF_8));
        record.headers().add("eventType", eventType.getBytes(StandardCharsets.UTF_8));
        kafkaTemplate.send(record).get();
        return messageId;
    }

    /** Lê o tópico desde o início e devolve os eventType dos comandos da chave (orderId) dada. */
    private List<String> commandTypes(String topic, OrderId orderId) {
        var props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA.getBootstrapServers());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "it-" + UUID.randomUUID());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);

        var result = new ArrayList<String>();
        try (var consumer = new KafkaConsumer<String, String>(props)) {
            consumer.subscribe(List.of(topic));
            for (int i = 0; i < 5; i++) {
                var records = consumer.poll(Duration.ofMillis(500));
                for (ConsumerRecord<String, String> r : records) {
                    if (orderId.asString().equals(r.key())) {
                        result.add(eventType(r));
                    }
                }
            }
        }
        return result;
    }

    private static String eventType(ConsumerRecord<String, String> record) {
        var header = record.headers().lastHeader("eventType");
        return header == null ? null : new String(header.value(), StandardCharsets.UTF_8);
    }
}
