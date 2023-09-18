package by.zzelberman.opensearch_consumer;

import com.google.gson.JsonParser;
import org.apache.http.HttpHost;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.opensearch.action.bulk.BulkRequest;
import org.opensearch.action.bulk.BulkResponse;
import org.opensearch.action.index.IndexRequest;
import org.opensearch.action.index.IndexResponse;
import org.opensearch.client.RequestOptions;
import org.opensearch.client.RestClient;
import org.opensearch.client.RestHighLevelClient;
import org.opensearch.client.indices.CreateIndexRequest;
import org.opensearch.client.indices.GetIndexRequest;
import org.opensearch.common.xcontent.XContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.Properties;

public class OpensearchKafkaConsumer {
    private static final String OPENSEARCH_INDEX = "wikimedia-change";
    private final static String CONSUMER_GROUP = "wikimedia-cgroup";
    private static final String TOPIC = "wikimedia-change-events";
    private static final Logger LOG = LoggerFactory.getLogger(OpensearchKafkaConsumer.class.getSimpleName());
    public static RestHighLevelClient getNonSecureOpensearchClient(String url) {
        URI cUri = URI.create(url);
        return new RestHighLevelClient(RestClient.builder(new HttpHost(cUri.getHost(), cUri.getPort())));
    }

    public static KafkaConsumer<String, String> getConsumer(final String topic) {
        Properties props = new Properties();
        props.setProperty("bootstrap.servers", "127.0.0.1:29092,127.0.0.1:29093,127.0.0.1:29094");
        props.setProperty("key.deserializer", StringDeserializer.class.getName());
        props.setProperty("value.deserializer", StringDeserializer.class.getName());
        props.setProperty("group.id", CONSUMER_GROUP);
        props.setProperty("auto.offset.reset", "earliest");
        props.setProperty(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");

        KafkaConsumer<String, String> consumer = new KafkaConsumer<String, String>(props);
        consumer.subscribe(Collections.singleton(topic));

        return consumer;
    }

    private static String extractId(final String messageBody) {
        return JsonParser.parseString(messageBody)
                .getAsJsonObject()
                .get("meta")
                .getAsJsonObject()
                .get("id")
                .getAsString();
    }
    public static void main(String[] args) {
        RestHighLevelClient osClient = getNonSecureOpensearchClient("http://localhost:9200");
        KafkaConsumer<String, String> consumer = getConsumer(TOPIC);

        try(osClient) {
            GetIndexRequest checkIndexReq = new GetIndexRequest(OPENSEARCH_INDEX);
            if (!osClient.indices().exists(checkIndexReq, RequestOptions.DEFAULT)) {
                CreateIndexRequest newIndexReq = new CreateIndexRequest(OPENSEARCH_INDEX);
                osClient.indices().create(newIndexReq, RequestOptions.DEFAULT);
                LOG.info("Index " + OPENSEARCH_INDEX + " has been created");
            } else {
                LOG.info("Index " + OPENSEARCH_INDEX + " exists, skipping index creation process");
            }

            //consuming messages into console
            while(true) {
                LOG.info("polling from infinite loop...");
                ConsumerRecords<String, String> recs = consumer.poll(Duration.ofSeconds(10));
                final BulkRequest bulkReq = new BulkRequest();
                recs.forEach(r -> {
                    LOG.info(r.topic() + " (P:" + r.partition() + "; O: " + r.offset() +
                            ") - " + r.key() + " : " + r.value());
                    try {
                        IndexRequest req = new IndexRequest(OPENSEARCH_INDEX)
                                .source(r.value(), XContentType.JSON)
                                .id(extractId(r.value()));
                        /* only for per=message creation strategy - not efficient
                        IndexResponse response = osClient.index(req, RequestOptions.DEFAULT);
                        LOG.info("Written to OpenSearch with id=" + response.getId());
                        */
                        bulkReq.add(req);
                    } catch(Exception e) {
                        LOG.error("Unable to complete bulk request creation while processing consumed messages", e);
                    }
                });

                if (bulkReq.numberOfActions() > 0) {
                    BulkResponse bulkResponse = osClient.bulk(bulkReq, RequestOptions.DEFAULT);
                    LOG.info("Message Bulk successfully inserted, " + bulkReq.numberOfActions() +
                            " messages were sent, failures: " + bulkResponse.hasFailures());
                    consumer.commitSync();
                    Thread.sleep(1000);
                }

            }

        } catch (IOException | InterruptedException e ) {
            throw new RuntimeException(e);
        }
    }
}
