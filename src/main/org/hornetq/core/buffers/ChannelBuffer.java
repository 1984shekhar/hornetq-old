/*
 * Copyright 2009 Red Hat, Inc.
 * Red Hat licenses this file to you under the Apache License, version
 * 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *    http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */

package org.hornetq.core.buffers;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.GatheringByteChannel;
import java.nio.channels.ScatteringByteChannel;
import java.nio.charset.UnsupportedCharsetException;

import org.hornetq.core.remoting.spi.HornetQBuffer;

/**
 * A random and sequential accessible sequence of zero or more bytes (octets).
 * This interface provides an abstract view for one or more primitive byte
 * arrays ({@code byte[]}) and {@linkplain ByteBuffer NIO buffers}.
 *
 * <h3>Creation of a buffer</h3>
 *
 * It is recommended to create a new buffer using the helper methods in
 * {@link ChannelBuffers} rather than calling an individual implementation's
 * constructor.
 *
 * <h3>Random Access Indexing</h3>
 *
 * Just like an ordinary primitive byte array, {@link ChannelBuffer} uses
 * <a href="http://en.wikipedia.org/wiki/Index_(information_technology)#Array_element_identifier">zero-based indexing</a>.
 * It means the index of the first byte is always {@code 0} and the index of
 * the last byte is always {@link #capacity() capacity - 1}.  For example, to
 * iterate all bytes of a buffer, you can do the following, regardless of
 * its internal implementation:
 *
 * <pre>
 * ChannelBuffer buffer = ...;
 * for (int i = 0; i &lt; buffer.capacity(); i ++</strong>) {
 *     byte b = array.getByte(i);
 *     System.out.println((char) b);
 * }
 * </pre>
 *
 * <h3>Sequential Access Indexing</h3>
 *
 * {@link ChannelBuffer} provides two pointer variables to support sequential
 * read and write operations - {@link #readerIndex() readerIndex} for a read
 * operation and {@link #writerIndex() writerIndex} for a write operation
 * respectively.  The following diagram shows how a buffer is segmented into
 * three areas by the two pointers:
 *
 * <pre>
 *      +-------------------+------------------+------------------+
 *      | discardable bytes |  readable bytes  |  writable bytes  |
 *      |                   |     (CONTENT)    |                  |
 *      +-------------------+------------------+------------------+
 *      |                   |                  |                  |
 *      0      <=      readerIndex   <=   writerIndex    <=    capacity
 * </pre>
 *
 * <h4>Readable bytes (the actual content)</h4>
 *
 * This segment is where the actual data is stored.  Any operation whose name
 * starts with {@code read} or {@code skip} will get or skip the data at the
 * current {@link #readerIndex() readerIndex} and increase it by the number of
 * read bytes.  If the argument of the read operation is also a
 * {@link ChannelBuffer} and no start index is specified, the specified
 * buffer's {@link #readerIndex() readerIndex} is increased together.
 * <p>
 * If there's not enough content left, {@link IndexOutOfBoundsException} is
 * raised.  The default value of newly allocated, wrapped or copied buffer's
 * {@link #readerIndex() readerIndex} is {@code 0}.
 *
 * <pre>
 * // Iterates the readable bytes of a buffer.
 * ChannelBuffer buffer = ...;
 * while (buffer.readable()) {
 *     System.out.println(buffer.readByte());
 * }
 * </pre>
 *
 * <h4>Writable bytes</h4>
 *
 * This segment is a undefined space which needs to be filled.  Any operation
 * whose name ends with {@code write} will write the data at the current
 * {@link #writerIndex() writerIndex} and increase it by the number of written
 * bytes.  If the argument of the write operation is also a {@link ChannelBuffer},
 * and no start index is specified, the specified buffer's
 * {@link #readerIndex() readerIndex} is increased together.
 * <p>
 * If there's not enough writable bytes left, {@link IndexOutOfBoundsException}
 * is raised.  The default value of newly allocated buffer's
 * {@link #writerIndex() writerIndex} is {@code 0}.  The default value of
 * wrapped or copied buffer's {@link #writerIndex() writerIndex} is the
 * {@link #capacity() capacity} of the buffer.
 *
 * <pre>
 * // Fills the writable bytes of a buffer with random integers.
 * ChannelBuffer buffer = ...;
 * while (buffer.writableBytes() >= 4) {
 *     buffer.writeInt(random.nextInt());
 * }
 * </pre>
 *
 * <h4>Discardable bytes</h4>
 *
 * This segment contains the bytes which were read already by a read operation.
 * Initially, the size of this segment is {@code 0}, but its size increases up
 * to the {@link #writerIndex() writerIndex} as read operations are executed.
 * The read bytes can be discarded by calling {@link #discardReadBytes()} to
 * reclaim unused area as depicted by the following diagram:
 *
 * <pre>
 *  BEFORE discardReadBytes()
 *
 *      +-------------------+------------------+------------------+
 *      | discardable bytes |  readable bytes  |  writable bytes  |
 *      +-------------------+------------------+------------------+
 *      |                   |                  |                  |
 *      0      <=      readerIndex   <=   writerIndex    <=    capacity
 *
 *
 *  AFTER discardReadBytes()
 *
 *      +------------------+--------------------------------------+
 *      |  readable bytes  |    writable bytes (got more space)   |
 *      +------------------+--------------------------------------+
 *      |                  |                                      |
 * readerIndex (0) <= writerIndex (decreased)        <=        capacity
 * </pre>
 *
 * <h4>Clearing the buffer indexes</h4>
 *
 * You can set both {@link #readerIndex() readerIndex} and
 * {@link #writerIndex() writerIndex} to {@code 0} by calling {@link #clear()}.
 * It does not clear the buffer content (e.g. filling with {@code 0}) but just
 * clears the two pointers.  Please also note that the semantic of this
 * operation is different from {@link ByteBuffer#clear()}.
 *
 * <pre>
 *  BEFORE clear()
 *
 *      +-------------------+------------------+------------------+
 *      | discardable bytes |  readable bytes  |  writable bytes  |
 *      +-------------------+------------------+------------------+
 *      |                   |                  |                  |
 *      0      <=      readerIndex   <=   writerIndex    <=    capacity
 *
 *
 *  AFTER clear()
 *
 *      +---------------------------------------------------------+
 *      |             writable bytes (got more space)             |
 *      +---------------------------------------------------------+
 *      |                                                         |
 *      0 = readerIndex = writerIndex            <=            capacity
 * </pre>
 *
 * <h3>Search operations</h3>
 *
 * Various {@code indexOf()} methods help you locate an index of a value which
 * meets a certain criteria.  Complicated dynamic sequential search can be done
 * with {@link ChannelBufferIndexFinder} as well as simple static single byte
 * search.
 *
 * <h3>Mark and reset</h3>
 *
 * There are two marker indexes in every buffer. One is for storing
 * {@link #readerIndex() readerIndex} and the other is for storing
 * {@link #writerIndex() writerIndex}.  You can always reposition one of the
 * two indexes by calling a reset method.  It works in a similar fashion to
 * the mark and reset methods in {@link InputStream} except that there's no
 * {@code readlimit}.
 *
 * <h3>Derived buffers</h3>
 *
 * You can create a view of an existing buffer by calling either
 * {@link #duplicate()}, {@link #slice()} or {@link #slice(int, int)}.
 * A derived buffer will have an independent {@link #readerIndex() readerIndex},
 * {@link #writerIndex() writerIndex} and marker indexes, while it shares
 * other internal data representation, just like a NIO buffer does.
 * <p>
 * In case a completely fresh copy of an existing buffer is required, please
 * call {@link #copy()} method instead.
 *
 * <h3>Conversion to existing JDK types</h3>
 *
 * <h4>NIO Buffers</h4>
 *
 * Various {@link #toByteBuffer()} and {@link #toByteBuffers()} methods convert
 * a {@link ChannelBuffer} into one or more NIO buffers.  These methods avoid
 * buffer allocation and memory copy whenever possible, but there's no
 * guarantee that memory copy will not be involved or that an explicit memory
 * copy will be involved.
 *
 * <h4>Strings</h4>
 *
 * Various {@link #toString(String)} methods convert a {@link ChannelBuffer}
 * into a {@link String}.  Plesae note that {@link #toString()} is not a
 * conversion method.
 *
 * <h4>I/O Streams</h4>
 *
 * Please refer to {@link ChannelBufferInputStream} and
 * {@link ChannelBufferOutputStream}.
 *
 * @author The Netty Project (netty-dev@lists.jboss.org)
 * @author Trustin Lee (tlee@redhat.com)
 *
 * @version $Rev: 472 $, $Date: 2008-11-14 16:45:53 +0900 (Fri, 14 Nov 2008) $
 *
 * @apiviz.landmark
 */
