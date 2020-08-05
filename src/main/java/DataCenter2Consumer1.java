import java.time.Duration;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.connect.mirror.RemoteClusterUtils;

import java.util.*;
import java.util.stream.Collectors;

public class DataCenter2Consumer1 {
    public static void main(String[] args) throws Exception {
        System.out.println("This is Data Center 2 Consumer.....");

        KafkaConsumer<String, String> consumer = null;
        String remoteDataCenterAlias = "source";
        String topicName = "source.ProductA";
        String groupId = "group_1";

        //A map to store target Kafka server details
        Map<String, Object> remoteDCPropertiesMap = new HashMap<>();
        remoteDCPropertiesMap.put("bootstrap.servers", "TARGET_DATA_CENTER_KAFKA_SERVER:PORT");
        remoteDCPropertiesMap.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        remoteDCPropertiesMap.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        remoteDCPropertiesMap.put("group.id", groupId);

        consumer = new KafkaConsumer<>(remoteDCPropertiesMap);

        //Get the partitions of the replicated topic in target server
        List<TopicPartition> partitions = consumer.partitionsFor(topicName)
                .stream().map(part -> {
                    TopicPartition tp = new TopicPartition(part.topic(), part.partition());
                    return tp;
                }).collect(Collectors.toList());

        consumer.assign(partitions);


        //Translate a remote consumer group's offsets into corresponding local offsets
        Map<TopicPartition, OffsetAndMetadata> newOffsets = RemoteClusterUtils.translateOffsets(remoteDCPropertiesMap
                , remoteDataCenterAlias, groupId, Duration.ofMillis(1000));

        for (Map.Entry<TopicPartition, OffsetAndMetadata> entry : newOffsets.entrySet()) {
            TopicPartition topicPartition = entry.getKey();
            OffsetAndMetadata offsetAndMetadata = entry.getValue();
            System.out.println("Topic: " + entry.getKey().topic() + ", Offset: " + entry.getValue().offset());
            consumer.seek(topicPartition, offsetAndMetadata);
        }

        try {
            while (true) {
                ConsumerRecords<String, String> records = consumer.poll(1000);
                for (ConsumerRecord<String, String> record : records) {
                    System.out.println("Topic:" + record.topic() +
                            " Partition:" + record.partition() +
                            " Offset:" + record.offset() + " Key: " + record.key() + " Value: " + record.value());

                    consumer.commitSync(Collections.singletonMap(new TopicPartition(record.topic(), record.partition())
                            , new OffsetAndMetadata(record.offset() + 1)));
                    Thread.sleep(100);
                }
            }
        } catch (Exception ex) {
            System.out.println("Exception.");
            ex.printStackTrace();
        } finally {
            consumer.close();
        }
    }
}
