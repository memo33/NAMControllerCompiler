package model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Scanner;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import jdpbfx.DBPFEntry;
import jdpbfx.DBPFFile;
import jdpbfx.DBPFFile.DirectDBPFEntry;
import jdpbfx.DBPFTGI;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * integration test
 */
public class ModelTest {

    @Rule
    public final TemporaryFolder tempFolder = new TemporaryFolder();
    
    @Test
    public void testWritingOfDBPFFile() throws IOException {
        final String sampleText = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789\r\n";
        final int count = 10000;
        File outputFile = tempFolder.newFile();
        
        ChangeListener changeListener = new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                // do nothing
            }
        };
        RULEntry rulEntry = new RULEntry(DBPFTGI.BLANKTGI, new ArrayDeque<File>(0), changeListener) {
            @Override
            void printHeader() throws IOException {
                // do not print header as it depends on modification dates
            }
            @Override
            void provideData() throws IOException {
                for (int i = 0; i < count; i++) {
                    writer.write(sampleText);
                }
                writer.flush();
            }
        };
        
        Collection<DBPFEntry> writeList = Arrays.asList((DBPFEntry) rulEntry);
        assertTrue(DBPFFile.Writer.write(outputFile, writeList));
        assertTrue(DBPFFile.Header.HEADER_SIZE + count * sampleText.length() + writeList.size() * 20 == outputFile.length());
        
        DBPFFile dbpfFile = DBPFFile.Reader.read(outputFile);
        assertTrue(dbpfFile.header.getIndexEntryCount() == writeList.size());
        assertTrue(dbpfFile.header.getIndexOffsetLocation() == DBPFFile.Header.HEADER_SIZE + count * sampleText.length());
        assertTrue(dbpfFile.header.getIndexSize() == writeList.size() * 20);
        assertTrue(dbpfFile.header.getIndexType() == 7);
        assertTrue(dbpfFile.header.getMajorVersion() == 1);
        assertTrue(dbpfFile.header.getMinorVersion() == 0);
        DirectDBPFEntry entry = dbpfFile.getEntry(0);
        assertEquals(entry.getTGI(), DBPFTGI.BLANKTGI);
        byte[] data = entry.createData();
        Scanner scanner = new Scanner(new ByteArrayInputStream(data));
        while (scanner.hasNextLine()) {
            assertEquals(scanner.nextLine() + "\r\n", sampleText);
        }
        scanner.close();
    }
    
    @Test
    public void testWritingOfMultipleEntries() throws IOException {
        int count = 20;
        final byte[] data = new byte[100];
        Collection<DBPFEntry> writeList = new LinkedList<DBPFEntry>();
        for (int i = 0; i < count; i++) {
            DBPFTGI tgi = DBPFTGI.valueOf(i, i, i);
            writeList.add(new DBPFEntry(tgi) {
                @Override
                public ReadableByteChannel createDataChannel() {
                    return Channels.newChannel(new ByteArrayInputStream(data));
                }
            });
        }
        File outputFile = tempFolder.newFile();
        assertTrue(DBPFFile.Writer.write(outputFile, writeList));
        assertTrue(DBPFFile.Header.HEADER_SIZE + count * data.length + writeList.size() * 20 == outputFile.length());
        
        DBPFFile dbpfFile = DBPFFile.Reader.read(outputFile);
        assertTrue(dbpfFile.header.getIndexEntryCount() == writeList.size());
        assertTrue(dbpfFile.header.getIndexOffsetLocation() == DBPFFile.Header.HEADER_SIZE + count * data.length);
        assertTrue(dbpfFile.header.getIndexSize() == writeList.size() * 20);
        assertTrue(dbpfFile.header.getIndexType() == 7);
        assertTrue(dbpfFile.header.getMajorVersion() == 1);
        assertTrue(dbpfFile.header.getMinorVersion() == 0);
        assertTrue(dbpfFile.getEntries().size() == writeList.size());
        int i = 0;
        for (DirectDBPFEntry entry : dbpfFile.getEntries()) {
            assertEquals(entry.getTGI(), DBPFTGI.valueOf(i,i,i));
            assertTrue(entry.createData().length == data.length);
            i++;
        }
    }
}