public interface ChannelBuffer extends Comparable<ChannelBuffer>, HornetQBuffer
{

   /**
    * Returns the number of bytes (octets) this buffer can contain.
    */
   int capacity();

   byte[] array();

   /**
    * Returns the {@code readerIndex} of this buffer.
    */
   int readerIndex();

   /**
    * Sets the {@code readerIndex} of this buffer.
    *
    * @throws IndexOutOfBoundsException
    *         if the specified {@code readerIndex} is
    *            less than {@code 0} or
    *            greater than {@code this.writerIndex}
    */
   void readerIndex(int readerIndex);

   /**
    * Returns the {@code writerIndex} of this buffer.
    */
   int writerIndex();

   /**
    * Sets the {@code writerIndex} of this buffer.
    *
    * @throws IndexOutOfBoundsException
    *         if the specified {@code writerIndex} is
    *            less than {@code this.readerIndex} or
    *            greater than {@code this.capacity}
    */
   void writerIndex(int writerIndex);

   /**
    * Sets the {@code readerIndex} and {@code writerIndex} of this buffer
    * in one shot.  This method is useful when you have to worry about the
    * invocation order of {@link #readerIndex(int)} and {@link #writerIndex(int)}
    * methods.  For example, the following code will fail:
    *
    * <pre>
    * // Create a buffer whose readerIndex, writerIndex and capacity are
    * // 0, 0 and 8 respectively.
    * ChannelBuffer buf = ChannelBuffers.buffer(8);
    *
    * // IndexOutOfBoundsException is thrown because the specified
    * // readerIndex (2) cannot be greater than the current writerIndex (0).
    * buf.readerIndex(2);
    * buf.writerIndex(4);
    * </pre>
    *
    * The following code will also fail:
    *
    * <pre>
    * // Create a buffer whose readerIndex, writerIndex and capacity are
    * // 0, 8 and 8 respectively.
    * ChannelBuffer buf = ChannelBuffers.wrappedBuffer(new byte[8]);
    *
    * // readerIndex becomes 8.
    * buf.readLong();
    *
    * // IndexOutOfBoundsException is thrown because the specified
    * // writerIndex (4) cannot be less than the current readerIndex (8).
    * buf.writerIndex(4);
    * buf.readerIndex(2);
    * </pre>
    *
    * By contrast, {@link #setIndex(int, int)} guarantees that it never
    * throws an {@link IndexOutOfBoundsException} as long as the specified
    * indexes meet basic constraints, regardless what the current index
    * values of the buffer are:
    *
    * <pre>
    * // No matter what the current state of the buffer is, the following
    * // call always succeeds as long as the capacity of the buffer is not
    * // less than 4.
    * buf.setIndex(2, 4);
    * </pre>
    *
    * @throws IndexOutOfBoundsException
    *         if the specified {@code readerIndex} is less than 0,
    *         if the specified {@code writerIndex} is less than the specified
    *         {@code readerIndex} or if the specified {@code writerIndex} is
    *         greater than {@code this.capacity}
    */
   void setIndex(int readerIndex, int writerIndex);

