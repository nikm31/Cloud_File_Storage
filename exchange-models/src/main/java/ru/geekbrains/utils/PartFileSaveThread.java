package ru.geekbrains.utils;

import ru.geekbrains.models.PartFileWriteWrapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.nio.file.StandardOpenOption.APPEND;

public class PartFileSaveThread extends Thread {

    private static Queue<PartFileWriteWrapper> writeQueue;

    public PartFileSaveThread() {
        PartFileSaveThread.writeQueue = new LinkedBlockingQueue<>();
    }

    public static void addPart(PartFileWriteWrapper wrapper) {
        writeQueue.add(wrapper);
    }

    @Override
    public void run() {
        while (true) {
            PartFileWriteWrapper poll = writeQueue.poll();
            if (poll != null) {
                try {
                    Files.write(poll.getPath(), poll.getData(), StandardOpenOption.CREATE, StandardOpenOption.SYNC, APPEND);
                } catch (IOException ex) {
                    Logger.getLogger(PartFileSaveThread.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }

}
