package aqua.blatt1.client;

import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import aqua.blatt1.common.Direction;
import aqua.blatt1.common.FishModel;
import aqua.blatt1.common.msgtypes.SnapshotToken;
import com.sun.org.apache.regexp.internal.RE;
import messaging.Message;

enum RecordMode{
	IDLE, LEFT, RIGHT,BOTH, WAITFORSNAPSHOTTOKEN;
}

enum FishPosition{
	HERE, LEFT, RIGHT;
}

public class TankModel extends Observable implements Iterable<FishModel> {

	public static final int WIDTH = 600;
	public static final int HEIGHT = 350;
	protected static final int MAX_FISHIES = 5;
	protected static final Random rand = new Random();
	protected volatile String id;
	protected final Set<FishModel> fishies;
	protected int fishCounter = 0;
	protected final ClientCommunicator.ClientForwarder forwarder;
	protected AtomicBoolean hasToken = new AtomicBoolean();
	//snapshot
	protected RecordMode mode = RecordMode.IDLE;
	protected int localState = 0; //number of fishies
	protected boolean initiateToken = false;
	protected SnapshotToken snapshotToken = null;
	//Namensdienste
	protected ConcurrentHashMap<String, FishPosition> fishPosition = new ConcurrentHashMap<>();


	public void setLeftNeighbor(InetSocketAddress leftNeighbor) {
		this.leftNeighbor = leftNeighbor;
	}

	public void setRightNeighbor(InetSocketAddress rightNeighbor) {
		this.rightNeighbor = rightNeighbor;
	}

	protected InetSocketAddress leftNeighbor = null;
	protected InetSocketAddress rightNeighbor = null;

	public TankModel(ClientCommunicator.ClientForwarder forwarder) {
		this.fishies = Collections.newSetFromMap(new ConcurrentHashMap<FishModel, Boolean>());
		this.forwarder = forwarder;
	}

	synchronized void onRegistration(String id) {
		this.id = id;
		newFish(WIDTH - FishModel.getXSize(), rand.nextInt(HEIGHT - FishModel.getYSize()));
	}

	public synchronized void newFish(int x, int y) {
		if (fishies.size() < MAX_FISHIES) {
			x = x > WIDTH - FishModel.getXSize() - 1 ? WIDTH - FishModel.getXSize() - 1 : x;
			y = y > HEIGHT - FishModel.getYSize() ? HEIGHT - FishModel.getYSize() : y;

			FishModel fish = new FishModel("fish" + (++fishCounter) + "@" + getId(), x, y,
					rand.nextBoolean() ? Direction.LEFT : Direction.RIGHT);

			fishies.add(fish);
			fishPosition.put(fish.getId(), FishPosition.HERE);
		}
	}

	public AtomicBoolean hasToken() {
		return hasToken;
	}

	synchronized void receiveFish(FishModel fish) {
		if(mode == RecordMode.BOTH){
			localState++;
		}
		if(mode == RecordMode.LEFT && fish.getDirection() == Direction.RIGHT){
			localState++;
		}
		if(mode == RecordMode.RIGHT && fish.getDirection() == Direction.LEFT){
			localState++;
		}
		fish.setToStart();
		fishies.add(fish);
		fishPosition.put(fish.getId(), FishPosition.HERE);
	}

	public String getId() {
		return id;
	}

	public synchronized int getFishCounter() {
		return fishCounter;
	}

	public synchronized Iterator<FishModel> iterator() {
		return fishies.iterator();
	}

	private synchronized void updateFishies() {
		for (Iterator<FishModel> it = iterator(); it.hasNext();) {
			FishModel fish = it.next();

			fish.update();

			if (fish.hitsEdge())
				if (hasToken.get()){

					if (fish.getDirection() == Direction.LEFT) {
						fishPosition.put(fish.getId(), FishPosition.LEFT);
						forwarder.handOff(fish, leftNeighbor);
					} else if (fish.getDirection() == Direction.RIGHT) {
						fishPosition.put(fish.getId(), FishPosition.RIGHT);
						forwarder.handOff(fish, rightNeighbor);
					}
				} else {
					fish.reverse();
				}
			if (fish.disappears())
				it.remove();
		}
	}

