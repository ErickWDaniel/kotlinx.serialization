package kotlinx.serialization.json.internal

import java.io.OutputStream
import java.io.Writer
import java.nio.charset.Charset


internal class JsonToWriterStringBuilder(private val writer: Writer) : JsonStringBuilder(
    // maybe this can also be taken from the pool, but currently initial char array size there is 128, which is too low.
    CharArray(BATCH_SIZE)
) {
    constructor(os: OutputStream, charset: Charset): this(os.writer(charset).buffered(READER_BUF_SIZE))

    override fun ensureTotalCapacity(oldSize: Int, additional: Int): Int {
        val requiredSize = oldSize + additional
        val currentSize = array.size
        if (currentSize <= requiredSize) {
            dumpAndReset(oldSize)
            if (additional > currentSize) {
                // Handle strings that are longer than buffer:
                // Ideally, we should make `ensureAdditionalCapacity` return boolean and fall back
                // to per-symbol path in appendQuoted on large strings,
                // but this approach is adequate for current stage, too.
                array = CharArray(requiredSize.coerceAtLeast(currentSize * 2))
            }
            return 0
        }
        return oldSize
    }

    private fun dumpAndReset(sz: Int = size) {
        writer.write(array, 0, sz)
        size = 0
    }

    override fun release() {
        dumpAndReset()
        writer.flush()
    }
}

