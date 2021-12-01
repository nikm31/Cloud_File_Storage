package ru.geekbrains.utils;

import io.netty.channel.Channel;
import ru.geekbrains.models.File.PartFile;
import ru.geekbrains.models.PartFileWriteWrapper;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Logger;

public class FileUtils {

    private static final Logger LOG = Logger.getLogger(FileUtils.class.getName());
    private static FileUtils fileUtils;

    private FileUtils() {
    }

    public static synchronized FileUtils getInstance() {
        if (fileUtils == null) {
            fileUtils = new FileUtils();
            PartFileSaveThread fileSaveThread = new PartFileSaveThread();
            fileSaveThread.start();
        }
        return fileUtils;
    }

    public static int PACKAGE_SIZE = 1024*1024*10;

    public synchronized void sendFileByParts(Path path, Channel channel, long offset) throws IOException {

        long endPos = offset + PACKAGE_SIZE;
        boolean isLast = false;
        if (endPos > path.toFile().length()) {
            endPos = path.toFile().length();
            isLast = true;
        }
        long packageSize = endPos - offset;
        byte[] content = getPartFromPath(path, offset, (int) packageSize);
        PartFile partFile = new PartFile(content, offset, endPos, isLast, path.toFile().getName());
        channel.writeAndFlush(partFile);
    }

    private byte[] getPartFromPath(Path path, long offset, int size) throws IOException {
        byte[] part;
        try (RandomAccessFile randomAccessFile = new RandomAccessFile(path.toString(), "r")) {
            part = new byte[size];
            randomAccessFile.seek(offset);
            randomAccessFile.read(part);
        }
        return part;
    }

    public void prepareAndSavePart(Path path, long offset, byte[] data) throws IOException {
        prepareFile(path, offset);
        savePart(path, data);
    }

    public void prepareFile(Path path, long offset) {
        try {
            if (offset == 0) {
                Files.deleteIfExists(path);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public synchronized void savePart(Path path, byte[] data) {
        PartFileSaveThread.addPart(new PartFileWriteWrapper(path, data));
    }

}
