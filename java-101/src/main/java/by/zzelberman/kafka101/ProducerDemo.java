package by.zzelberman.kafka101;

import java.util.Properties;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProducerDemo {
	private static final Logger LOG = LoggerFactory.getLogger(ProducerDemo.class);
	
	public static void main(String[] args) {
		LOG.info("From ProducerDemo.main(args)");
		
		Properties props = new Properties();
		props.setProperty("bootstrap.servers", "127.0.0.1:29092,127.0.0.1:29093,127.0.0.1:29094");
		props.setProperty("key.serializer", StringSerializer.class.getName());
		props.setProperty("value.serializer", StringSerializer.class.getName());
		
		KafkaProducer<String, String> producer = new KafkaProducer<String, String>(props);
		ProducerRecord<String, String> rec = new ProducerRecord<String, String>("java-demo", "Hello from Java producer");
		
		LOG.info("sending msg from producer...");
		producer.send(rec);  //async
		producer.flush();    //sync
		LOG.info("all msgs have been flushed");
		producer.close(); 
		LOG.info("producer closed");
		
	}
}
