package org.insightlab.akka.samples.iot.actors;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.insightlab.akka.samples.iot.messages.registration.RequestTrackDevice;
import org.insightlab.akka.samples.iot.messages.temperature.RequestAllTemperatures;
import org.insightlab.akka.samples.iot.messages.temperature.RespondAllTemperatures;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.Terminated;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import scala.concurrent.duration.FiniteDuration;

public class DeviceGroup extends AbstractActor {
	private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
	
	public final String groupId;
	
	final long nextCollectionId = 0L;
	
	private final Map<String,ActorRef> deviceActors;
	private final Map<ActorRef,String> actorsIds;
	
	public DeviceGroup(String groupId) {
		this.groupId = groupId;
		deviceActors = new HashMap<>();
		actorsIds = new HashMap<>();
	}
	
	public static Props props(String groupId){
		return Props.create(DeviceGroup.class, groupId);
	}
	
	public static final class RequestDeviceList{
		final long requestId;
		
		public RequestDeviceList(long requestId){
			this.requestId = requestId;
		}
	}
	
	public static final class ReplyDeviceList{
		final long requestId;
		final Set<String> ids;
		
		public ReplyDeviceList(long requestId, Set<String> ids){
			this.requestId = requestId;
			this.ids = ids;
		}
	}
	
	private void onAllTemperatures(RequestAllTemperatures r){
		getContext().actorOf(DeviceGroupQuery.props(
				actorsIds, r.requestId, getSender(), new FiniteDuration(3, TimeUnit.SECONDS)));
	}
	
	private void onTrackDevice(RequestTrackDevice trackMsg){
		if(this.groupId.equals(trackMsg.groupId)){
			ActorRef deviceActor = deviceActors.get(trackMsg.deviceId);
			
			if(deviceActor != null){
				deviceActor.forward(trackMsg, getContext());
			}
			else{
				log.info("Creating device actor for {}", trackMsg.deviceId);
				deviceActor = getContext().actorOf(Device.props(groupId, trackMsg.deviceId),"device-"+trackMsg.deviceId);
				getContext().watch(deviceActor);
				deviceActors.put(trackMsg.deviceId, deviceActor);
				actorsIds.put(deviceActor, trackMsg.deviceId);
				deviceActor.forward(trackMsg, getContext());
			}
			
		}
		else{
			log.warning("Ignoring TrackDevice request for {}. This actor is responsible for {}.",trackMsg.groupId, groupId);
		}
	}

	private void onTerminate(Terminated t){
		ActorRef deviceActor = t.getActor();
		String deviceId = actorsIds.get(deviceActor);
		
		log.info("Device actor {} has been terminated",deviceId);
		
		actorsIds.remove(deviceActor);
		deviceActors.remove(deviceId);
		
	}
	
	private void onDeviceList(RequestDeviceList r){
		getSender().tell(new ReplyDeviceList(r.requestId, deviceActors.keySet()), getSelf());
	}
	
	@Override
	public void preStart() {
		log.info("DeviceGroup {} started", groupId);
	}
	
	@Override
	public void postStop() {
		log.info("DeviceGroup {} stopped", groupId);
	}
	
	@Override
	public Receive createReceive() {
		return receiveBuilder()
				.match(RequestTrackDevice.class, this::onTrackDevice)
				.match(Terminated.class, this::onTerminate)
				.match(RequestDeviceList.class, this::onDeviceList)
				.match(RequestAllTemperatures.class, this::onAllTemperatures)
				.build();
	}

}
