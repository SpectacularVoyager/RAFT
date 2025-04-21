package Encoding.Binary;

import java.nio.ByteBuffer;

public class Encoder <T>{
    Class<T> clazz;
    int len=0;
    public Encoder(Class<T> clazz){
        this.clazz=clazz;

    }

    public void encoder(ByteBuffer buffer,T o){

    }
    public void decode(ByteBuffer buffer,T o){

    }
}
