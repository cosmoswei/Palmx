package me.xuqu.palmx.serialize;

import me.xuqu.palmx.common.SerializationType;
import me.xuqu.palmx.serialize.impl.JavaSerialization;
import me.xuqu.palmx.serialize.impl.JsonSerialization;
import me.xuqu.palmx.serialize.impl.KryoSerialization;
import me.xuqu.palmx.serialize.impl.ProtostuffSerialization;

public interface Serialization {

    <T> byte[] serialize(T t);

    <T> T deserialize(Class<T> tClass, byte[] bytes);

    static <T> byte[] serialize(byte idx, T t) {
        SerializationType type = SerializationType.values()[idx];
        Object serializedObject = null;
        switch (type) {
            case JAVA:
                serializedObject = java().serialize(t);
                break;
            case JSON:
                serializedObject = json().serialize(t);
                break;
            case KRYO:
                serializedObject = kryo().serialize(t);
                break;
            case PROTOSTUFF:
                serializedObject = protostuff().serialize(t);
                break;
            default:
                throw new IllegalArgumentException("Unknown serialization type: " + type);
        }
        return (byte[]) serializedObject;
    }


    static <T> T deserialize(byte idx, Class<T> tClass, byte[] bytes) {
        Object deserializedObject = null;
        SerializationType type = SerializationType.values()[idx];

        switch (type) {
            case JAVA:
                deserializedObject = java().deserialize(tClass, bytes);
                break;
            case JSON:
                deserializedObject = json().deserialize(tClass, bytes);
                break;
            case KRYO:
                deserializedObject = kryo().deserialize(tClass, bytes);
                break;
            case PROTOSTUFF:
                deserializedObject = protostuff().deserialize(tClass, bytes);
                break;
            default:
                // 处理未知的序列化类型，例如抛出异常
                throw new IllegalArgumentException("Unknown serialization type: " + type);
        }
        return (T) deserializedObject;
    }

    class Holder {
        static final Serialization javaSerialization = new JavaSerialization();
        static final Serialization jsonSerialization = new JsonSerialization();
        static final Serialization kryoSerialization = new KryoSerialization();
        static final Serialization protostuffSerialization = new ProtostuffSerialization();
    }

    static Serialization java() {
        return Holder.javaSerialization;
    }

    static Serialization json() {
        return Holder.jsonSerialization;
    }

    static Serialization kryo() {
        return Holder.kryoSerialization;
    }

    static Serialization protostuff() {
        return Holder.protostuffSerialization;
    }
}
