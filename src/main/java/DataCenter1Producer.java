import org.apache.kafka.clients.producer.*;

import java.util.Properties;
import java.util.Random;

public class DataCenter1Producer {

    public static void main(String[] args) throws InterruptedException {

        String topicName = "ProductA";
        String msg;

        Properties props = new Properties();
        props.put("bootstrap.servers", "SOURCE_DATA_CENTER_KAFKA_SERVER:PORT");
        props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");

        Producer<String, String> producer = new KafkaProducer<String, String>(props);
        try {
            for (int j = 0; j < 2; j++) {
                for (int i = 0; i < 2; i++) {
                    msg = "DC1: Message1_for P0 - " + new Random().nextInt();
                    producer.send(new ProducerRecord<String, String>(topicName, msg), new Callback() {
                                public void onCompletion(RecordMetadata recordMetadata, Exception e) {
                                    if (e == null) {
                                        System.out.println("Successfully received the details as: \n" +
                                                "Topic:" + recordMetadata.topic() + "  " +
                                                "Partition:" + recordMetadata.partition() + "  " +
                                                "Offset" + recordMetadata.offset() + "  " +
                                                "Timestamp" + recordMetadata.timestamp());
                                    } else {
                                        System.out.println("Can't produce,getting error");

                                    }
                                }
                            }
                    );
                }
                System.out.println("\n");
            }
        } catch (Exception ex) {
            System.out.println("Interrupted");
        } finally {
            producer.close();
        }

    }
}