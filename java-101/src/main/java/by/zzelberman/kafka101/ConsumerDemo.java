package by.zzelberman.kafka101;

import java.time.Duration;
import java.util.Arrays;
import java.util.Properties;

import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConsumerDemo {
	private static final Logger LOG = LoggerFactory.getLogger(ConsumerDemo.class);
	
	public static void main(String[] args) {
		LOG.info("From ConsumerDemo.main(args)");
		final String topic = "java-demo";
		final String group = "java-first-cgroup";
		
		Properties props = new Properties();
		props.setProperty("bootstrap.servers", "127.0.0.1:29092,127.0.0.1:29093,127.0.0.1:29094");
		props.setProperty("key.deserializer", StringDeserializer.class.getName());
		props.setProperty("value.deserializer", StringDeserializer.class.getName());
		
		props.setProperty("group.id", group);
		props.setProperty("auto.offset.reset", "earliest");
		
		KafkaConsumer<String, String> consumer = new KafkaConsumer<String, String>(props);
		consumer.subscribe(Arrays.asList(topic));
		
		while(true) {
			LOG.info("polling from infinite loop...");
			ConsumerRecords<String, String> recs = consumer.poll(Duration.ofSeconds(1));
			recs.forEach(r -> LOG.info(r.topic() + " (P:" + r.partition() + "; O: " + r.offset() + 
					") - " + r.key() + " : " + r.value()));
			
		}
		
		
		
	}
}
