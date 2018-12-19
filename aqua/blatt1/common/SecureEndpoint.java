package aqua.blatt1.common;

import messaging.Endpoint;
import messaging.Message;

import javax.crypto.Cipher;
import javax.crypto.SealedObject;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.net.InetSocketAddress;


public class SecureEndpoint extends Endpoint {
    private SecretKeySpec key;
    private Cipher encrypt;
    private Cipher decrypt;
    ByteArrayOutputStream bos = new ByteArrayOutputStream();

    public SecureEndpoint (){
        super();
        try{
            key =  new SecretKeySpec("CAFEBABECAFEBABE".getBytes("UTF-8"),"AES");
            encrypt = Cipher.getInstance("AES");
            encrypt.init(Cipher.ENCRYPT_MODE,key);
            decrypt = Cipher.getInstance("AES");
            decrypt.init(Cipher.DECRYPT_MODE,key);
        } catch (Exception e) {
            System.out.print(e);
        }
    }

    public SecureEndpoint (int port){
        super(port);
        try{
            key =  new SecretKeySpec("CAFEBABECAFEBABE".getBytes("UTF-8"),"AES");
            encrypt = Cipher.getInstance("AES");
            encrypt.init(Cipher.ENCRYPT_MODE,key);
            decrypt = Cipher.getInstance("AES");
            decrypt.init(Cipher.DECRYPT_MODE,key);
        } catch (Exception e) {
            System.out.print(e);
        }
    }

    public void send(InetSocketAddress receiver, Serializable payload){

        try{
            super.send(receiver,new SealedObject(payload,encrypt));
        } catch (Exception e) {
            System.out.print(e);
        }
    }

    public Message blockingReceive() {
        Message message = super.blockingReceive();
        SealedObject so = (SealedObject) message.getPayload();
        Message ret = null;
        try {
            ret = new Message((Serializable)so.getObject(decrypt),message.getSender());
        } catch (Exception e){
            System.out.print(e);
        }
        return ret;
    }
}
