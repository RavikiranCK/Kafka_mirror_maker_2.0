## Kafka_mirror_maker_2.0 Example

The main idea about this small project is to demonstrate the Active/Active Datacenter Replication using the MirrorMaker 2.0.  
Some of the key benefits/highlights of MirrorMaker 2.0 are:
  - Kafka connectors architecture
  - Checkpoints, offsets and topics replicated
  - Topics have their configurations copied
  - SSL/SASL support
  - Multiple deployment modes
    - MM Kafka dedicated cluster
    - Connect standalone
    - Connectors deployed in a distributed connect cluster

<br>
#### Active/Active Datacenter Replication:

Create a topic named **ProductA** in Source data center.
```sh
./kafka-topics.sh --create --zookeeper localhost:2181 --replication-factor 1 --partitions 1 --topic ProductA
```

##### Configuration File for MM2:
```sh
# Kafka datacenters.
clusters = source, target
source.bootstrap.servers = localhost:9093
target.bootstrap.servers = localhost:9095
#Enable active to active replication
source->target.enabled = true
target->source.enabled = true 
# Mirror maker configurations.
offset-syncs.topic.replication.factor = 1
heartbeats.topic.replication.factor = 1
checkpoints.topic.replication.factor = 1
topics = .*
groups = .*
tasks.max = 1
replication.factor = 1
refresh.topics.enabled = true
sync.topic.configs.enabled = true
refresh.topics.interval.seconds = 10
topics.blacklist = .*[\-\.]internal, .*\.replica, __consumer_offsets
groups.blacklist = console-consumer-.*, connect-.*, __.*

# Enable heartbeats and checkpoints.
source->target.emit.heartbeats.enabled = true 
source->target.emit.checkpoints.enabled = true 
```

##### Run the following command to start mirror maker:
```sh
./bin/connect-mirror-maker.sh ./config/active-to-active-mm2.properties
```

**Check the topics on the source and target data centers:**
<img width="750" alt="TopicRep" src="https://user-images.githubusercontent.com/7210287/89422803-6978c000-d753-11ea-8a2f-27325437f799.PNG">

_All the configurations files required to set up the kafka servers are present under config folder_

The project makes use of the following dependencies to implement Producer and Consumers services:
```xml
   <dependency>
    	<groupId>org.apache.kafka</groupId>
        <artifactId>kafka-clients</artifactId>
        <version>2.5.0</version>
    </dependency>

    <dependency>
       <groupId>org.apache.kafka</groupId>
       <artifactId>connect-mirror-client</artifactId>
       <version>2.5.0</version>
    </dependency>
```
 - The class _```DataCenter1Producer.java```_ inputs the messages to topic _ProductA_ in Data center 1 which is running on port 9093
 - The class _```DataCenter1Consumer1.java```_ is running a consumer in Data center 1; listening on port **9093** and it consumes the messages from the topic _**ProductA**_ and committing the offsets synchronously after processing each message.
 - The class _```DataCenter2Consumer1.java```_ is running a consumer in Data center 2; listening on port **9095** and it consumes the messages from topic _**source.ProductA**_ which is the replicated topic of source data center. 

To handle the Automatic failover, Kafka’s new **```RemoteClusterUtils.java```** utility class is used to find the local offsets corresponding to the latest checkpoint from a specific source consumer group. 
 
```java
Map<TopicPartition, OffsetAndMetadata> translateOffsets(Map<String, Object> properties,
            String targetClusterAlias, String consumerGroupId, Duration timeout)
```

The project can be extended to manage the multiple consumers failover scenarios and mismatched partitions in source and target data centers.  
<br>
>**Future work**: So far the failover handling is done for a single consumer running in the source data center. A similar approach can be implemented to handle the failover scenarios in target data center as well.  
Using the different APIs that are available in  **```RemoteClusterUtils.java```** utility, you can automatically detect the failures in source and target data centers, which can be helpful to manage real time disaster recoveries across data centers. 

<br>

###### References:
 - https://cwiki.apache.org/confluence/display/KAFKA/KIP-382%3A+MirrorMaker+2.0 
 - https://dzone.com/articles/mirror-maker-v20
 - https://strimzi.io/blog/2020/03/30/introducing-mirrormaker2/
