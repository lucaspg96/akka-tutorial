package org.insightlab.akka.samples.iot.actors;

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.insightlab.akka.samples.iot.messages.temperature.ReadTemperature;
import org.insightlab.akka.samples.iot.messages.temperature.RespondAllTemperatures;
import org.insightlab.akka.samples.iot.messages.temperature.RespondTemperature;
import org.insightlab.akka.samples.iot.states.temperature.DeviceNotAvailable;
import org.insightlab.akka.samples.iot.states.temperature.DeviceTimedOut;
import org.insightlab.akka.samples.iot.states.temperature.Temperature;
import org.insightlab.akka.samples.iot.states.temperature.TemperatureNotAvailable;
import org.insightlab.akka.samples.iot.states.temperature.TemperatureReading;
import org.junit.BeforeClass;
import org.junit.Test;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.PoisonPill;
import akka.testkit.TestKit;
import scala.concurrent.duration.FiniteDuration;

public class DeviceGroupQueryTest {

	static ActorSystem system;
	
	@BeforeClass
	public static void setUpClass(){
		system = ActorSystem.create("iot-test-system");
	}

	
	@Test
	public void testReturnTemperatureValueForWorkingDevices(){
		TestKit requester = new TestKit(system);
		
		TestKit device1 = new TestKit(system);
		TestKit device2 = new TestKit(system);
		
		Map<ActorRef,String> actorToDevice = new HashMap<>();
		actorToDevice.put(device1.testActor(),"device1");
		actorToDevice.put(device2.testActor(),"device2");
		
		ActorRef queryActor = system.actorOf(DeviceGroupQuery.props(
				actorToDevice, 1L, requester.testActor(), new FiniteDuration(3, TimeUnit.SECONDS)));
		
		assertEquals(0L, device1.expectMsgClass(ReadTemperature.class).requestId);
		assertEquals(0L, device2.expectMsgClass(ReadTemperature.class).requestId);
		
		queryActor.tell(new RespondTemperature(0L,  Optional.of(1.0)), device1.testActor());
		queryActor.tell(new RespondTemperature(0L,  Optional.of(2.0)), device2.testActor());
		
		RespondAllTemperatures response = requester.expectMsgClass(RespondAllTemperatures.class);
		assertEquals(1L, response.requestId);
		
		Map<String, TemperatureReading> expectedTemperatures = new HashMap<>();
		expectedTemperatures.put("device1", new Temperature(1.0));
		expectedTemperatures.put("device2", new Temperature(2.0));
		
		assertTrue(expectedTemperatures.equals(response.temperatures));
	}
	
	@Test
	public void testReturnTemperatureNotAvailableForDevicesWithNoReadings(){
		TestKit requester = new TestKit(system);
		
		TestKit device1 = new TestKit(system);
		TestKit device2 = new TestKit(system);
		
		Map<ActorRef,String> actorToDevice = new HashMap<>();
		actorToDevice.put(device1.testActor(),"device1");
		actorToDevice.put(device2.testActor(),"device2");
		
		ActorRef queryActor = system.actorOf(DeviceGroupQuery.props(
				actorToDevice, 1L, requester.testActor(), new FiniteDuration(3, TimeUnit.SECONDS)));
		
		assertEquals(0L, device1.expectMsgClass(ReadTemperature.class).requestId);
		assertEquals(0L, device2.expectMsgClass(ReadTemperature.class).requestId);
		
		queryActor.tell(new RespondTemperature(0L,  Optional.empty()), device1.testActor());
		queryActor.tell(new RespondTemperature(0L,  Optional.of(2.0)), device2.testActor());
		
		RespondAllTemperatures response = requester.expectMsgClass(RespondAllTemperatures.class);
		assertEquals(1L, response.requestId);
		
		Map<String, TemperatureReading> expectedTemperatures = new HashMap<>();
		expectedTemperatures.put("device1", new TemperatureNotAvailable());
		expectedTemperatures.put("device2", new Temperature(2.0));

		assertTrue(expectedTemperatures.equals(response.temperatures));
	}
	