   /**
    * Returns the number of readable bytes which is equal to
    * {@code (this.writerIndex - this.readerIndex)}.
    */
   int readableBytes();

   /**
    * Returns the number of writable bytes which is equal to
    * {@code (this.capacity - this.writerIndex)}.
    */
   int writableBytes();

   /**
    * Returns {@code true}
    * if and only if {@code (this.writerIndex - this.readerIndex)} is greater
    * than {@code 0}.
    */
   boolean readable();

   /**
    * Returns {@code true}
    * if and only if {@code (this.capacity - this.writerIndex)} is greater
    * than {@code 0}.
    */
   boolean writable();

   /**
    * Sets the {@code readerIndex} and {@code writerIndex} of this buffer to
    * {@code 0}.
    * This method is identical to {@link #setIndex(int, int) setIndex(0, 0)}.
    * <p>
    * Please note that the behavior of this method is different
    * from that of NIO buffer, which sets the {@code limit} to
    * the {@code capacity} of the buffer.
    */
   void clear();

   /**
    * Marks the current {@code readerIndex} in this buffer.  You can
    * reposition the current {@code readerIndex} to the marked
    * {@code readerIndex} by calling {@link #resetReaderIndex()}.
    * The initial value of the marked {@code readerIndex} is {@code 0}.
    */
   void markReaderIndex();

   /**
    * Repositions the current {@code readerIndex} to the marked
    * {@code readerIndex} in this buffer.
    *
    * @throws IndexOutOfBoundsException
    *         if the current {@code writerIndex} is less than the marked
    *         {@code readerIndex}
    */
   void resetReaderIndex();

   /**
    * Marks the current {@code writerIndex} in this buffer.  You can
    * reposition the current {@code writerIndex} to the marked
    * {@code writerIndex} by calling {@link #resetWriterIndex()}.
    * The initial value of the marked {@code writerIndex} is {@code 0}.
    */
   void markWriterIndex();

   /**
    * Repositions the current {@code writerIndex} to the marked
    * {@code writerIndex} in this buffer.
    *
    * @throws IndexOutOfBoundsException
    *         if the current {@code readerIndex} is greater than the marked
    *         {@code writerIndex}
    */
   void resetWriterIndex();

   /**
    * Discards the bytes between the 0th index and {@code readerIndex}.
    * It moves the bytes between {@code readerIndex} and {@code writerIndex}
    * to the 0th index, and sets {@code readerIndex} and {@code writerIndex}
    * to {@code 0} and {@code oldWriterIndex - oldReaderIndex} respectively.
    * <p>
    * Please refer to the class documentation for more detailed explanation.
    */
   void discardReadBytes();

   /**
    * Gets a byte at the specified absolute {@code index} in this buffer.
    *
    * @throws IndexOutOfBoundsException
    *         if the specified {@code index} is less than {@code 0} or
    *         {@code index + 1} is greater than {@code this.capacity}
    */
   byte getByte(int index);

   /**
    * Gets an unsigned byte at the specified absolute {@code index} in this
    * buffer.
    *
    * @throws IndexOutOfBoundsException
    *         if the specified {@code index} is less than {@code 0} or
    *         {@code index + 1} is greater than {@code this.capacity}
    */
   short getUnsignedByte(int index);

   /**
    * Gets a 16-bit short integer at the specified absolute {@code index} in
    * this buffer.
    *
    * @throws IndexOutOfBoundsException
    *         if the specified {@code index} is less than {@code 0} or
    *         {@code index + 2} is greater than {@code this.capacity}
    */
   short getShort(int index);

   /**
    * Gets an unsigned 16-bit short integer at the specified absolute
    * {@code index} in this buffer.
    *
    * @throws IndexOutOfBoundsException
    *         if the specified {@code index} is less than {@code 0} or
    *         {@code index + 2} is greater than {@code this.capacity}
    */
   int getUnsignedShort(int index);

   /**
    * Gets a 24-bit medium integer at the specified absolute {@code index} in
    * this buffer.
    *
    * @throws IndexOutOfBoundsException
    *         if the specified {@code index} is less than {@code 0} or
    *         {@code index + 3} is greater than {@code this.capacity}
    */
   int getMedium(int index);

   /**
    * Gets an unsigned 24-bit medium integer at the specified absolute
    * {@code index} in this buffer.
    *
    * @throws IndexOutOfBoundsException
    *         if the specified {@code index} is less than {@code 0} or
    *         {@code index + 3} is greater than {@code this.capacity}
    */
   int getUnsignedMedium(int index);

   /**
    * Gets a 32-bit integer at the specified absolute {@code index} in
    * this buffer.
    *
    * @throws IndexOutOfBoundsException
    *         if the specified {@code index} is less than {@code 0} or
    *         {@code index + 4} is greater than {@code this.capacity}
    */
   int getInt(int index);

