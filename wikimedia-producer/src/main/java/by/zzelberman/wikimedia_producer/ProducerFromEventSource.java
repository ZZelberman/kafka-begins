package by.zzelberman.wikimedia_producer;

import java.net.MalformedURLException;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.kafka.clients.producer.ProducerConfig.*;

import com.launchdarkly.eventsource.EventSource;

public class ProducerFromEventSource {
	private static final String EVENT_SOURCE_ENDPOINT = "https://stream.wikimedia.org/v2/stream/recentchange";
	private static final String TOPIC = "wikimedia-change-events"; 
	private static final Logger LOG = LoggerFactory.getLogger(ProducerFromEventSource.class);
	private static volatile long eventCount = 0;
	
	public static void main(String[] args) {
		Properties props = new Properties();
		props.setProperty(BOOTSTRAP_SERVERS_CONFIG, "127.0.0.1:29092,127.0.0.1:29093,127.0.0.1:29094");
		props.setProperty(KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
		props.setProperty(VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
		//these are enabled by default, but actual for Kafka <= 2.8
		props.setProperty(ACKS_CONFIG, "all");
		props.setProperty(ENABLE_IDEMPOTENCE_CONFIG, "true");
		props.setProperty(RETRIES_CONFIG, Integer.toString(Integer.MAX_VALUE));
		//effective settings
		props.setProperty(COMPRESSION_TYPE_CONFIG, "snappy");
		props.setProperty(LINGER_MS_CONFIG, "20");
		props.setProperty(BATCH_SIZE_CONFIG, Integer.toString(1024 * 32));
		
		KafkaProducer<String, String> producer = new KafkaProducer<String, String>(props);
		
		EventSource.Builder esb = null;
		try {
			esb = new EventSource.Builder(new java.net.URL(EVENT_SOURCE_ENDPOINT));
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		EventSource src = esb.build();
		
		new Thread(() -> {
			try {
				TimeUnit.SECONDS.sleep(3);
				LOG.info("Calling EventSource to close itself");
				src.close();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}).start();
		
		try {
			src.messages().forEach(m -> {
				LOG.info("[" + ++eventCount + "] - " + m.getData());
				producer.send(new ProducerRecord<String, String>(TOPIC, m.getData()));
			});
			LOG.info("Event Source closed");
		} finally {
			producer.flush();
			producer.close();
			LOG.info("Producer is closed after EventSource is stopped");
		}
		
		
	}
}
