package aqua.blatt1.client;

import java.net.InetSocketAddress;

import aqua.blatt1.common.Direction;
import aqua.blatt1.common.msgtypes.*;
import messaging.Endpoint;
import messaging.Message;
import aqua.blatt1.common.FishModel;
import aqua.blatt1.common.Properties;

import javax.swing.*;

public class ClientCommunicator {
	private final Endpoint endpoint;

	public ClientCommunicator() {
		endpoint = new Endpoint();
	}

	public class ClientForwarder {
		private final InetSocketAddress broker;

		private ClientForwarder() {
			this.broker = new InetSocketAddress(Properties.HOST, Properties.PORT);
		}

		public void register() {
			endpoint.send(broker, new RegisterRequest());
		}

		public void deregister(String id) {
			endpoint.send(broker, new DeregisterRequest(id));
		}

		public void handOff(FishModel fish, InetSocketAddress adr) {
				endpoint.send(adr, new HandoffRequest(fish));
		}

		public void Token(InetSocketAddress adr) {
			endpoint.send(adr, new Token());
		}
		public void snapshotMarker(InetSocketAddress rightNeighbour, InetSocketAddress leftNeighbour) {
			endpoint.send(rightNeighbour, new SnapshotMarker());
			endpoint.send(leftNeighbour, new SnapshotMarker());
        }
        public void snapshotToken (InetSocketAddress leftNeighbour, SnapshotToken snapshotToken){
			endpoint.send(leftNeighbour, snapshotToken);
		}
		public void sendLocationRequest(InetSocketAddress destination, String fishId){
			endpoint.send(destination, new LocationRequest(fishId));
		}
		public void sendNameResolutionRequest(String tankId, String requestId){
			endpoint.send(broker, new NameResolutionRequest(tankId,requestId));
		}
		public void sendLocationUpdate (InetSocketAddress  source, String fishId){
			endpoint.send(source, new LocationUpdate(fishId));
		}
	}

	public class ClientReceiver extends Thread {
		private final TankModel tankModel;

		private ClientReceiver(TankModel tankModel) {
			this.tankModel = tankModel;
		}

		@Override
		public void run() {
			while (!isInterrupted()) {
				Message msg = endpoint.blockingReceive();

				if (msg.getPayload() instanceof RegisterResponse)
					tankModel.onRegistration(((RegisterResponse) msg.getPayload()).getId());

				if (msg.getPayload() instanceof HandoffRequest)
					tankModel.receiveFish(((HandoffRequest) msg.getPayload()).getFish());
				if (msg.getPayload() instanceof NeighborUpdate){
					NeighborUpdate update = (NeighborUpdate) msg.getPayload();
					InetSocketAddress left = update.getLeftNeighbor();
					InetSocketAddress right = update.getRightNeighbor();
					if(left != null){
						tankModel.setLeftNeighbor(left);
					}
					if(right != null){
						tankModel.setRightNeighbor(right);
					}
				}
				if (msg.getPayload() instanceof Token){
					tankModel.setToken();
				}
                if (msg.getPayload() instanceof SnapshotMarker){
                    tankModel.initiateSnapshot(msg.getSender(), false);
                }
                if(msg.getPayload() instanceof  SnapshotToken){
					if(tankModel.initiateToken){
						SnapshotToken snapshotToken = (SnapshotToken) msg.getPayload();
						JOptionPane.showMessageDialog(null, "Anzahl Fische: " + snapshotToken.getAmountOfFishies(),"Eine Nachricht",JOptionPane.INFORMATION_MESSAGE);
					}else {
						tankModel.snapshotToken = (SnapshotToken) msg.getPayload();

					}
				}
				if(msg.getPayload() instanceof  LocationRequest){
					LocationRequest lr = (LocationRequest) msg.getPayload();
					tankModel.locateFishGlobally(lr.getFishId());
				}
				if(msg.getPayload() instanceof  NameResolutionResponse){
					NameResolutionResponse n = (NameResolutionResponse) msg.getPayload();
					tankModel.forwarder.sendLocationUpdate(n.getSource(), n.getRequestId());
				}
				if(msg.getPayload() instanceof  LocationUpdate){
					LocationUpdate lu = (LocationUpdate) msg.getPayload();
					tankModel.homeAgent.put(lu.getFishId(), msg.getSender());
				}
			}
			System.out.println("Receiver stopped.");
		}
	}

	public ClientForwarder newClientForwarder() {
		return new ClientForwarder();
	}

	public ClientReceiver newClientReceiver(TankModel tankModel) {
		return new ClientReceiver(tankModel);
	}

}