   /**
    * Gets an unsigned 32-bit integer at the specified absolute {@code index}
    * in this buffer.
    *
    * @throws IndexOutOfBoundsException
    *         if the specified {@code index} is less than {@code 0} or
    *         {@code index + 4} is greater than {@code this.capacity}
    */
   long getUnsignedInt(int index);

   /**
    * Gets a 64-bit long integer at the specified absolute {@code index} in
    * this buffer.
    *
    * @throws IndexOutOfBoundsException
    *         if the specified {@code index} is less than {@code 0} or
    *         {@code index + 8} is greater than {@code this.capacity}
    */
   long getLong(int index);

   /**
    * Transfers this buffer's data to the specified destination starting at
    * the specified absolute {@code index} until the destination becomes
    * non-writable.  This method is basically same with
    * {@link #getBytes(int, ChannelBuffer, int, int)}, except that this
    * method increases the {@code writerIndex} of the destination by the
    * number of the transferred bytes while
    * {@link #getBytes(int, ChannelBuffer, int, int)} does not.
    *
    * @throws IndexOutOfBoundsException
    *         if the specified {@code index} is less than {@code 0} or
    *         if {@code index + dst.writableBytes} is greater than
    *            {@code this.capacity}
    */
   void getBytes(int index, ChannelBuffer dst);

   /**
    * Transfers this buffer's data to the specified destination starting at
    * the specified absolute {@code index}.  This method is basically same
    * with {@link #getBytes(int, ChannelBuffer, int, int)}, except that this
    * method increases the {@code writerIndex} of the destination by the
    * number of the transferred bytes while
    * {@link #getBytes(int, ChannelBuffer, int, int)} does not.
    *
    * @param length the number of bytes to transfer
    *
    * @throws IndexOutOfBoundsException
    *         if the specified {@code index} is less than {@code 0},
    *         if {@code index + length} is greater than
    *            {@code this.capacity}, or
    *         if {@code length} is greater than {@code dst.writableBytes}
    */
   void getBytes(int index, ChannelBuffer dst, int length);

   /**
    * Transfers this buffer's data to the specified destination starting at
    * the specified absolute {@code index}.
    *
    * @param dstIndex the first index of the destination
    * @param length   the number of bytes to transfer
    *
    * @throws IndexOutOfBoundsException
    *         if the specified {@code index} is less than {@code 0},
    *         if the specified {@code dstIndex} is less than {@code 0},
    *         if {@code index + length} is greater than
    *            {@code this.capacity}, or
    *         if {@code dstIndex + length} is greater than
    *            {@code dst.capacity}
    */
   void getBytes(int index, ChannelBuffer dst, int dstIndex, int length);

   /**
    * Transfers this buffer's data to the specified destination starting at
    * the specified absolute {@code index}.
    *
    * @throws IndexOutOfBoundsException
    *         if the specified {@code index} is less than {@code 0} or
    *         if {@code index + dst.length} is greater than
    *            {@code this.capacity}
    */
   void getBytes(int index, byte[] dst);

   /**
    * Transfers this buffer's data to the specified destination starting at
    * the specified absolute {@code index}.
    *
    * @param dstIndex the first index of the destination
    * @param length   the number of bytes to transfer
    *
    * @throws IndexOutOfBoundsException
    *         if the specified {@code index} is less than {@code 0},
    *         if the specified {@code dstIndex} is less than {@code 0},
    *         if {@code index + length} is greater than
    *            {@code this.capacity}, or
    *         if {@code dstIndex + length} is greater than
    *            {@code dst.length}
    */
   void getBytes(int index, byte[] dst, int dstIndex, int length);

   /**
    * Transfers this buffer's data to the specified destination starting at
    * the specified absolute {@code index} until the destination's position
    * reaches its limit.
    *
    * @throws IndexOutOfBoundsException
    *         if the specified {@code index} is less than {@code 0} or
    *         if {@code index + dst.remaining()} is greater than
    *            {@code this.capacity}
    */
   void getBytes(int index, ByteBuffer dst);

   /**
    * Transfers this buffer's data to the specified stream starting at the
    * specified absolute {@code index}.
    *
    * @param length the number of bytes to transfer
    *
    * @throws IndexOutOfBoundsException
    *         if the specified {@code index} is less than {@code 0} or
    *         if {@code index + length} is greater than
    *            {@code this.capacity}
    * @throws IOException
    *         if the specified stream threw an exception during I/O
    */
   void getBytes(int index, OutputStream out, int length) throws IOException;

   /**
    * Transfers this buffer's data to the specified channel starting at the
    * specified absolute {@code index}.
    *
    * @param length the maximum number of bytes to transfer
    *
    * @return the actual number of bytes written out to the specified channel
    *
    * @throws IndexOutOfBoundsException
    *         if the specified {@code index} is less than {@code 0} or
    *         if {@code index + length} is greater than
    *            {@code this.capacity}
    * @throws IOException
    *         if the specified channel threw an exception during I/O
    */
   int getBytes(int index, GatheringByteChannel out, int length) throws IOException;

   /**
    * Sets the specified byte at the specified absolute {@code index} in this
    * buffer.
    *
    * @throws IndexOutOfBoundsException
    *         if the specified {@code index} is less than {@code 0} or
    *         {@code index + 1} is greater than {@code this.capacity}
    */
   void setByte(int index, byte value);

   /**
    * Sets the specified 16-bit short integer at the specified absolute
    * {@code index} in this buffer.
    *
    * @throws IndexOutOfBoundsException
    *         if the specified {@code index} is less than {@code 0} or
    *         {@code index + 2} is greater than {@code this.capacity}
    */
   void setShort(int index, short value);

