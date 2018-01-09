package org.insightlab.akka.samples.iot.actors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.insightlab.akka.samples.iot.messages.registration.DeviceRegistred;
import org.insightlab.akka.samples.iot.messages.registration.RequestTrackDevice;
import org.insightlab.akka.samples.iot.messages.temperature.RecordTemperature;
import org.insightlab.akka.samples.iot.messages.temperature.TemperatureRecorded;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.PoisonPill;
import akka.testkit.TestKit;
import scala.Function0;

public class DeviceManagerTest {

	static ActorSystem system;
	TestKit probe;
	
	@BeforeClass
	public static void setUpClass(){
		system = ActorSystem.create("iot-test-system");
	}
	
	@Before
	public void setUp(){
		probe = new TestKit(system);
	}

	@Test
	public void testRegisterDeviceActorOnSameGroup() {
		ActorRef manageActor = system.actorOf(DeviceManager.props());
		
		manageActor.tell(new RequestTrackDevice("group", "device1"), probe.testActor());
		probe.expectMsgClass(DeviceRegistred.class);
		ActorRef deviceActor1 = probe.lastSender();
		
		manageActor.tell(new RequestTrackDevice("group", "device2"), probe.testActor());
		probe.expectMsgClass(DeviceRegistred.class);
		ActorRef deviceActor2 = probe.lastSender();
		
		assertNotEquals(deviceActor1,deviceActor2);
		
		deviceActor1.tell(new RecordTemperature(0L, 1.0), probe.testActor());
		assertEquals(0L,probe.expectMsgClass(TemperatureRecorded.class).requestId);
		
		deviceActor2.tell(new RecordTemperature(1L, 2.0), probe.testActor());
		assertEquals(1L,probe.expectMsgClass(TemperatureRecorded.class).requestId);
	}
	
	@Test
	public void testRegisterDeviceActorOnDifferentGroup() {
		ActorRef manageActor = system.actorOf(DeviceManager.props());
		
		manageActor.tell(new RequestTrackDevice("group1", "device1"), probe.testActor());
		probe.expectMsgClass(DeviceRegistred.class);
		ActorRef deviceActor1 = probe.lastSender();
		
		manageActor.tell(new RequestTrackDevice("group2", "device2"), probe.testActor());
		probe.expectMsgClass(DeviceRegistred.class);
		ActorRef deviceActor2 = probe.lastSender();
		
		assertNotEquals(deviceActor1,deviceActor2);
		
		deviceActor1.tell(new RecordTemperature(0L, 1.0), probe.testActor());
		assertEquals(0L,probe.expectMsgClass(TemperatureRecorded.class).requestId);
		
		deviceActor2.tell(new RecordTemperature(1L, 2.0), probe.testActor());
		assertEquals(1L,probe.expectMsgClass(TemperatureRecorded.class).requestId);
	}
	
	@Test
	public void testReturnSameActorForSameDeviceId(){
		ActorRef manageActor = system.actorOf(DeviceManager.props());
		
		manageActor.tell(new RequestTrackDevice("group", "device1"),probe.testActor());
		probe.expectMsgClass(DeviceRegistred.class);
		ActorRef deviceActor1 = probe.lastSender();
		
		manageActor.tell(new RequestTrackDevice("group", "device1"),probe.testActor());
		probe.expectMsgClass(DeviceRegistred.class);
		ActorRef deviceActor2 = probe.lastSender();
		
		assertEquals(deviceActor1, deviceActor2);
	}

}
