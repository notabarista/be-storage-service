package org.notabarista.storage.kafka.producer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.log4j.Log4j2;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.notabarista.kafka.MediaEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Component
@Log4j2
public class MediaEventProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public MediaEventProducer(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    public ListenableFuture<SendResult<String, String>> sendMediaEvent(MediaEvent mediaEvent) throws JsonProcessingException {
        String key = mediaEvent.getItemID();
        String value = objectMapper.writeValueAsString(mediaEvent);

        ProducerRecord<String, String> producerRecord = buildProducerRecord(kafkaTemplate.getDefaultTopic(), key, value);

        ListenableFuture<SendResult<String, String>> result = kafkaTemplate.send(producerRecord);
        result.addCallback(new ListenableFutureCallback<>() {
            @Override
            public void onFailure(Throwable ex) {
                handleFailure(key, value, ex);
            }

            @Override
            public void onSuccess(SendResult<String, String> result) {
                handleSuccess(key, value, result);
            }
        });

        return result;
    }

    private ProducerRecord<String, String> buildProducerRecord(String topic, String key, String value) {
        return new ProducerRecord<>(topic, null, key, value, List.of(new RecordHeader("event-source", "scanner".getBytes())));
    }

    public SendResult<String, String> sendLibraryEventSynchronous(MediaEvent mediaEvent) throws JsonProcessingException, ExecutionException, InterruptedException, TimeoutException {
        String key = mediaEvent.getItemID();
        String value = objectMapper.writeValueAsString(mediaEvent);
        SendResult<String, String> result = null;
        try {
            result = kafkaTemplate.sendDefault(key, value).get(1, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            log.error("Error sending the message with key {}, exception is {}", key, e.getMessage());
            throw e;
        }

        return result;
    }

    private void handleFailure(String key, String value, Throwable ex) {
        log.error("Error sending the message with key {}, exception is {}", key, ex.getMessage());
        try {
            throw ex;
        } catch (Throwable throwable) {
            log.error("Error in onFailure: {}", throwable.getMessage());
        }
    }

    private void handleSuccess(String key, String value, SendResult<String, String> result) {
        log.info("Message sent successfully for key : {} and the value is {} , partition is {}", key, value, result.getRecordMetadata().partition());
    }

}