   /**
    * Sets the specified 24-bit medium integer at the specified absolute
    * {@code index} in this buffer.  Please note that the most significant
    * byte is ignored in the specified value.
    *
    * @throws IndexOutOfBoundsException
    *         if the specified {@code index} is less than {@code 0} or
    *         {@code index + 3} is greater than {@code this.capacity}
    */
   void setMedium(int index, int value);

   /**
    * Sets the specified 32-bit integer at the specified absolute
    * {@code index} in this buffer.
    *
    * @throws IndexOutOfBoundsException
    *         if the specified {@code index} is less than {@code 0} or
    *         {@code index + 4} is greater than {@code this.capacity}
    */
   void setInt(int index, int value);

   /**
    * Sets the specified 64-bit long integer at the specified absolute
    * {@code index} in this buffer.
    *
    * @throws IndexOutOfBoundsException
    *         if the specified {@code index} is less than {@code 0} or
    *         {@code index + 8} is greater than {@code this.capacity}
    */
   void setLong(int index, long value);

   /**
    * Transfers the specified source buffer's data to this buffer starting at
    * the specified absolute {@code index} until the destination becomes
    * unreadable.  This method is basically same with
    * {@link #setBytes(int, ChannelBuffer, int, int)}, except that this
    * method increases the {@code readerIndex} of the source buffer by
    * the number of the transferred bytes while
    * {@link #getBytes(int, ChannelBuffer, int, int)} does not.
    *
    * @throws IndexOutOfBoundsException
    *         if the specified {@code index} is less than {@code 0} or
    *         if {@code index + src.readableBytes} is greater than
    *            {@code this.capacity}
    */
   void setBytes(int index, ChannelBuffer src);

   /**
    * Transfers the specified source buffer's data to this buffer starting at
    * the specified absolute {@code index}.  This method is basically same
    * with {@link #setBytes(int, ChannelBuffer, int, int)}, except that this
    * method increases the {@code readerIndex} of the source buffer by
    * the number of the transferred bytes while
    * {@link #getBytes(int, ChannelBuffer, int, int)} does not.
    *
    * @param length the number of bytes to transfer
    *
    * @throws IndexOutOfBoundsException
    *         if the specified {@code index} is less than {@code 0},
    *         if {@code index + length} is greater than
    *            {@code this.capacity}, or
    *         if {@code length} is greater than {@code src.readableBytes}
    */
   void setBytes(int index, ChannelBuffer src, int length);

   /**
    * Transfers the specified source buffer's data to this buffer starting at
    * the specified absolute {@code index}.
    *
    * @param srcIndex the first index of the source
    * @param length   the number of bytes to transfer
    *
    * @throws IndexOutOfBoundsException
    *         if the specified {@code index} is less than {@code 0},
    *         if the specified {@code srcIndex} is less than {@code 0},
    *         if {@code index + length} is greater than
    *            {@code this.capacity}, or
    *         if {@code srcIndex + length} is greater than
    *            {@code src.capacity}
    */
   void setBytes(int index, ChannelBuffer src, int srcIndex, int length);

   /**
    * Transfers the specified source array's data to this buffer starting at
    * the specified absolute {@code index}.
    *
    * @throws IndexOutOfBoundsException
    *         if the specified {@code index} is less than {@code 0} or
    *         if {@code index + src.length} is greater than
    *            {@code this.capacity}
    */
   void setBytes(int index, byte[] src);

   /**
    * Transfers the specified source array's data to this buffer starting at
    * the specified absolute {@code index}.
    *
    * @throws IndexOutOfBoundsException
    *         if the specified {@code index} is less than {@code 0},
    *         if the specified {@code srcIndex} is less than {@code 0},
    *         if {@code index + length} is greater than
    *            {@code this.capacity}, or
    *         if {@code srcIndex + length} is greater than {@code src.length}
    */
   void setBytes(int index, byte[] src, int srcIndex, int length);

   /**
    * Transfers the specified source buffer's data to this buffer starting at
    * the specified absolute {@code index} until the source buffer's position
    * reaches its limit.
    *
    * @throws IndexOutOfBoundsException
    *         if the specified {@code index} is less than {@code 0} or
    *         if {@code index + src.remaining()} is greater than
    *            {@code this.capacity}
    */
   void setBytes(int index, ByteBuffer src);

   /**
    * Transfers the content of the specified source stream to this buffer
    * starting at the specified absolute {@code index}.
    *
    * @param length the number of bytes to transfer
    *
    * @return the actual number of bytes read in from the specified channel.
    *         {@code -1} if the specified channel is closed.
    *
    * @throws IndexOutOfBoundsException
    *         if the specified {@code index} is less than {@code 0} or
    *         if {@code index + length} is greater than {@code this.capacity}
    * @throws IOException
    *         if the specified stream threw an exception during I/O
    */
   int setBytes(int index, InputStream in, int length) throws IOException;

   /**
    * Transfers the content of the specified source channel to this buffer
    * starting at the specified absolute {@code index}.
    *
    * @param length the maximum number of bytes to transfer
    *
    * @return the actual number of bytes read in from the specified channel.
    *         {@code -1} if the specified channel is closed.
    *
    * @throws IndexOutOfBoundsException
    *         if the specified {@code index} is less than {@code 0} or
    *         if {@code index + length} is greater than {@code this.capacity}
    * @throws IOException
    *         if the specified channel threw an exception during I/O
    */
   int setBytes(int index, ScatteringByteChannel in, int length) throws IOException;

