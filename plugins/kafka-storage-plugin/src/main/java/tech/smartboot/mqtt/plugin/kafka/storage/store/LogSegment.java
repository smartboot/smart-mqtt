package tech.smartboot.mqtt.plugin.kafka.storage.store;

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class LogSegment {
    private static final byte RECORD_VERSION = 1;
    private static final byte FLAG_RETAINED = 1;

    private final Path path;
    private final long baseOffset;
    private final FileChannel channel;
    private final List<Long> offsets = new ArrayList<>();
    private final List<Long> positions = new ArrayList<>();

    private long sizeInBytes;
    private long lastAppendTime = System.currentTimeMillis();
    private boolean dirty;

    LogSegment(Path path, long baseOffset) throws IOException {
        this.path = path;
        this.baseOffset = baseOffset;
        this.channel = FileChannel.open(path,
                StandardOpenOption.CREATE,
                StandardOpenOption.READ,
                StandardOpenOption.WRITE);
        load();
    }

    private void load() throws IOException {
        long position = 0;
        ByteBuffer sizeBuffer = ByteBuffer.allocate(4);
        long channelSize = channel.size();
        while (position + 4 <= channelSize) {
            sizeBuffer.clear();
            readFully(position, sizeBuffer);
            sizeBuffer.flip();
            int recordSize = sizeBuffer.getInt();
            if (recordSize <= 0 || position + 4L + recordSize > channelSize) {
                break;
            }
            ByteBuffer recordBuffer = ByteBuffer.allocate(recordSize);
            readFully(position + 4, recordBuffer);
            recordBuffer.flip();
            StoredMessage message = deserialize(recordBuffer, null, 0);
            offsets.add(message.getOffset());
            positions.add(position);
            position += 4L + recordSize;
        }
        sizeInBytes = position;
        channel.position(sizeInBytes);
    }

    long getBaseOffset() {
        return baseOffset;
    }

    long getSizeInBytes() {
        return sizeInBytes;
    }

    boolean isDirty() {
        return dirty;
    }

    int recordCount() {
        return offsets.size();
    }

    long firstOffset() {
        return offsets.isEmpty() ? baseOffset : offsets.get(0);
    }

    long lastOffset() {
        return offsets.isEmpty() ? baseOffset - 1 : offsets.get(offsets.size() - 1);
    }

    synchronized AppendResult append(StoredMessage message) throws IOException {
        ByteBuffer serialized = serialize(message);
        int written = serialized.remaining();
        long position = sizeInBytes;
        channel.position(position);
        while (serialized.hasRemaining()) {
            channel.write(serialized);
        }
        offsets.add(message.getOffset());
        positions.add(position);
        sizeInBytes += written;
        lastAppendTime = System.currentTimeMillis();
        dirty = true;
        return new AppendResult(message, written);
    }

    synchronized List<StoredMessage> readFromOffset(String topic,
                                                    int partition,
                                                    long offset,
                                                    int maxBytes,
                                                    int maxRecords) throws IOException {
        if (offsets.isEmpty()) {
            return Collections.emptyList();
        }
        int startIndex = findStartIndex(offset);
        if (startIndex < 0 || startIndex >= offsets.size()) {
            return Collections.emptyList();
        }
        List<StoredMessage> messages = new ArrayList<>();
        int bytes = 0;
        for (int i = startIndex; i < offsets.size(); i++) {
            StoredMessage message = readMessageAt(topic, partition, positions.get(i));
            int messageBytes = sizeOf(message);
            if (!messages.isEmpty() && (bytes + messageBytes > maxBytes || messages.size() >= maxRecords)) {
                break;
            }
            messages.add(message);
            bytes += messageBytes;
            if (messages.size() >= maxRecords) {
                break;
            }
        }
        return messages;
    }

    synchronized OffsetLookupResult findOffsetByTimestamp(String topic, int partition, long timestamp) throws IOException {
        for (int i = 0; i < positions.size(); i++) {
            StoredMessage message = readMessageAt(topic, partition, positions.get(i));
            if (message.getTimestamp() >= timestamp) {
                return new OffsetLookupResult(message.getOffset(), message.getTimestamp());
            }
        }
        return new OffsetLookupResult(lastOffset() + 1, -1);
    }

    synchronized void force() throws IOException {
        channel.force(true);
        dirty = false;
    }

    long lastAppendTime() {
        return lastAppendTime;
    }

    synchronized void close() throws IOException {
        channel.close();
    }

    synchronized void delete() throws IOException {
        close();
        Files.deleteIfExists(path);
    }

    private int findStartIndex(long targetOffset) {
        int low = 0;
        int high = offsets.size() - 1;
        int result = offsets.size();
        while (low <= high) {
            int mid = (low + high) >>> 1;
            long value = offsets.get(mid);
            if (value >= targetOffset) {
                result = mid;
                high = mid - 1;
            } else {
                low = mid + 1;
            }
        }
        return result;
    }

    private StoredMessage readMessageAt(String topic, int partition, long position) throws IOException {
        ByteBuffer sizeBuffer = ByteBuffer.allocate(4);
        readFully(position, sizeBuffer);
        sizeBuffer.flip();
        int size = sizeBuffer.getInt();
        ByteBuffer recordBuffer = ByteBuffer.allocate(size);
        readFully(position + 4, recordBuffer);
        recordBuffer.flip();
        return deserialize(recordBuffer, topic, partition);
    }

    private void readFully(long position, ByteBuffer buffer) throws IOException {
        while (buffer.hasRemaining()) {
            int read = channel.read(buffer, position + buffer.position());
            if (read < 0) {
                throw new EOFException("unexpected end of segment: " + path);
            }
        }
    }

    private static ByteBuffer serialize(StoredMessage message) {
        byte[] clientIdBytes = message.getSourceClientId() == null ? null : message.getSourceClientId().getBytes(StandardCharsets.UTF_8);
        byte[] keyBytes = message.getKey();
        byte[] valueBytes = message.getValue();
        int payloadSize = 1 + 1 + 2 + 1 + 8 + 8 + 4 + 4 + 4
                + length(clientIdBytes)
                + length(keyBytes)
                + length(valueBytes);
        ByteBuffer buffer = ByteBuffer.allocate(4 + payloadSize);
        buffer.putInt(payloadSize);
        buffer.put(RECORD_VERSION);
        buffer.put(message.getSourceType());
        buffer.putShort(message.getQos());
        buffer.put((byte) (message.isRetained() ? FLAG_RETAINED : 0));
        buffer.putLong(message.getOffset());
        buffer.putLong(message.getTimestamp());
        putBytes(buffer, clientIdBytes);
        putBytes(buffer, keyBytes);
        putBytes(buffer, valueBytes);
        buffer.flip();
        return buffer;
    }

    private static StoredMessage deserialize(ByteBuffer buffer, String topic, int partition) {
        byte version = buffer.get();
        if (version != RECORD_VERSION) {
            throw new IllegalStateException("unsupported record version: " + version);
        }
        byte sourceType = buffer.get();
        short qos = buffer.getShort();
        boolean retained = (buffer.get() & FLAG_RETAINED) == FLAG_RETAINED;
        long offset = buffer.getLong();
        long timestamp = buffer.getLong();
        String sourceClientId = toString(readBytes(buffer));
        byte[] key = readBytes(buffer);
        byte[] value = readBytes(buffer);
        return new StoredMessage(topic, partition, offset, timestamp, key, value, qos, retained, sourceClientId, sourceType);
    }

    private static int length(byte[] bytes) {
        return bytes == null ? 0 : bytes.length;
    }

    private static void putBytes(ByteBuffer buffer, byte[] bytes) {
        if (bytes == null) {
            buffer.putInt(-1);
        } else {
            buffer.putInt(bytes.length);
            buffer.put(bytes);
        }
    }

    private static byte[] readBytes(ByteBuffer buffer) {
        int length = buffer.getInt();
        if (length < 0) {
            return null;
        }
        byte[] bytes = new byte[length];
        buffer.get(bytes);
        return bytes;
    }

    private static String toString(byte[] bytes) {
        return bytes == null ? null : new String(bytes, StandardCharsets.UTF_8);
    }

    static int sizeOf(StoredMessage message) {
        return 4 + 1 + 1 + 2 + 1 + 8 + 8 + 4 + 4 + 4
                + length(message.getSourceClientId() == null ? null : message.getSourceClientId().getBytes(StandardCharsets.UTF_8))
                + length(message.getKey())
                + length(message.getValue());
    }

    static final class AppendResult {
        private final StoredMessage message;
        private final int writtenBytes;

        AppendResult(StoredMessage message, int writtenBytes) {
            this.message = message;
            this.writtenBytes = writtenBytes;
        }

        StoredMessage getMessage() {
            return message;
        }

        int getWrittenBytes() {
            return writtenBytes;
        }
    }

    static final class OffsetLookupResult {
        private final long offset;
        private final long timestamp;

        OffsetLookupResult(long offset, long timestamp) {
            this.offset = offset;
            this.timestamp = timestamp;
        }

        long getOffset() {
            return offset;
        }

        long getTimestamp() {
            return timestamp;
        }
    }
}
