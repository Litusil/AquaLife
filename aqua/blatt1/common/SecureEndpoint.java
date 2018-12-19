package aqua.blatt1.common;

import aqua.blatt1.common.msgtypes.KeyExchangeMessage;
import messaging.Endpoint;
import messaging.Message;

import javax.crypto.Cipher;
import javax.crypto.SealedObject;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.net.InetSocketAddress;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PublicKey;
import java.util.HashMap;
import java.util.Map;


public class SecureEndpoint extends Endpoint {
    private Cipher encrypt;
    private Cipher decrypt;


    Map<InetSocketAddress, PublicKey> keymanager = new HashMap<>();

    KeyPairGenerator keyPairGenerator;
    KeyPair keyPair;

    public SecureEndpoint (){
        super();
        try{
            keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(4096);
            keyPair = keyPairGenerator.generateKeyPair();
        } catch (Exception e) {
            System.out.print(e);
        }
    }

    public SecureEndpoint (int port){
        super(port);
        try{
            keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(4096);
            keyPair = keyPairGenerator.generateKeyPair();
        } catch (Exception e) {
            System.out.print(e);
        }
    }

    public void send(InetSocketAddress receiver, Serializable payload){

        try{
            if(!keymanager.containsKey(receiver)){
                super.send(receiver, new KeyExchangeMessage(null));
                while(!keymanager.containsKey(receiver)){
                    System.out.println("spin to win");
                }
            }
            PublicKey pubKey = keymanager.get(receiver);
            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.ENCRYPT_MODE, pubKey);
            super.send(receiver,new SealedObject(payload,cipher));
        } catch (Exception e) {
            System.out.print(e);
        }
    }

    public Message blockingReceive() {
        Message message = super.blockingReceive();
        if(message.getPayload() instanceof KeyExchangeMessage){

            System.out.println("KeyExchangeMessage");
            KeyExchangeMessage keyReq = (KeyExchangeMessage)message.getPayload();
            if(keyReq.getKey()==null){
                super.send(message.getSender(),new KeyExchangeMessage(keyPair.getPublic()));
            } else {
                keymanager.put(message.getSender(),keyReq.getKey());
            }
            return null;
        }

        SealedObject so = (SealedObject) message.getPayload();
        Message ret = null;

        try {
            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.DECRYPT_MODE, keyPair.getPrivate());
            ret = new Message((Serializable)so.getObject(cipher),message.getSender());
            System.out.println("keine KeyExchangeMessage");
        } catch (Exception e){
            System.out.print("Error no KeyExchange" + e);
        }
        return ret;
    }
}