	private synchronized void update() {
		updateFishies();
		setChanged();
		notifyObservers();
	}
	protected void setToken() {
		hasToken.set(true);
		new java.util.Timer().schedule(
			new java.util.TimerTask() {
			@Override
			public void run() {
				hasToken.set(false);
				forwarder.Token(rightNeighbor);
			}
		}, 2000);
	}

	protected void run() {
		forwarder.register();

		try {
			while (!Thread.currentThread().isInterrupted()) {
				update();
				TimeUnit.MILLISECONDS.sleep(10);
			}
		} catch (InterruptedException consumed) {
			// allow method to terminate
		}
	}

	public synchronized void finish() {
		if(hasToken.get())
		{
			forwarder.Token(rightNeighbor);
		}
		forwarder.deregister(id);
	}

    public void initiateSnapshot(InetSocketAddress sender, boolean initiateToken) {
		if(this.initiateToken != true){
			this.initiateToken = initiateToken;
		}

		if (mode == RecordMode.IDLE) {
			if (sender == null) {
				System.out.println("snapshot init");
				mode = RecordMode.BOTH;
				localState = fishCounter;
				snapshotToken = new SnapshotToken();
				forwarder.snapshotMarker(rightNeighbor, leftNeighbor);
			}else {
				if (sender.equals(rightNeighbor)) {
					System.out.println("right neighbour send snapshotmarker");
					mode = RecordMode.LEFT;
					localState = fishCounter;
					forwarder.snapshotMarker(rightNeighbor, leftNeighbor);
				}
				if (sender.equals(leftNeighbor)) {
					System.out.println("left neighbour send snapshotmarker");
					mode = RecordMode.RIGHT;
					localState = fishCounter;
					forwarder.snapshotMarker(rightNeighbor, leftNeighbor);
				}
			}
		}
		if (sender != null) {


			if (mode == RecordMode.BOTH && sender.equals(leftNeighbor)) {
				mode = RecordMode.RIGHT;
			}
			if (mode == RecordMode.BOTH && sender.equals(rightNeighbor)) {
				mode = RecordMode.LEFT;
			}

			if (mode == RecordMode.LEFT && sender.equals(leftNeighbor)) {
				System.out.println("Gezählte Fische: " + localState);
				mode = RecordMode.WAITFORSNAPSHOTTOKEN;
				snapshotToken.addFishies(localState);
				forwarder.snapshotToken(leftNeighbor, snapshotToken);
				//snapshot done
			}
			if (mode == RecordMode.RIGHT && sender.equals(rightNeighbor)) {
				System.out.println("Gezählte Fische: " + localState);
				mode = RecordMode.WAITFORSNAPSHOTTOKEN;
				snapshotToken.addFishies(localState);
				forwarder.snapshotToken(leftNeighbor, snapshotToken);
				//snapshot done
			}
		}

    }
    public void locateFishGlobally (String fishId){
		//if(fishPosition.containsKey(fishId)){
		//}
        System.out.println(fishPosition);
        if(fishPosition.get(fishId).equals(FishPosition.HERE))
        {
            locateFishLocally(fishId);
        }
        if(fishPosition.get(fishId).equals(FishPosition.LEFT)){
            forwarder.sendLocationRequest(leftNeighbor, fishId);
        }
        if(fishPosition.get(fishId).equals(FishPosition.RIGHT)){
            forwarder.sendLocationRequest(rightNeighbor, fishId);
        }
	}
	private void locateFishLocally(String fishId){
	    for ( FishModel f:fishies){
	        if(f.getId().equals(fishId)){
	            f.toggle();
            }
        }
    }

}