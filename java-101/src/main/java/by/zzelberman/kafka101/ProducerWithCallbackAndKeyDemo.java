package by.zzelberman.kafka101;

import java.util.Properties;
import java.util.stream.IntStream;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProducerWithCallbackAndKeyDemo {
	private static final Logger LOG = LoggerFactory.getLogger(ProducerWithCallbackAndKeyDemo.class);
	
	public static void main(String[] args) {
		LOG.info("From ProducerDemo.main(args)");
		
		Properties props = new Properties();
		props.setProperty("bootstrap.servers", "127.0.0.1:29092,127.0.0.1:29093,127.0.0.1:29094");
		props.setProperty("key.serializer", StringSerializer.class.getName());
		props.setProperty("value.serializer", StringSerializer.class.getName());
		props.setProperty("batch.size", "10");
		
		KafkaProducer<String, String> producer = new KafkaProducer<String, String>(props);
		IntStream.rangeClosed(1, 10).forEach(i -> {
			IntStream.rangeClosed(i, i + 10).forEach(j -> {
				String key = "key_" + (j - 1) / 10;
				ProducerRecord<String, String> rec = new ProducerRecord<>("java-demo", key, "val_" + j);
				producer.send(rec, (metadata, exc) -> {
					if (exc != null) {
						LOG.error("Error in producer callback: ", exc);
						return;
					}
					LOG.info("\n    Topic: " + metadata.topic() + "; key: " + key + "; partition: " + metadata.partition() + "; offset: " + metadata.offset());
				});  //async
			});
			
			try {
				Thread.sleep(300);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		});
		
		producer.flush();    //sync
		LOG.info("all msgs have been flushed");
		producer.close(); 
		LOG.info("producer closed");
		
	}
}