   /**
    * Fills this buffer with <tt>NUL (0x00)</tt> starting at the specified
    * absolute {@code index}.
    *
    * @param length the number of <tt>NUL</tt>s to write to the buffer
    *
    * @throws IndexOutOfBoundsException
    *         if the specified {@code index} is less than {@code 0} or
    *         if {@code index + length} is greater than {@code this.capacity}
    */
   void setZero(int index, int length);

   /**
    * Gets a byte at the current {@code readerIndex} and increases
    * the {@code readerIndex} by {@code 1} in this buffer.
    *
    * @throws IndexOutOfBoundsException
    *         if {@code this.readableBytes} is less than {@code 1}
    */
   byte readByte();

   /**
    * Gets an unsigned byte at the current {@code readerIndex} and increases
    * the {@code readerIndex} by {@code 1} in this buffer.
    *
    * @throws IndexOutOfBoundsException
    *         if {@code this.readableBytes} is less than {@code 1}
    */
   short readUnsignedByte();

   /**
    * Gets a 16-bit short integer at the current {@code readerIndex}
    * and increases the {@code readerIndex} by {@code 2} in this buffer.
    *
    * @throws IndexOutOfBoundsException
    *         if {@code this.readableBytes} is less than {@code 2}
    */
   short readShort();

   /**
    * Gets an unsigned 16-bit short integer at the current {@code readerIndex}
    * and increases the {@code readerIndex} by {@code 2} in this buffer.
    *
    * @throws IndexOutOfBoundsException
    *         if {@code this.readableBytes} is less than {@code 2}
    */
   int readUnsignedShort();

   /**
    * Gets a 24-bit medium integer at the current {@code readerIndex}
    * and increases the {@code readerIndex} by {@code 3} in this buffer.
    *
    * @throws IndexOutOfBoundsException
    *         if {@code this.readableBytes} is less than {@code 3}
    */
   int readMedium();

   /**
    * Gets an unsigned 24-bit medium integer at the current {@code readerIndex}
    * and increases the {@code readerIndex} by {@code 3} in this buffer.
    *
    * @throws IndexOutOfBoundsException
    *         if {@code this.readableBytes} is less than {@code 3}
    */
   int readUnsignedMedium();

   /**
    * Gets a 32-bit integer at the current {@code readerIndex}
    * and increases the {@code readerIndex} by {@code 4} in this buffer.
    *
    * @throws IndexOutOfBoundsException
    *         if {@code this.readableBytes} is less than {@code 4}
    */
   int readInt();

   /**
    * Gets an unsigned 32-bit integer at the current {@code readerIndex}
    * and increases the {@code readerIndex} by {@code 4} in this buffer.
    *
    * @throws IndexOutOfBoundsException
    *         if {@code this.readableBytes} is less than {@code 4}
    */
   long readUnsignedInt();

   /**
    * Gets a 64-bit integer at the current {@code readerIndex}
    * and increases the {@code readerIndex} by {@code 8} in this buffer.
    *
    * @throws IndexOutOfBoundsException
    *         if {@code this.readableBytes} is less than {@code 8}
    */
   long readLong();

   /**
    * Transfers this buffer's data to the specified destination starting at
    * the current {@code readerIndex} until the destination becomes
    * non-writable, and increases the {@code readerIndex} by the number of the
    * transferred bytes.  This method is basically same with
    * {@link #readBytes(ChannelBuffer, int, int)}, except that this method
    * increases the {@code writerIndex} of the destination by the number of
    * the transferred bytes while {@link #readBytes(ChannelBuffer, int, int)}
    * does not.
    *
    * @throws IndexOutOfBoundsException
    *         if {@code dst.writableBytes} is greater than
    *            {@code this.readableBytes}
    */
   void readBytes(ChannelBuffer dst);

   /**
    * Transfers this buffer's data to the specified destination starting at
    * the current {@code readerIndex} and increases the {@code readerIndex}
    * by the number of the transferred bytes (= {@code length}).  This method
    * is basically same with {@link #readBytes(ChannelBuffer, int, int)},
    * except that this method increases the {@code writerIndex} of the
    * destination by the number of the transferred bytes (= {@code length})
    * while {@link #readBytes(ChannelBuffer, int, int)} does not.
    *
    * @throws IndexOutOfBoundsException
    *         if {@code length} is greater than {@code this.readableBytes} or
    *         if {@code length} is greater than {@code dst.writableBytes}
    */
   void readBytes(ChannelBuffer dst, int length);

   /**
    * Transfers this buffer's data to the specified destination starting at
    * the current {@code readerIndex} and increases the {@code readerIndex}
    * by the number of the transferred bytes (= {@code length}).
    *
    * @param dstIndex the first index of the destination
    * @param length   the number of bytes to transfer
    *
    * @throws IndexOutOfBoundsException
    *         if the specified {@code dstIndex} is less than {@code 0},
    *         if {@code length} is greater than {@code this.readableBytes}, or
    *         if {@code dstIndex + length} is greater than
    *            {@code dst.capacity}
    */
   void readBytes(ChannelBuffer dst, int dstIndex, int length);

   /**
    * Transfers this buffer's data to the specified destination starting at
    * the current {@code readerIndex} and increases the {@code readerIndex}
    * by the number of the transferred bytes (= {@code dst.length}).
    *
    * @throws IndexOutOfBoundsException
    *         if {@code dst.length} is greater than {@code this.readableBytes}
    */
   void readBytes(byte[] dst);

