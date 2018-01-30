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

public class Device extends AbstractActor {
	private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
	
	final String groupId;
	final String deviceId;
	
	Optional<Double> lastTemperatureReading = Optional.empty();
	
	public Device(String groupId, String deviceId){
		this.groupId = groupId;
		this.deviceId = deviceId;
	}
	
	public static Props props(String groupId, String deviceId){
		return Props.create(Device.class, groupId, deviceId);
	}
	
	@Override
	public void preStart(){
		log.info("Device actor {}-{} started", groupId, deviceId);
	}
	
	@Override
	public void postStop(){
		log.info("Device actor {}-{} stopped", groupId, deviceId);
	}
	
	@Override
	public Receive createReceive(){
		return receiveBuilder()
				.match(RequestTrackDevice.class, r -> {
					if(this.groupId.equals(r.groupId) && this.deviceId.equals(r.deviceId)){
						getSender().tell(new DeviceRegistred(), getSelf());
					}
					else{
						log.warning("Ignoring TrackDevice request for {}-{}. This actor is responsible for {}-{}.",
								r.groupId,r.deviceId,this.groupId,this.deviceId);
					}
				})
				.match(RecordTemperature.class, r -> {
					log.info("Recorded temperature reading {} with {}", r.value, r.requestId);
					lastTemperatureReading = Optional.of(r.value);
					getSender().tell(new TemperatureRecorded(r.requestId), getSelf());
				})
				.match(ReadTemperature.class, r ->
					getSender().tell(new RespondTemperature(r.requestId, lastTemperatureReading), getSelf())
				).build();
	}
	
}
