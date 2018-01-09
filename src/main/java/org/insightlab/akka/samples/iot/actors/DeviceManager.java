package org.insightlab.akka.samples.iot.actors;

import java.util.HashMap;
import java.util.Map;

import org.insightlab.akka.samples.iot.messages.registration.RequestTrackDevice;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.Terminated;
import akka.event.Logging;
import akka.event.LoggingAdapter;

public final class DeviceManager extends AbstractActor {
	private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
	
	private final Map<String, ActorRef> groupToActor = new HashMap<>();
	private final Map<ActorRef, String> actorToGroup = new HashMap<>();
	
	public static Props props(){
		return Props.create(DeviceManager.class);
	}
	
	private void onTrackDevice(RequestTrackDevice trackMsg){
		String groupId = trackMsg.groupId;
		ActorRef ref = groupToActor.get(groupId);
		
		if(ref!=null){
			ref.forward(trackMsg, getContext());
		}
		else{
			log.info("Creating device group actor for {}", groupId);
			ActorRef groupActor = getContext().actorOf(DeviceGroup.props(groupId),"group-"+groupId);
			getContext().watch(groupActor);
			groupActor.forward(trackMsg, getContext());
			
			groupToActor.put(groupId, groupActor);
			actorToGroup.put(groupActor, groupId);
		}
	}
	
	private void onTerminated(Terminated t){
		ActorRef groupActor = t.getActor();
		String groupId = actorToGroup.get(groupActor);
		
		log.info("Device group {} has been terminated",groupId);
		
		actorToGroup.remove(groupActor);
		groupToActor.remove(groupId);
	}
	
	@Override
	public void preStart() {
		log.info("DeviceManager started");
	}

	@Override
	public void postStop() {
	  log.info("DeviceManager stopped");
	}

	@Override
	public Receive createReceive() {
		return receiveBuilder()
				.match(RequestTrackDevice.class, this::onTrackDevice)
				.match(Terminated.class, this::onTerminated)
				.build();
	}
}
