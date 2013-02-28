package at.fhooe.mcm.smc.wav;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

public class WavReader {

	private String filePath;
	private RandomAccessFile raf;
	private int channels;
	private int sampleRate;
	private int byteRate;
	private int frameSize;
	private short resolution; 
	private int length;
	private int payloadLength;
	
	private long currentPos;

	public WavReader(String filePath) {
		this.filePath = filePath;
		init();
	}

	private void init() {
		try {
			raf = new RandomAccessFile(filePath, "r");
			
			// read and file length payload length fields
			raf.seek(4);
			length = Integer.reverseBytes(raf.readInt());
			raf.seek(40);
			payloadLength = Integer.reverseBytes(raf.readInt());
			
			// get other metadata
			// channel count
			raf.seek(22);
			channels = Short.reverseBytes(raf.readShort());
			sampleRate = Integer.reverseBytes(raf.readInt());
			byteRate =  Integer.reverseBytes(raf.readInt());
			frameSize = Short.reverseBytes(raf.readShort());
			resolution = Short.reverseBytes(raf.readShort());
			
			// set at start of data part
			currentPos = 44;
			raf.seek(currentPos);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public int read(byte[] buffer, int offset, int count) throws IOException {
		int read = raf.read(buffer, offset, count);
		currentPos += read;
		raf.seek(currentPos);
		return read;
	}
	
	public void reset() {
		init();
	}
	
	public void close() throws IOException {
		raf.close();
	}

	/**
	 * @return the channels
	 */
	public int getChannels() {
		return channels;
	}

	/**
	 * @return the sampleRate
	 */
	public int getSampleRate() {
		return sampleRate;
	}

	/**
	 * @return the frameSize
	 */
	public int getFrameSize() {
		return frameSize;
	}

	/**
	 * @return the byteRate
	 */
	public int getByteRate() {
		return byteRate;
	}

	/**
	 * @return the resolution
	 */
	public short getResolution() {
		return resolution;
	}

	/**
	 * @return the payloadLength
	 */
	public int getPayloadLength() {
		return payloadLength;
	}
}
