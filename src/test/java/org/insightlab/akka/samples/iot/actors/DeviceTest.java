package org.insightlab.akka.samples.iot.actors;

import static org.junit.Assert.assertEquals;

import java.util.Optional;

import org.insightlab.akka.samples.iot.messages.registration.DeviceRegistred;
import org.insightlab.akka.samples.iot.messages.registration.RequestTrackDevice;
import org.insightlab.akka.samples.iot.messages.temperature.ReadTemperature;
import org.insightlab.akka.samples.iot.messages.temperature.RecordTemperature;
import org.insightlab.akka.samples.iot.messages.temperature.RespondTemperature;
import org.insightlab.akka.samples.iot.messages.temperature.TemperatureRecorded;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.testkit.TestKit;

public class DeviceTest {
	ActorSystem system;
	TestKit probe;
	
	@Before
	public void setUp(){
		system = ActorSystem.create("iot-test-system");
		probe = new TestKit(system);
	}
	
	@After
	public void setDown(){
		system.terminate();
	}
	
	@Test
	public void testReplyWithEmpyTemperature() {
		
		ActorRef deviceActor = system.actorOf(Device.props("group", "device"));
		
		deviceActor.tell(new RecordTemperature(1L,24.0), probe.testActor());
		assertEquals(1L,  probe.expectMsgClass(TemperatureRecorded.class).requestId);
		
		deviceActor.tell(new ReadTemperature(2L), probe.testActor());
		RespondTemperature response1 = probe.expectMsgClass(RespondTemperature.class);
		assertEquals(2L, response1.requestId);
		assertEquals(Optional.of(24.0), response1.value);
		
		deviceActor.tell(new RecordTemperature(3L, 55.0), probe.testActor());
		assertEquals(3L, probe.expectMsgClass(TemperatureRecorded.class).requestId);
		
		deviceActor.tell(new ReadTemperature(4L), probe.testActor());
		RespondTemperature response2 = probe.expectMsgClass(RespondTemperature.class);
		assertEquals(4L,  response2.requestId);
		assertEquals(Optional.of(55.0), response2.value);

	}
	
	@Test
	public void testReplyToRegistrationRequest(){
		ActorRef deviceActor = system.actorOf(Device.props("group", "device"));
		
		deviceActor.tell(new RequestTrackDevice("group", "device"), probe.testActor());
		probe.expectMsgClass(DeviceRegistred.class);
		assertEquals(deviceActor, probe.lastSender());
	}
	
	@Test
	public void testIgnoreWrongRegistrationRequest(){
		ActorRef deviceActor = system.actorOf(Device.props("group", "device"));
		
		deviceActor.tell(new RequestTrackDevice("wrongGroup", "device"), probe.testActor());
		probe.expectNoMsg();
		
		deviceActor.tell(new RequestTrackDevice("group", "wrongDevice"), probe.testActor());
		probe.expectNoMsg();
	}

}
