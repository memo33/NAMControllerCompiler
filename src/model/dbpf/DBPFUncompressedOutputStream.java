package model.dbpf;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;

/**
 * This class allows for direct writing of uncompressed
 * subfiles into a DBPF file.
 * 
 * When the stream is closed, the DBPF-index is printed
 * and the DBPF file header is updated accordingly.
 * 
 * Before printing a subfile, you have to invoke the writeTGI-method
 * in which you specify the TGI of the subfile.
 * 
 * Then you can append as many file-system-files as you want using the
 * writeFile-method, or use a PrintWriter to print.
 * 
 * IMPORTANT: If you are using this stream nested in another one, e.g. in
 * conjunction with a PrintWriter, you have to call the flush-method
 * of the PrintWriter in order to complete a subfile, before calling
 * the writeTGI-method to start a new subfile. Otherwise, the output
 * DBPF file will be corrupt. You also have to flush the PrintWriter,
 * if you use this stream's write methods and the PrintWriter's write
 * methods interchangeably. 
 */
public class DBPFUncompressedOutputStream extends FileOutputStream {

	private int numberOfFiles = 0;
	private List<Integer> indexList;
	private int lastPointer = 0x60;
	private boolean isClosed = false;
	private FileChannel outChannel;
	private File file;

	/**
	 * Constructor.
	 * @param file The file for the DBPF file output. If this file
	 *             already exists, it will be deleted, first.
	 * @throws IOException
	 */
	public DBPFUncompressedOutputStream(File file) throws IOException {
		super(file);
		if (file.exists()) file.delete();

		this.file = file;
		super.write(new byte[0x60]);
		indexList = new ArrayList<Integer>();
		outChannel = this.getChannel();
	}

	/**
	 * Writes DBPF-Header using RandomAccessFile.
	 * @throws IOException
	 */
	private void writeHeader() throws IOException {
	    RandomAccessFile raf = null;
	    try {
		    raf = new RandomAccessFile(file, "rwd");
			raf.writeBytes("DBPF");
			raf.writeInt(Integer.reverseBytes(1));
			raf.writeInt(Integer.reverseBytes(0));
			raf.write(new byte[12]);
			raf.writeInt(Integer.reverseBytes((int) (System.currentTimeMillis()/1000)));
			raf.writeInt(Integer.reverseBytes((int) (System.currentTimeMillis()/1000)));
			raf.writeInt(Integer.reverseBytes(7));
			raf.writeInt(Integer.reverseBytes(numberOfFiles));
			raf.writeInt(Integer.reverseBytes((int) outChannel.position()));
			raf.writeInt(Integer.reverseBytes(20 * numberOfFiles));
			raf.write(new byte[0x30]);
		} finally {
		    if (raf != null) {
		        raf.close();
		    }
		}
	}

	/**
	 * Begins a new subfile and sets the TGI accordingly.
	 * IMPORTANT: If you are using this stream nested in another one,
	 * you have to call its flush-method to finish the subfile,
	 * before calling this method to start a new subfile.
	 * @param tgi The TGI of the next subfile.
	 * @throws IOException
	 */
	public void writeTGI(DBPFTGI tgi) throws IOException {
		if (numberOfFiles > 0) // add length of previous subfile
			appendLatestSubFileLength();
		indexList.add(tgi.getType());
		indexList.add(tgi.getGroup());
		indexList.add(tgi.getInstance());
		indexList.add((int) outChannel.position());
		numberOfFiles++;
	}
	
	/**
	 * Appends the content of a file to this DBPF file. 
	 * @param file
	 */
	public void writeFile(File file) {
	    FileInputStream fis = null;
	    FileChannel source = null;
		try {
		    fis = new FileInputStream(file);
		    source = fis.getChannel();
			long len = outChannel.transferFrom(source, outChannel.position(), source.size());
			outChannel.position(outChannel.position() + len);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
		    if (source != null) {
		        try {
                    source.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
		    }
		    if (fis != null) {
		        try {
		            fis.close();
		        } catch (IOException e) {
		            e.printStackTrace();
		        }
		    }
		}
	}

	/**
	 * Now the length of the previous subfile is known and is added
	 * to the index-list.
	 * @throws IOException
	 */
	private void appendLatestSubFileLength() throws IOException {
		int newPointer = (int) outChannel.position();
		indexList.add(newPointer - lastPointer);
		lastPointer = newPointer;
	}

	/*
	 * Writes DBPF header and index table. 
	 */
	@Override
	public void close() throws IOException {
		if (!isClosed) {
			isClosed = true;
			if (numberOfFiles > 0) appendLatestSubFileLength();
			this.writeHeader();

			BufferedOutputStream bos = null;
			try {
			    bos = new BufferedOutputStream(this);
				for (Integer entry : indexList) {
					ByteBuffer bb = ByteBuffer.allocate(4);
					bb.putInt(Integer.reverseBytes(entry));
					bos.write(bb.array());
				}
				bos.flush();
			} finally {
			    if (bos != null) {
			        bos.close();
			    }
			}
			outChannel.close();
		}
		super.close();
	}
}