   /**
    * Transfers this buffer's data to the specified destination starting at
    * the current {@code readerIndex} and increases the {@code readerIndex}
    * by the number of the transferred bytes (= {@code length}).
    *
    * @param dstIndex the first index of the destination
    * @param length   the number of bytes to transfer
    *
    * @throws IndexOutOfBoundsException
    *         if the specified {@code dstIndex} is less than {@code 0},
    *         if {@code length} is greater than {@code this.readableBytes}, or
    *         if {@code dstIndex + length} is greater than {@code dst.length}
    */
   void readBytes(byte[] dst, int dstIndex, int length);

   /**
    * Transfers this buffer's data to the specified destination starting at
    * the current {@code readerIndex} until the destination's position
    * reaches its limit, and increases the {@code readerIndex} by the
    * number of the transferred bytes.
    *
    * @throws IndexOutOfBoundsException
    *         if {@code dst.remaining()} is greater than
    *            {@code this.readableBytes}
    */
   void readBytes(ByteBuffer dst);

   /**
    * Transfers this buffer's data to the specified stream starting at the
    * current {@code readerIndex}.
    *
    * @param length the number of bytes to transfer
    *
    * @throws IndexOutOfBoundsException
    *         if {@code length} is greater than {@code this.readableBytes}
    * @throws IOException
    *         if the specified stream threw an exception during I/O
    */
   void readBytes(OutputStream out, int length) throws IOException;

   /**
    * Transfers this buffer's data to the specified stream starting at the
    * current {@code readerIndex}.
    *
    * @param length the maximum number of bytes to transfer
    *
    * @return the actual number of bytes written out to the specified channel
    *
    * @throws IndexOutOfBoundsException
    *         if {@code length} is greater than {@code this.readableBytes}
    * @throws IOException
    *         if the specified channel threw an exception during I/O
    */
   int readBytes(GatheringByteChannel out, int length) throws IOException;

   /**
    * Increases the current {@code readerIndex} by the specified
    * {@code length} in this buffer.
    *
    * @throws IndexOutOfBoundsException
    *         if {@code length} is greater than {@code this.readableBytes}
    */
   void skipBytes(int length);

   /**
    * Sets the specified byte at the current {@code writerIndex}
    * and increases the {@code writerIndex} by {@code 1} in this buffer.
    *
    * @throws IndexOutOfBoundsException
    *         if {@code this.writableBytes} is less than {@code 1}
    */
   void writeByte(byte value);

   /**
    * Sets the specified 16-bit short integer at the current
    * {@code writerIndex} and increases the {@code writerIndex} by {@code 2}
    * in this buffer.
    *
    * @throws IndexOutOfBoundsException
    *         if {@code this.writableBytes} is less than {@code 2}
    */
   void writeShort(short value);

   /**
    * Sets the specified 24-bit medium integer at the current
    * {@code writerIndex} and increases the {@code writerIndex} by {@code 3}
    * in this buffer.
    *
    * @throws IndexOutOfBoundsException
    *         if {@code this.writableBytes} is less than {@code 3}
    */
   void writeMedium(int value);

   /**
    * Sets the specified 32-bit integer at the current {@code writerIndex}
    * and increases the {@code writerIndex} by {@code 4} in this buffer.
    *
    * @throws IndexOutOfBoundsException
    *         if {@code this.writableBytes} is less than {@code 4}
    */
   void writeInt(int value);

   /**
    * Sets the specified 64-bit long integer at the current
    * {@code writerIndex} and increases the {@code writerIndex} by {@code 8}
    * in this buffer.
    *
    * @throws IndexOutOfBoundsException
    *         if {@code this.writableBytes} is less than {@code 8}
    */
   void writeLong(long value);

   /**
    * Transfers the specified source buffer's data to this buffer starting at
    * the current {@code writerIndex} until the source buffer becomes
    * unreadable, and increases the {@code writerIndex} by the number of
    * the transferred bytes.  This method is basically same with
    * {@link #writeBytes(ChannelBuffer, int, int)}, except that this method
    * increases the {@code readerIndex} of the source buffer by the number of
    * the transferred bytes while {@link #writeBytes(ChannelBuffer, int, int)}
    * does not.
    *
    * @throws IndexOutOfBoundsException
    *         if {@code src.readableBytes} is greater than
    *            {@code this.writableBytes}
    *
    */
   void writeBytes(ChannelBuffer src);

   /**
    * Transfers the specified source buffer's data to this buffer starting at
    * the current {@code writerIndex} and increases the {@code writerIndex}
    * by the number of the transferred bytes (= {@code length}).  This method
    * is basically same with {@link #writeBytes(ChannelBuffer, int, int)},
    * except that this method increases the {@code readerIndex} of the source
    * buffer by the number of the transferred bytes (= {@code length}) while
    * {@link #writeBytes(ChannelBuffer, int, int)} does not.
    *
    * @param length the number of bytes to transfer
    *
    * @throws IndexOutOfBoundsException
    *         if {@code length} is greater than {@code this.writableBytes} or
    *         if {@code length} is greater then {@code src.readableBytes}
    */
   void writeBytes(ChannelBuffer src, int length);

   /**
    * Transfers the specified source buffer's data to this buffer starting at
    * the current {@code writerIndex} and increases the {@code writerIndex}
    * by the number of the transferred bytes (= {@code length}).
    *
    * @param srcIndex the first index of the source
    * @param length   the number of bytes to transfer
    *
    * @throws IndexOutOfBoundsException
    *         if the specified {@code srcIndex} is less than {@code 0},
    *         if {@code srcIndex + length} is greater than
    *            {@code src.capacity}, or
    *         if {@code length} is greater than {@code this.writableBytes}
    */
   void writeBytes(ChannelBuffer src, int srcIndex, int length);

