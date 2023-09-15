package by.zzelberman.kafka101;

import java.time.Duration;
import java.util.Arrays;
import java.util.Properties;

import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConsumerDemoGracefulShutdown {
	private static final Logger LOG = LoggerFactory.getLogger(ConsumerDemoGracefulShutdown.class);

	public static void main(String[] args) {
		LOG.info("From ConsumerDemoGracefulShutdown.main(args)");
		final String topic = "java-demo";
		final String group = "java-first-cgroup";

		Properties props = new Properties();
		props.setProperty("bootstrap.servers", "127.0.0.1:29092,127.0.0.1:29093,127.0.0.1:29094");
		props.setProperty("key.deserializer", StringDeserializer.class.getName());
		props.setProperty("value.deserializer", StringDeserializer.class.getName());

		props.setProperty("group.id", group);
		props.setProperty("auto.offset.reset", "earliest");

		KafkaConsumer<String, String> consumer = new KafkaConsumer<String, String>(props);

		final Thread shutdownThreadReference = Thread.currentThread();
		Runnable hookReference = () -> {
			Thread.currentThread().setName("Shutdown Hook Thread");
			LOG.info("Processing shutdown hook to gracefully down Kafka consumer");
			try {
				consumer.wakeup();
				shutdownThreadReference.join();
				LOG.info("Shutdown hook is completed");
			} catch (InterruptedException e) {
				LOG.error("Waiting for shutdown thread finish has been interrupted", e);
			}
		};
		Runtime.getRuntime().addShutdownHook(new Thread(hookReference));
	
		Runnable hookCaller = () -> {
			try {
				Thread.currentThread().sleep(6000);
				causeShutdownInEclipse();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		};
		new Thread(hookCaller).start();
		
		try {
			consumer.subscribe(Arrays.asList(topic));

			while (true) {
				LOG.info("polling from infinite loop...");
				ConsumerRecords<String, String> recs = consumer.poll(Duration.ofSeconds(1));
				recs.forEach(r -> LOG.info(r.topic() + " (P:" + r.partition() + "; O: " + r.offset() + ") - " +
						r.key() + " : " + r.value()));

			}
		} catch (WakeupException e) {
			LOG.info("Consumer has been woken up by a shutdown hook");
		} catch (Exception e) {
			LOG.error("Unexpected error while trying to shutdown the consumer", e);
		} finally {
			LOG.info("Finaly Closing the consumer");
			consumer.close();
			LOG.info("Consumer has been closed");
		}

	}
	
	static void causeShutdownInEclipse() {
		System.exit(0);
	}
}
