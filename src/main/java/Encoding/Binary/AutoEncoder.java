package Encoding.Binary;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.SneakyThrows;
import lombok.ToString;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.*;

@EqualsAndHashCode
@AllArgsConstructor
class Primitive {
    EncodableType type;
    int size;
}

@ToString
@AllArgsConstructor
class Encodable {
    EncodableType type;
    Field f;
}

public class AutoEncoder<T> {
    Class<T> clazz;
    private static final Map<Class<?>, Primitive> primitives = new HashMap<>();

    List<Field> valid;
    List<Encodable> encodable = new ArrayList<>();

    static {
        primitives.put(Long.class, new Primitive(EncodableType.LONG, 8));
        primitives.put(long.class, new Primitive(EncodableType.LONG, 8));
        primitives.put(Integer.class, new Primitive(EncodableType.INT, 4));
        primitives.put(int.class, new Primitive(EncodableType.INT, 4));
        primitives.put(Short.class, new Primitive(EncodableType.SHORT, 2));
        primitives.put(short.class, new Primitive(EncodableType.SHORT, 2));
        primitives.put(Character.class, new Primitive(EncodableType.CHAR, 1));
        primitives.put(char.class, new Primitive(EncodableType.CHAR, 1));
        primitives.put(Byte.class, new Primitive(EncodableType.BYTE, 1));
        primitives.put(byte.class, new Primitive(EncodableType.BYTE, 1));
        primitives.put(Boolean.class, new Primitive(EncodableType.BOOLEAN, 1));
        primitives.put(boolean.class, new Primitive(EncodableType.BOOLEAN, 1));

    }

    private static final int STRING_HEADER_LEN = 4;

    public AutoEncoder(Class<T> clazz) {
        this.clazz = clazz;
        valid = new ArrayList<>();
        for (Field f : clazz.getDeclaredFields()) {
            if (f.getAnnotation(Serialize.class) != null)
                valid.add(f);
        }
        for (Field f : valid) {
            if (String.class.isAssignableFrom(f.getType())) {
                encodable.add(new Encodable(EncodableType.STRING, f));
            } else if (BinaryEncodable.class.isAssignableFrom(f.getType())) {
                encodable.add(new Encodable(EncodableType.ENCODABLE, f));
            } else if (f.getType().isPrimitive()) {
                if (primitives.containsKey(f.getType())) {
                    encodable.add(new Encodable(primitives.get(f.getType()).type, f));
                } else {
                    throw new UnsupportedOperationException("SIZE OF PRIMITIVE NOT IN MAP:\t" + f.getType());
                }
            } else {
                throw new UnsupportedOperationException("CANNOT ENCODE/DECODE:\t" + f.getType());
            }
        }
    }

    public ByteBuffer allocate(T o) {
        return ByteBuffer.allocate(getSize(o));
    }

    @SneakyThrows
    public ByteBuffer encode(T o, ByteBuffer buffer) {
        for (var t : encodable) {
            Field f = t.f;
            switch (t.type) {
                case LONG -> {
                    buffer.putLong((long) f.get(o));
                }
                case INT -> {
                    buffer.putInt((int) f.get(o));
                }
                case SHORT -> {
                    buffer.putShort((short) f.get(o));
                }
                case CHAR -> {
                    buffer.put((byte) ((char) f.get(o)));
                }
                case BYTE -> {
                    buffer.put((byte) f.get(o));
                }
                case BOOLEAN -> {
                    buffer.put(((byte) f.get(o) > 0) ? (byte) 1 : 0);
                }
                case STRING -> {
                    String s = (String) f.get(o);
                    buffer.putInt(s.length());
                    buffer.put(s.getBytes(StandardCharsets.UTF_8));
                }
                case ENCODABLE -> {
                    BinaryEncodable s = (BinaryEncodable) f.get(o);
                    s.getEncoder().encode(s, buffer);
                }
            }
        }
        return buffer;
    }

    @SneakyThrows
    public void decode(T o, ByteBuffer buffer) {
        for (var t : encodable) {
            Field f = t.f;
            switch (t.type) {
                case LONG -> {
                    f.set(o, buffer.getLong());
                }
                case INT -> {
                    f.set(o, buffer.getInt());
                }
                case SHORT -> {
                    f.set(o, buffer.getShort());
                }
                case CHAR -> {
                    f.set(o, buffer.get());
                }
                case BYTE -> {
                    f.set(o, buffer.get());
                }
                case BOOLEAN -> {
                    f.set(o, buffer.get() > 0);
                }
                case STRING -> {
                    int l = buffer.getInt();
                    byte[] bytes = new byte[l];
                    buffer.get(bytes);
                    String s = new String(bytes);
                    f.set(o, s);
                }
                case ENCODABLE -> {
                    Object child = f.getType().getConstructor(ByteBuffer.class).newInstance(buffer);
                    f.set(o, child);
                }
            }
        }
    }

    @SneakyThrows
    int getSize(T o) {
        int size = 0;
        for (Field f : valid) {
            if (!f.isAnnotationPresent(Serialize.class)) {
                continue;
            }
            if (String.class.isAssignableFrom(f.getType())) {
                size += STRING_HEADER_LEN + ((String) f.get(o)).length();
            } else if (BinaryEncodable.class.isAssignableFrom(f.getType())) {
                size += ((BinaryEncodable) f.get(o)).getEncoder().getSize(f.get(o));
            } else if (f.getType().isPrimitive()) {
                if (primitives.containsKey(f.getType())) {
                    size += primitives.get(f.getType()).size;
                } else {
                    throw new UnsupportedOperationException("SIZE OF PRIMITIVE NOT IN MAP:\t" + f.getType());
                }
            } else {
                throw new UnsupportedOperationException("CANNOT ENCODE/DECODE:\t" + f.getType());
            }
        }
        return size;
    }
}
