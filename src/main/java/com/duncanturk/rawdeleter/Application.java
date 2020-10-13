package com.duncanturk.rawdeleter;

import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDropEvent;
import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static java.util.function.Predicate.not;

public class Application {

    private final JFrame frame = new JFrame();
    private final JButton button = new JButton("Öffnen ...");

    private final Set<String> RAW_ENDINGS = Set.of("ARW", "CR2");
    private final Set<String> JPG_ENDINGS = Set.of("JPEG", "JPG", "JPE", "JFIF");

    private final JLabel jpegLabel = new JLabel("0");
    private final JLabel pairsLabel = new JLabel("0");
    private final JLabel removeLabel = new JLabel("0");
    private final JLabel pathField = new JLabel();
    private final JButton deleteButton = new JButton("Löschen!");


    public static void main(String[] args) {
        new Application().start();
    }

    File dir = null;

    public Application() {
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setAlwaysOnTop(true);

        pathField.setPreferredSize(new Dimension(400, 20));
        var panel = new JPanel(new MigLayout("", "[]rel[][]"));
        panel.add(new JLabel("Pfad:"));
        panel.add(pathField, "grow");
        panel.add(button, "wrap");

        var chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        button.addActionListener(a -> {
            frame.setAlwaysOnTop(false);
            if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                setDir(chooser.getSelectedFile());
            } else {
                System.out.println("No Selection ");
            }
            frame.setAlwaysOnTop(true);
        });

        panel.add(new JLabel("JPEGs:"));
        panel.add(jpegLabel, "wrap");
        panel.add(new JLabel("Paare:"));
        panel.add(pairsLabel, "wrap");
        panel.add(new JLabel("Lösche:"));
        panel.add(removeLabel);
        panel.add(deleteButton);

        deleteButton.addActionListener(a -> {
            if (dir != null) {
                getFilesToDelete(dir).forEach(File::delete);
                setDir(dir);
            }
        });
        frame.add(panel);
        frame.pack();

        Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
        frame.setLocation(dim.width / 2 - frame.getSize().width / 2, dim.height / 2 - frame.getSize().height / 2);


        panel.setDropTarget(new DropTarget() {
            public synchronized void drop(DropTargetDropEvent evt) {
                try {
                    evt.acceptDrop(DnDConstants.ACTION_COPY);
                    List<File> droppedFiles = (List<File>) evt.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
                    for (File file : droppedFiles) {
                        if (file.isDirectory()) {
                            setDir(file);
                            break;
                        }
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
    }

    public void setDir(File dir) {
        this.dir = dir;
        pathField.setText(dir.getAbsolutePath());
        removeLabel.setText("" + getFilesToDelete(dir).size());
        jpegLabel.setText("" + getJpegs(dir).size());
        pairsLabel.setText("" + getRawsWithJpeg(dir).size());
    }

    public void start() {
        frame.setVisible(true);
    }

    private Collection<File> getRawsWithJpeg(File dir) {
        return Arrays.stream(dir.listFiles())
                .filter(this::isRaw)
                .filter(hasJpeg(dir))
                .collect(Collectors.toSet());
    }

    private Collection<File> getJpegs(File dir) {
        return Arrays.stream(dir.listFiles())
                .filter(this::isJpeg)
                .collect(Collectors.toSet());
    }

    private Collection<File> getFilesToDelete(File dir) {
        return Arrays.stream(dir.listFiles())
                .filter(this::isRaw)
                .filter(not(hasJpeg(dir)))
                .collect(Collectors.toSet());
    }

    private Predicate<File> hasJpeg(File dir) {
        return f ->
                Arrays.stream(dir.listFiles())
                        .filter(this::isJpeg)
                        .map(this::getName)
                        .collect(Collectors.toSet()).contains(getName(f));
    }

    private boolean isJpeg(File file) {
        return JPG_ENDINGS.contains(getEnding(file));
    }

    private boolean isRaw(File file) {
        return RAW_ENDINGS.contains(getEnding(file));
    }

    private String getName(File file) {
        var lastIndex = file.getName().lastIndexOf('.');
        if (lastIndex >= 1)
            return file.getName().substring(0, lastIndex);
        else return file.getName();
    }

    private String getEnding(File file) {
        var nameSplit = file.getName().split("\\.");
        return nameSplit[nameSplit.length - 1].toUpperCase();
    }
}
