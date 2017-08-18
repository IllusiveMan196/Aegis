package me.impy.aegis.crypto.slots;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.crypto.SecretKey;

import me.impy.aegis.crypto.CryptoUtils;
import me.impy.aegis.util.LittleByteBuffer;

public class SlotCollection implements Iterable<Slot>, Serializable {
    private List<Slot> _slots = new ArrayList<>();
    private byte[] _masterHash;

    public static byte[] serialize(SlotCollection slots) {
        // yep, no streams at this api level
        int size = 0;
        for (Slot slot : slots) {
            size += slot.getSize();
        }
        size += CryptoUtils.CRYPTO_HASH_SIZE;

        LittleByteBuffer buffer = LittleByteBuffer.allocate(size);
        buffer.put(slots.getMasterHash());

        for (Slot slot : slots) {
            byte[] bytes = slot.serialize();
            buffer.put(bytes);
        }
        return buffer.array();
    }

    public static SlotCollection deserialize(byte[] data) throws Exception {
        LittleByteBuffer buffer = LittleByteBuffer.wrap(data);
        byte[] masterHash = new byte[CryptoUtils.CRYPTO_HASH_SIZE];
        buffer.get(masterHash);

        SlotCollection slots = new SlotCollection();
        slots.setMasterHash(masterHash);

        while (buffer.remaining() > 0) {
            Slot slot;

            switch (buffer.peek()) {
                case Slot.TYPE_RAW:
                    slot = new RawSlot();
                    break;
                case Slot.TYPE_DERIVED:
                    slot = new PasswordSlot();
                    break;
                case Slot.TYPE_FINGERPRINT:
                    slot = new FingerprintSlot();
                    break;
                default:
                    throw new Exception("unrecognized slot type");
            }

            byte[] bytes = new byte[slot.getSize()];
            buffer.get(bytes);

            slot.deserialize(bytes);
            slots.add(slot);
        }

        return slots;
    }

    public void add(Slot slot) {
        _slots.add(slot);
    }

    public void remove(Slot slot) {
        _slots.remove(slot);
    }

    public int size() {
        return _slots.size();
    }

    public <T extends Slot> T find(Class<T> type) {
        for (Slot slot : this) {
            if (slot.getClass() == type) {
                return type.cast(slot);
            }
        }
        return null;
    }

    public <T extends Slot> boolean has(Class<T> type) {
        return find(type) != null;
    }

    @Override
    public Iterator<Slot> iterator() {
        return _slots.iterator();
    }

    public void setMasterHash(byte[] masterHash) {
        _masterHash = masterHash;
    }

    public byte[] getMasterHash() {
        return _masterHash;
    }
}