	@Test
	public void testReturnDeviceNotAvailableIfDeviceStopsBeforeAnswering(){
		TestKit requester = new TestKit(system);
		
		TestKit device1 = new TestKit(system);
		TestKit device2 = new TestKit(system);
		
		Map<ActorRef,String> actorToDevice = new HashMap<>();
		actorToDevice.put(device1.testActor(),"device1");
		actorToDevice.put(device2.testActor(),"device2");
		
		ActorRef queryActor = system.actorOf(DeviceGroupQuery.props(
				actorToDevice, 1L, requester.testActor(), new FiniteDuration(3, TimeUnit.SECONDS)));
		
		assertEquals(0L, device1.expectMsgClass(ReadTemperature.class).requestId);
		assertEquals(0L, device2.expectMsgClass(ReadTemperature.class).requestId);
		
		queryActor.tell(new RespondTemperature(0L,  Optional.of(1.0)), device1.testActor());
		device2.testActor().tell(PoisonPill.getInstance(), ActorRef.noSender());
		
		RespondAllTemperatures response = requester.expectMsgClass(RespondAllTemperatures.class);
		assertEquals(1L, response.requestId);
		
		Map<String, TemperatureReading> expectedTemperatures = new HashMap<>();
		expectedTemperatures.put("device1", new Temperature(1.0));
		expectedTemperatures.put("device2", new DeviceNotAvailable());

		assertTrue(expectedTemperatures.equals(response.temperatures));
	}
	
	@Test
	public void testReturnTemperatureReadingEvenIfDeviceStopsAfterAnswering(){
		TestKit requester = new TestKit(system);
		
		TestKit device1 = new TestKit(system);
		TestKit device2 = new TestKit(system);
		
		Map<ActorRef,String> actorToDevice = new HashMap<>();
		actorToDevice.put(device1.testActor(),"device1");
		actorToDevice.put(device2.testActor(),"device2");
		
		ActorRef queryActor = system.actorOf(DeviceGroupQuery.props(
				actorToDevice, 1L, requester.testActor(), new FiniteDuration(3, TimeUnit.SECONDS)));
		
		assertEquals(0L, device1.expectMsgClass(ReadTemperature.class).requestId);
		assertEquals(0L, device2.expectMsgClass(ReadTemperature.class).requestId);
		
		queryActor.tell(new RespondTemperature(0L,  Optional.of(1.0)), device1.testActor());
		queryActor.tell(new RespondTemperature(0L,  Optional.of(2.0)), device2.testActor());
		device2.testActor().tell(PoisonPill.getInstance(), ActorRef.noSender());
		
		RespondAllTemperatures response = requester.expectMsgClass(RespondAllTemperatures.class);
		assertEquals(1L, response.requestId);
		
		Map<String, TemperatureReading> expectedTemperatures = new HashMap<>();
		expectedTemperatures.put("device1", new Temperature(1.0));
		expectedTemperatures.put("device2", new Temperature(2.0));

		assertTrue(expectedTemperatures.equals(response.temperatures));
	}
	
	@Test
	public void testReturnDeviceTimedOutIfDeviceDoesNotAnswerInTime(){
		TestKit requester = new TestKit(system);
		
		TestKit device1 = new TestKit(system);
		TestKit device2 = new TestKit(system);
		
		Map<ActorRef,String> actorToDevice = new HashMap<>();
		actorToDevice.put(device1.testActor(),"device1");
		actorToDevice.put(device2.testActor(),"device2");
		
		ActorRef queryActor = system.actorOf(DeviceGroupQuery.props(
				actorToDevice, 1L, requester.testActor(), new FiniteDuration(3, TimeUnit.SECONDS)));
		
		assertEquals(0L, device1.expectMsgClass(ReadTemperature.class).requestId);
		assertEquals(0L, device2.expectMsgClass(ReadTemperature.class).requestId);
		
		queryActor.tell(new RespondTemperature(0L,  Optional.of(1.0)), device1.testActor());
		
		RespondAllTemperatures response = requester.expectMsgClass(
				FiniteDuration.create(5,  TimeUnit.SECONDS),RespondAllTemperatures.class);
		
		assertEquals(1L, response.requestId);
		
		Map<String, TemperatureReading> expectedTemperatures = new HashMap<>();
		expectedTemperatures.put("device1", new Temperature(1.0));
		expectedTemperatures.put("device2", new DeviceTimedOut());

		assertTrue(expectedTemperatures.equals(response.temperatures));
	}

}
