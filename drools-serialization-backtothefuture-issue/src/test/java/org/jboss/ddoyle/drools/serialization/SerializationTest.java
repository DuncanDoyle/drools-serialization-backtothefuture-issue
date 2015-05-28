package org.jboss.ddoyle.drools.serialization;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.concurrent.TimeUnit;

import org.drools.core.time.impl.PseudoClockScheduler;
import org.jboss.ddoyle.drools.serialization.model.SimpleEvent;
import org.junit.Test;
import org.kie.api.KieBase;
import org.kie.api.KieServices;
import org.kie.api.marshalling.Marshaller;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.KieSessionConfiguration;
import org.kie.internal.marshalling.MarshallerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Test that shows a problem with serialization in combination with inserting events in reverse order.
 * Fixed in Drools 6.2.x branch. 
 * 
 * @author <a href="mailto:duncan.doyle@redhat.com">Duncan Doyle</a>
 */
public class SerializationTest {

	private static final Logger LOGGER = LoggerFactory.getLogger(SerializationTest.class);

	@Test
	public void test() {
		KieServices kieServices = KieServices.Factory.get();
		KieContainer kieContainer = kieServices.getKieClasspathContainer();
		KieBase kieBase = kieContainer.getKieBase();

		// Create the session.
		KieSession kieSession = kieContainer.newKieSession();

		// Create the events.
		SimpleEvent simpleEventOne = new SimpleEvent();
		simpleEventOne.setCode("id1");
		simpleEventOne.setTimestamp(0);

		SimpleEvent simpleEventTwo = new SimpleEvent();
		simpleEventTwo.setCode("id2");
		simpleEventTwo.setTimestamp((28 * 60 * 60 * 1000));

		byte[] savedKieSession;

		try {
			/*
			 * Insert the events in the reverse order, so the oldest event last. This gives NPE in combination with older versions of
			 * Drools/JBoss BRMS.
			 */
			insertAndAdvance(kieSession, simpleEventTwo);
			insertAndAdvance(kieSession, simpleEventOne);

			LOGGER.info("Firing rules!");
			kieSession.fireAllRules();
			savedKieSession = saveKieSession(kieSession);
		} finally {
			kieSession.dispose();
		}

		// Load the KieSession.
		KieSession newKieSession = loadKieSession(savedKieSession, kieBase);
		assertEquals(2, newKieSession.getFactCount());

		try {
			LOGGER.info("Firing rules.");
			newKieSession.fireAllRules();
		} finally {
			newKieSession.dispose();
		}
	}

	public static void insertAndAdvance(KieSession kieSession, SimpleEvent event) {
		kieSession.insert(event);
		// Advance the clock if required.
		PseudoClockScheduler clock = kieSession.getSessionClock();
		long advanceTime = event.getTimestamp() - clock.getCurrentTime();
		if (advanceTime > 0) {
			clock.advanceTime(advanceTime, TimeUnit.MILLISECONDS);
		}
	}

	private static byte[] saveKieSession(KieSession kieSession) {
		ObjectOutputStream oos = null;
		ByteArrayOutputStream baos = null;
		try {

			baos = new ByteArrayOutputStream();
			oos = new ObjectOutputStream(baos);

			/*
			 * It seems that the Marshaller does not persist the actual SessionClock, which is a problem when using the PseudoClock, so we
			 * persist the SessionConfiguration, Environment and clock time to be able to restore the pseudo-clock (if it's used).
			 */
			KieSessionConfiguration kieSessionConfiguration = kieSession.getSessionConfiguration();
			oos.writeObject(kieSessionConfiguration);

			Marshaller marshaller = MarshallerFactory.newMarshaller(kieSession.getKieBase());
			marshaller.marshall(baos, kieSession);
			// baos.writeTo(fos);
			return baos.toByteArray();

		} catch (IOException ioe) {
			String errorMessage = "Unable to save KieSession.";
			LOGGER.error(errorMessage, ioe);
			throw new RuntimeException(errorMessage, ioe);
		} finally {
			if (oos != null) {
				try {
					oos.close();
				} catch (IOException e) {
					LOGGER.warn("Unable to close ObjectOutputStream.");
					// Not much we can do here, so swallowing excepion.
				}
			}
		}

	}

	protected static KieSession loadKieSession(byte[] kieSessionBytes, KieBase kieBase) {
		ObjectInputStream ois = null;
		try {

			ByteArrayInputStream bais = new ByteArrayInputStream(kieSessionBytes);

			ois = new ObjectInputStream(bais);

			KieSessionConfiguration kieSessionConfiguration = (KieSessionConfiguration) ois.readObject();

			Marshaller marshaller = MarshallerFactory.newMarshaller(kieBase);
			KieSession kieSession = marshaller.unmarshall(bais, kieSessionConfiguration, null);
			return kieSession;
		} catch (ClassNotFoundException cnfe) {
			String errorMessage = "Error loading serialized KieBase from file.";
			LOGGER.error(errorMessage, cnfe);
			throw new RuntimeException(errorMessage, cnfe);
		} catch (IOException ioe) {
			String errorMessage = "Error loading stored KieSession.";
			LOGGER.error(errorMessage, ioe);
			throw new RuntimeException(errorMessage, ioe);
		} catch (Exception e) {
			String errorMessage = "Error loading stored KieSession.";
			LOGGER.error(errorMessage, e);
			throw new RuntimeException(errorMessage, e);
		} finally {
			if (ois != null) {
				// This will also close the FIS.
				try {
					ois.close();
				} catch (IOException e) {
					LOGGER.warn("Unable to close ObjectInputStream.");
					// Not much we can do here, so swallowing exception.
				}
			}

		}
	}

}
