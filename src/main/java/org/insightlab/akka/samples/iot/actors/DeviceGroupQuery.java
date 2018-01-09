package org.insightlab.akka.samples.iot.actors;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.insightlab.akka.samples.iot.messages.temperature.ReadTemperature;
import org.insightlab.akka.samples.iot.messages.temperature.RespondAllTemperatures;
import org.insightlab.akka.samples.iot.messages.temperature.RespondTemperature;
import org.insightlab.akka.samples.iot.states.temperature.DeviceNotAvailable;
import org.insightlab.akka.samples.iot.states.temperature.DeviceTimedOut;
import org.insightlab.akka.samples.iot.states.temperature.Temperature;
import org.insightlab.akka.samples.iot.states.temperature.TemperatureNotAvailable;
import org.insightlab.akka.samples.iot.states.temperature.TemperatureReading;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Cancellable;
import akka.actor.Props;
import akka.actor.Terminated;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import scala.concurrent.duration.FiniteDuration;

public class DeviceGroupQuery extends AbstractActor {
	
	public static final class CollectionTimeout{
	}
	
	private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
	
	final Map<ActorRef,String> actorToDevice;
	final long requestId;
	final ActorRef requester;
	
	Cancellable queryTimeoutTimer;
	
	public DeviceGroupQuery(Map<ActorRef,String> actorToDevice, long requestId, ActorRef requester, FiniteDuration timeout){
		this.actorToDevice = actorToDevice;
		this.requestId = requestId;
		this.requester = requester;
				
		queryTimeoutTimer = getContext().getSystem().scheduler().scheduleOnce(
				timeout, getSelf(), new CollectionTimeout(), getContext().dispatcher(), getSelf());
	}
	
	public static Props props(Map<ActorRef,String> actorToDevice, long requestId, ActorRef requester, FiniteDuration timeout){
		return Props.create(DeviceGroupQuery.class, actorToDevice, requestId, requester, timeout);
	}
	
	@Override
	public void preStart(){
		for (ActorRef deviceActor : actorToDevice.keySet()){
			getContext().watch(deviceActor);
			deviceActor.tell(new ReadTemperature(0L), getSelf());
		}
	}
	
	@Override
	public void postStop(){
		queryTimeoutTimer.cancel();
	}
	
	@Override
	public Receive createReceive() {
		return waitingForReplies(new HashMap<>(), actorToDevice.keySet());
	}
	
	public Receive waitingForReplies(Map<String,TemperatureReading> repliesSoFar, Set<ActorRef> stillWaiting){
		return receiveBuilder()
				.match(RespondTemperature.class, r -> {
					ActorRef deviceActor = getSender();
					TemperatureReading reading = r.value.map(v -> (TemperatureReading) new Temperature(v))
													.orElse(new TemperatureNotAvailable());
					
					receivedResponse(deviceActor, reading, stillWaiting, repliesSoFar);
				})
				.match(Terminated.class, t -> receivedResponse(t.getActor(), new DeviceNotAvailable(), stillWaiting, repliesSoFar))
				.match(CollectionTimeout.class,  t -> {
					Map<String, TemperatureReading> replies = new HashMap<>(repliesSoFar);
					for(ActorRef deviceActor : stillWaiting){
						String deviceId = actorToDevice.get(deviceActor);
						replies.put(deviceId, new DeviceTimedOut());
					}
					
					requester.tell(new RespondAllTemperatures(requestId, replies), getSelf());
					getContext().stop(getSelf());
				})
				.build();
	}

	private void receivedResponse(ActorRef deviceActor, TemperatureReading reading, Set<ActorRef> stillWaiting,
			Map<String, TemperatureReading> repliesSoFar) {
		
		getContext().unwatch(deviceActor);
		String deviceId = actorToDevice.get(deviceActor);
		
		Set<ActorRef> newStillWaiting = new HashSet<>(stillWaiting);
		newStillWaiting.remove(deviceActor);
		
		Map<String, TemperatureReading> newRepliesSoFar = new HashMap<>(repliesSoFar);
		newRepliesSoFar.put(deviceId, reading);
		
		if(newStillWaiting.isEmpty()){
			requester.tell(new RespondAllTemperatures(requestId, newRepliesSoFar), getSelf());
			getContext().stop(getSelf());
		}
		else{
			getContext().become(waitingForReplies(newRepliesSoFar, newStillWaiting));
		}
	}
	
}
