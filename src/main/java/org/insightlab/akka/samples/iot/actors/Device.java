package org.insightlab.akka.samples.iot.actors;

import java.util.Optional;

import org.insightlab.akka.samples.iot.messages.registration.DeviceRegistred;
import org.insightlab.akka.samples.iot.messages.registration.RequestTrackDevice;
import org.insightlab.akka.samples.iot.messages.temperature.ReadTemperature;
import org.insightlab.akka.samples.iot.messages.temperature.RecordTemperature;
import org.insightlab.akka.samples.iot.messages.temperature.RespondTemperature;
import org.insightlab.akka.samples.iot.messages.temperature.TemperatureRecorded;

import akka.actor.AbstractActor;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;

//This class represents an IoT device that register temperature
public class Device extends AbstractActor {
	private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
	
	//Every device has an group
	final String groupId;
	//Every device has its id
	final String deviceId;
	
	//The device may or not have a temperature reading, so we use an Optional object
	Optional<Double> lastTemperatureReading = Optional.empty();
	
	public Device(String groupId, String deviceId){
		this.groupId = groupId;
		this.deviceId = deviceId;
	}
	
	//This static method is used to create an actor indirectly
	public static Props props(String groupId, String deviceId){
		return Props.create(Device.class, groupId, deviceId);
	}
	
	//This method executes when the actor starts
	@Override
	public void preStart(){
		log.info("Device actor {}-{} started", groupId, deviceId);
	}
	
	//This method executes when the actor stops
	@Override
	public void postStop(){
		log.info("Device actor {}-{} stopped", groupId, deviceId);
	}
	
	//At this method we define, for each message pattern, the behavior of the actor 
	@Override
	public Receive createReceive(){
		return receiveBuilder()
				//The RequestTrackDevice message is for create the actor (if it wasn't created yet)
				.match(RequestTrackDevice.class, r -> {
					if(this.groupId.equals(r.groupId) && this.deviceId.equals(r.deviceId)){
						//If the message was send correctly, a DeviceRegistred message is sent
						getSender().tell(new DeviceRegistred(), getSelf());
					}
					else{
						//Else, a warning is shown on the log
						log.warning("Ignoring TrackDevice request for {}-{}. This actor is responsible for {}-{}.",
								r.groupId,r.deviceId,this.groupId,this.deviceId);
					}
				})
				//The RecordTemperature message is for store a new temperature reading on the device
				.match(RecordTemperature.class, r -> {
					log.info("Recorded temperature reading {} with {}", r.value, r.requestId);
					lastTemperatureReading = Optional.of(r.value);
					//A TemperatureRecorded is sent as an answer
					getSender().tell(new TemperatureRecorded(r.requestId), getSelf());
				})
				//The ReadTemperature message is for send the device's temperature reading 
				.match(ReadTemperature.class, r ->
					getSender().tell(new RespondTemperature(r.requestId, lastTemperatureReading), getSelf())
				).build();
	}
	
}
