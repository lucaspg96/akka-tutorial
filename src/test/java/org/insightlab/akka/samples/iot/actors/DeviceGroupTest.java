package org.insightlab.akka.samples.iot.actors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNotEquals;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.insightlab.akka.samples.iot.messages.registration.DeviceRegistred;
import org.insightlab.akka.samples.iot.messages.registration.RequestTrackDevice;
import org.insightlab.akka.samples.iot.messages.temperature.RecordTemperature;
import org.insightlab.akka.samples.iot.messages.temperature.RequestAllTemperatures;
import org.insightlab.akka.samples.iot.messages.temperature.RespondAllTemperatures;
import org.insightlab.akka.samples.iot.messages.temperature.TemperatureRecorded;
import org.insightlab.akka.samples.iot.states.temperature.Temperature;
import org.insightlab.akka.samples.iot.states.temperature.TemperatureNotAvailable;
import org.insightlab.akka.samples.iot.states.temperature.TemperatureReading;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.PoisonPill;
import akka.testkit.TestKit;
import scala.Function0;

public class DeviceGroupTest {
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
	public void testRegisterDeviceActor() {
		ActorRef groupActor = system.actorOf(DeviceGroup.props("group"));
		
		groupActor.tell(new RequestTrackDevice("group", "device1"), probe.testActor());
		probe.expectMsgClass(DeviceRegistred.class);
		ActorRef deviceActor1 = probe.lastSender();
		
		groupActor.tell(new RequestTrackDevice("group", "device2"), probe.testActor());
		probe.expectMsgClass(DeviceRegistred.class);
		ActorRef deviceActor2 = probe.lastSender();
		
		assertNotEquals(deviceActor1,deviceActor2);
		
		deviceActor1.tell(new RecordTemperature(0L, 1.0), probe.testActor());
		assertEquals(0L,probe.expectMsgClass(TemperatureRecorded.class).requestId);
		
		deviceActor2.tell(new RecordTemperature(1L, 2.0), probe.testActor());
		assertEquals(1L,probe.expectMsgClass(TemperatureRecorded.class).requestId);
	}
	
	@Test
	public void testIgnoreRequestsForWrongGroup(){
		ActorRef groupActor = system.actorOf(DeviceGroup.props("group"));
		
		groupActor.tell(new RequestTrackDevice("wrongGroup", "device"), probe.testActor());
		probe.expectNoMsg();
	}
	
	@Test
	public void testReturnSameActorForSameDeviceId(){
		ActorRef groupActor = system.actorOf(DeviceGroup.props("group"));
		
		groupActor.tell(new RequestTrackDevice("group", "device1"),probe.testActor());
		probe.expectMsgClass(DeviceRegistred.class);
		ActorRef deviceActor1 = probe.lastSender();
		
		groupActor.tell(new RequestTrackDevice("group", "device1"),probe.testActor());
		probe.expectMsgClass(DeviceRegistred.class);
		ActorRef deviceActor2 = probe.lastSender();
		
		assertEquals(deviceActor1, deviceActor2);
	}

	@Test
	public void testListActiveDevices(){
		ActorRef groupActor = system.actorOf(DeviceGroup.props("group"));
		
		groupActor.tell(new RequestTrackDevice("group", "device1"), probe.testActor());
		probe.expectMsgClass(DeviceRegistred.class);
		
		groupActor.tell(new RequestTrackDevice("group", "device2"), probe.testActor());
		probe.expectMsgClass(DeviceRegistred.class);
		
		groupActor.tell(new DeviceGroup.RequestDeviceList(0L), probe.testActor());
		DeviceGroup.ReplyDeviceList reply = probe.expectMsgClass(DeviceGroup.ReplyDeviceList.class);
		
		assertEquals(0L,  reply.requestId);
		assertEquals(Stream.of("device1","device2").collect(Collectors.toSet()),reply.ids);
	}
	
	@Test
	public void testListActiveDevicesAfterOneShutdown(){
		ActorRef groupActor = system.actorOf(DeviceGroup.props("group"));
		
		groupActor.tell(new RequestTrackDevice("group", "device1"), probe.testActor());
		probe.expectMsgClass(DeviceRegistred.class);
		ActorRef toShutdown = probe.lastSender();
		
		groupActor.tell(new RequestTrackDevice("group", "device2"), probe.testActor());
		probe.expectMsgClass(DeviceRegistred.class);
		
		groupActor.tell(new DeviceGroup.RequestDeviceList(0L), probe.testActor());
		DeviceGroup.ReplyDeviceList reply = probe.expectMsgClass(DeviceGroup.ReplyDeviceList.class);
		
		assertEquals(0L,  reply.requestId);
		assertEquals(Stream.of("device1","device2").collect(Collectors.toSet()),reply.ids);
		
		probe.watch(toShutdown);
		toShutdown.tell(PoisonPill.getInstance(), ActorRef.noSender());
		probe.expectTerminated(toShutdown, probe.expectTerminated$default$2());
		probe.awaitAssert(new Function0<Object>() {
			
			@Override
			public Object apply() {
				groupActor.tell(new DeviceGroup.RequestDeviceList(1L), probe.testActor());
				DeviceGroup.ReplyDeviceList r = probe.expectMsgClass(DeviceGroup.ReplyDeviceList.class);
				
				assertEquals(1L,r.requestId);
				assertEquals(Stream.of("device2").collect(Collectors.toSet()),r.ids);
				return null;
			}
		}, probe.awaitAssert$default$2(), probe.awaitAssert$default$3());
	}
	
	@Test
	public void testCollectTemperaturesFromAllActiveDevices(){
		TestKit probe = new TestKit(system);
		ActorRef groupActor = system.actorOf(DeviceGroup.props("group"));
		
		groupActor.tell(new RequestTrackDevice("group", "device1"), probe.testActor());
		probe.expectMsgClass(DeviceRegistred.class);
		ActorRef deviceActor1 = probe.lastSender();
		
		groupActor.tell(new RequestTrackDevice("group", "device2"), probe.testActor());
		probe.expectMsgClass(DeviceRegistred.class);
		ActorRef deviceActor2 = probe.lastSender();
		
		groupActor.tell(new RequestTrackDevice("group", "device3"), probe.testActor());
		probe.expectMsgClass(DeviceRegistred.class);
		ActorRef deviceActor3 = probe.lastSender();
		
		deviceActor1.tell(new RecordTemperature(0L, 1.0), probe.testActor());
		assertEquals(0L, probe.expectMsgClass(TemperatureRecorded.class).requestId);
		
		deviceActor2.tell(new RecordTemperature(1L, 2.0), probe.testActor());
		assertEquals(1L, probe.expectMsgClass(TemperatureRecorded.class).requestId);
		
		groupActor.tell(new RequestAllTemperatures(0L), probe.testActor());
		RespondAllTemperatures response = probe.expectMsgClass(RespondAllTemperatures.class);
		assertEquals(0L, response.requestId);
		
		Map<String, TemperatureReading> expectedTemperatures = new HashMap<>();
		expectedTemperatures.put("device1", new Temperature(1.0));
		expectedTemperatures.put("device2", new Temperature(2.0));
		expectedTemperatures.put("device3", new TemperatureNotAvailable());
		
		assertTrue(expectedTemperatures.equals(response.temperatures));

	}
}