   /**
    * Transfers the specified source array's data to this buffer starting at
    * the current {@code writerIndex} and increases the {@code writerIndex}
    * by the number of the transferred bytes (= {@code src.length}).
    *
    * @throws IndexOutOfBoundsException
    *         if {@code src.length} is greater than {@code this.writableBytes}
    */
   void writeBytes(byte[] src);

   /**
    * Transfers the specified source array's data to this buffer starting at
    * the current {@code writerIndex} and increases the {@code writerIndex}
    * by the number of the transferred bytes (= {@code length}).
    *
    * @param srcIndex the first index of the source
    * @param length   the number of bytes to transfer
    *
    * @throws IndexOutOfBoundsException
    *         if the specified {@code srcIndex} is less than {@code 0},
    *         if {@code srcIndex + length} is greater than
    *            {@code src.length}, or
    *         if {@code length} is greater than {@code this.writableBytes}
    */
   void writeBytes(byte[] src, int srcIndex, int length);

   /**
    * Transfers the specified source buffer's data to this buffer starting at
    * the current {@code writerIndex} until the source buffer's position
    * reaches its limit, and increases the {@code writerIndex} by the
    * number of the transferred bytes.
    *
    * @throws IndexOutOfBoundsException
    *         if {@code src.remaining()} is greater than
    *            {@code this.writableBytes}
    */
   void writeBytes(ByteBuffer src);

   /**
    * Transfers the content of the specified stream to this buffer
    * starting at the current {@code writerIndex} and increases the
    * {@code writerIndex} by the number of the transferred bytes.
    *
    * @param length the number of bytes to transfer
    *
    * @throws IndexOutOfBoundsException
    *         if {@code length} is greater than {@code this.writableBytes}
    * @throws IOException
    *         if the specified stream threw an exception during I/O
    */
   void writeBytes(InputStream in, int length) throws IOException;

   /**
    * Transfers the content of the specified channel to this buffer
    * starting at the current {@code writerIndex} and increases the
    * {@code writerIndex} by the number of the transferred bytes.
    *
    * @param length the maximum number of bytes to transfer
    *
    * @return the actual number of bytes read in from the specified channel
    *
    * @throws IndexOutOfBoundsException
    *         if {@code length} is greater than {@code this.writableBytes}
    * @throws IOException
    *         if the specified channel threw an exception during I/O
    */
   int writeBytes(ScatteringByteChannel in, int length) throws IOException;

   /**
    * Fills this buffer with <tt>NUL (0x00)</tt> starting at the current
    * {@code writerIndex} and increases the {@code writerIndex} by the
    * specified {@code length}.
    *
    * @param length the number of <tt>NUL</tt>s to write to the buffer
    *
    * @throws IndexOutOfBoundsException
    *         if {@code length} is greater than {@code this.writableBytes}
    */
   void writeZero(int length);

   /**
    * Converts this buffer's readable bytes into a NIO buffer.  The returned
    * buffer might or might not share the content with this buffer, while
    * they have separate indexes and marks.  This method is identical to
    * {@code buf.toByteBuffer(buf.readerIndex(), buf.readableBytes())}.
    */
   ByteBuffer toByteBuffer();

   /**
    * Converts this buffer's sub-region into a NIO buffer.  The returned
    * buffer might or might not share the content with this buffer, while
    * they have separate indexes and marks.
    */
   ByteBuffer toByteBuffer(int index, int length);

   /**
    * Converts this buffer's sub-region into an array of NIO buffers.
    * The returned buffers might or might not share the content with this
    * buffer, while they have separate indexes and marks.
    */
   ByteBuffer[] toByteBuffers(int index, int length);

   /**
    * Decodes this buffer's readable bytes into a string with the specified
    * character set name.  This method is identical to
    * {@code buf.toString(buf.readerIndex(), buf.readableBytes(), charsetName)}.
    *
    * @throws UnsupportedCharsetException
    *         if the specified character set name is not supported by the
    *         current VM
    */
   String toString(String charsetName);

   /**
    * Decodes this buffer's sub-region into a string with the specified
    * character set name.
    *
    * @throws UnsupportedCharsetException
    *         if the specified character set name is not supported by the
    *         current VM
    */
   String toString(int index, int length, String charsetName);

   /**
    * Returns a hash code which was calculated from the content of this
    * buffer.  If there's a byte array which is
    * {@linkplain #equals(Object) equal to} this array, both arrays should
    * return the same value.
    */
   int hashCode();

   /**
    * Determines if the content of the specified buffer is identical to the
    * content of this array.  'Identical' here means:
    * <ul>
    * <li>the size of the contents of the two buffers are same and</li>
    * <li>every single byte of the content of the two buffers are same.</li>
    * </ul>
    * Please note that it does not compare {@link #readerIndex()} nor
    * {@link #writerIndex()}.  This method also returns {@code false} for
    * {@code null} and an object which is not an instance of
    * {@link ChannelBuffer} type.
    */
   boolean equals(Object obj);

   /**
    * Compares the content of the specified buffer to the content of this
    * buffer.  Comparison is performed in the same manner with the string
    * comparison functions of various languages such as {@code strcmp},
    * {@code memcmp} and {@link String#compareTo(String)}.
    */
   int compareTo(ChannelBuffer buffer);

   /**
    * Returns the string representation of this buffer.  This method does not
    * necessarily return the whole content of the buffer but returns
    * the values of the key properties such as {@link #readerIndex()},
    * {@link #writerIndex()} and {@link #capacity()}.
    */
   String toString();
}
