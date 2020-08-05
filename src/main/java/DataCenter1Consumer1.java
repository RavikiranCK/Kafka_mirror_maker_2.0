import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;

import java.util.Arrays;
import java.util.Collections;
import java.util.Properties;

public class DataCenter1Consumer1 {
    public static void main(String[] args) throws Exception {
        System.out.println("This is Data Center 1 Consumer.....");

        String topicName = "ProductA";
        KafkaConsumer<String, String> consumer = null;

        String groupId = "group_1";
        Properties props = new Properties();
        props.put("bootstrap.servers", "SOURCE_DATA_CENTER_KAFKA_SERVER:PORT");
        props.put("group.id", groupId);
        props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        props.put("enable.auto.commit", "false");

        consumer = new KafkaConsumer<>(props);
        consumer.subscribe(Arrays.asList(topicName));

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
            System.out.println("Exception while consuming.");
            ex.printStackTrace();
        } finally {
            consumer.close();
        }
    }
}
