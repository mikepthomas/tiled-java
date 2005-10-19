/*
 *  Tiled Map Editor, (c) 2004
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 * 
 *  Adam Turk <aturk@biggeruniverse.com>
 *  Bjorn Lindeijer <b.lindeijer@xs4all.nl>
 *  Rainer Deyke <rainerd@eldwood.com>
 */

package tiled.mapeditor;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Properties;
import java.util.Vector;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import tiled.core.*;
import tiled.mapeditor.util.*;
import tiled.mapeditor.widget.*;


public class TileDialog extends JDialog
    implements ActionListener, ListSelectionListener
{
    private Tile currentTile;
    private TileSet tileset;
    private JList tileList, imageList;
    private Vector imageIds;
    private JTable tileProperties, tileAnimation;
    private AnimationTableModel animationModel;
    private JComboBox tLinkList;
    private JButton bOk, bNew, bDelete, bDuplicate;
    private JButton bAddImage, bReplaceImage, bDeleteImage,
            bDeleteAllUnusedImages;
    private AbstractButton frameAddButton, frameCloneButton, frameDelButton;
    private AbstractButton frameUpButton, frameDownButton;
    private AbstractButton frameChangeImageButton;

    private String location;
    private JTextField tilesetNameEntry;
    private JCheckBox externalBitmapCheck;
    //private JCheckBox sharedImagesCheck;
    private JTabbedPane tabs;
    private int currentImageIndex = -1;
    private int currentFrame = -1;

    public TileDialog(Dialog parent, TileSet s) {
        super(parent, "Edit Tileset '" + s.getName() + "'", true);
        location = "";
        init();
        setTileset(s);
        setCurrentTile(null);
        pack();
        setLocationRelativeTo(getOwner());
    }

    // The following function was taken verbatim from MapEditor.java.  Beware
    // the unnecessary code duplication.
    private ImageIcon loadIcon(String fname) {
        try {
            return new ImageIcon
              (ImageIO.read(MapEditor.class.getResourceAsStream(fname)));
        } catch (java.io.IOException e) {
            System.out.println("Failed to load icon: " + fname);
            return null;
        }
    }

    private AbstractButton createButton(Icon icon, String command) {
        AbstractButton button;
        button = new JButton("", icon);
        button.setMargin(new Insets(0, 0, 0, 0));
        button.setActionCommand(command);
        button.addActionListener(this);
        button.setToolTipText(command);
        return button;
    }

    private JPanel createTilePanel() {
        // Create the buttons

        bDelete = new JButton("Delete Tile");
        // bChangeI = new JButton("Change Image");
        bDuplicate = new JButton("Duplicate Tile");
        bNew = new JButton("Add Tile");

        bDelete.addActionListener(this);
        // bChangeI.addActionListener(this);
        bDuplicate.addActionListener(this);
        bNew.addActionListener(this);

        tileList = new JList();
        tileList.setCellRenderer(new TileDialogListRenderer());


        // Tile properties table

        tileProperties = new JTable(new PropertiesTableModel());
        tileProperties.getSelectionModel().addListSelectionListener(this);
        JScrollPane propScrollPane = new JScrollPane(tileProperties);
        propScrollPane.setPreferredSize(new Dimension(150, 150));

        // Tile animation table

        animationModel = new AnimationTableModel();
        tileAnimation = new JTable(animationModel);
        tileAnimation.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tileAnimation.getSelectionModel().addListSelectionListener(this);
        JScrollPane animScrollPane = new JScrollPane(tileAnimation);
        animScrollPane.setPreferredSize(new Dimension(64, 150));

        // Tile animation buttons

        Icon imgAdd = loadIcon("resources/gnome-new.png");
        Icon imgDel = loadIcon("resources/gnome-delete.png");
        Icon imgDup = loadIcon("resources/gimp-duplicate-16.png");
        Icon imgUp = loadIcon("resources/gnome-up.png");
        Icon imgDown = loadIcon("resources/gnome-down.png");
        Icon imgImage = loadIcon("resources/stock_animation.png");

        frameAddButton = createButton(imgAdd, "Add Frame");
        frameDelButton = createButton(imgDel, "Delete Frame");
        frameCloneButton = createButton(imgDup, "Duplicate Frame");
        frameUpButton = createButton(imgUp, "Move Frame Up");
        frameDownButton = createButton(imgDown, "Move Frame Down");
        frameChangeImageButton = createButton(imgImage, "Change Image");

        JPanel animationButtons = new JPanel();
        animationButtons.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.weightx = 1;
        animationButtons.add(frameAddButton, c);
        animationButtons.add(frameUpButton, c);
        animationButtons.add(frameDownButton, c);
        animationButtons.add(frameCloneButton, c);
        animationButtons.add(frameChangeImageButton, c);
        animationButtons.add(frameDelButton, c);
        animationButtons.setMaximumSize(new Dimension(Integer.MAX_VALUE,
                    animationButtons.getPreferredSize().height));

        JPanel animationPanel = new JPanel();
        animationPanel.setLayout
            (new BoxLayout(animationPanel, BoxLayout.Y_AXIS));
        animationPanel.add(animScrollPane);
        animationPanel.add(animationButtons);

        // Tile list

        tileList.addListSelectionListener(this);
        JScrollPane sp = new JScrollPane();
        sp.getViewport().setView(tileList);
        sp.setPreferredSize(new Dimension(150, 150));

        // The split pane

        JSplitPane subSplitPane = new JSplitPane(
                JSplitPane.HORIZONTAL_SPLIT, true);
        subSplitPane.setLeftComponent(propScrollPane);
        subSplitPane.setRightComponent(animationPanel);

        JSplitPane splitPane = new JSplitPane(
                JSplitPane.HORIZONTAL_SPLIT, true);
        splitPane.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        splitPane.setResizeWeight(0.25);
        splitPane.setLeftComponent(sp);
        splitPane.setRightComponent(subSplitPane);


        // The buttons

        JPanel buttons = new VerticalStaticJPanel();
        buttons.setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0));
        buttons.setLayout(new BoxLayout(buttons, BoxLayout.X_AXIS));
        buttons.add(bNew);
        buttons.add(Box.createRigidArea(new Dimension(5, 0)));
        buttons.add(bDelete);
        // buttons.add(Box.createRigidArea(new Dimension(5, 0)));
        // buttons.add(bChangeI);
        buttons.add(Box.createRigidArea(new Dimension(5, 0)));
        buttons.add(bDuplicate);
        buttons.add(Box.createRigidArea(new Dimension(5, 0)));
        buttons.add(Box.createGlue());


        // Putting it all together

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new GridBagLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        c.fill = GridBagConstraints.BOTH;
        c.weightx = 1; c.weighty = 1;
        mainPanel.add(splitPane, c);
        c.weightx = 0; c.weighty = 0; c.gridy = 1;
        mainPanel.add(buttons, c);

        return mainPanel;
    }

    private JPanel createTilesetPanel()
    {
        JLabel name_label = new JLabel("Name: ");
        tilesetNameEntry = new JTextField(32);
        //sharedImagesCheck = new JCheckBox("Use shared images");
        externalBitmapCheck = new JCheckBox("Use external bitmap");
        //sharedImagesCheck.addActionListener(this);
        externalBitmapCheck.addActionListener(this);

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0; c.gridy = 0;
        mainPanel.add(name_label, c);
        c.gridx = 1; c.gridy = 0;
        mainPanel.add(tilesetNameEntry);
        c.gridx = 0; c.gridy = 1; c.gridwidth = 2;
        //mainPanel.add(sharedImagesCheck, c);
        c.gridx = 0; c.gridy = 2; c.gridwidth = 2;
        mainPanel.add(externalBitmapCheck, c);

        return mainPanel;
    }

    private JPanel createImagePanel()
    {
        imageList = new JList();
        imageList.setCellRenderer(new ImageCellRenderer());
        imageList.setLayoutOrientation(JList.HORIZONTAL_WRAP);
        imageList.addListSelectionListener(this);
        JScrollPane sp = new JScrollPane();
        sp.getViewport().setView(imageList);
        sp.setPreferredSize(new Dimension(150, 150));

        // Buttons
        bAddImage = new JButton("Add Image");
        bAddImage.addActionListener(this);
        bReplaceImage = new JButton("Replace Image");
        bReplaceImage.addActionListener(this);
        bDeleteImage = new JButton("Delete Image");
        bDeleteImage.addActionListener(this);
        bDeleteAllUnusedImages = new JButton("Delete Unused Images");
        bDeleteAllUnusedImages.addActionListener(this);
        JPanel buttons = new VerticalStaticJPanel();
        buttons.setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0));
        buttons.setLayout(new BoxLayout(buttons, BoxLayout.X_AXIS));
        buttons.add(bAddImage);
        buttons.add(Box.createRigidArea(new Dimension(5, 0)));
        buttons.add(bReplaceImage);
        buttons.add(Box.createRigidArea(new Dimension(5, 0)));
        buttons.add(bDeleteImage);
        buttons.add(Box.createRigidArea(new Dimension(5, 0)));
        buttons.add(bDeleteAllUnusedImages);

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new GridBagLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.weightx = 1; c.weighty = 1;
        mainPanel.add(sp, c);
        c.weightx = 0; c.weighty = 0; c.gridy = 1;
        mainPanel.add(buttons, c);
        return mainPanel;
    }

    private void init() {
        tabs = new JTabbedPane(JTabbedPane.TOP);
        tabs.addTab("Tileset", createTilesetPanel());
        tabs.addTab("Tiles", createTilePanel());
        tabs.addTab("Images", createImagePanel());

        bOk = new JButton("OK");
        bOk.addActionListener(this);

        JPanel buttons = new VerticalStaticJPanel();
        buttons.setLayout(new BoxLayout(buttons, BoxLayout.X_AXIS));
        buttons.add(Box.createGlue());
        buttons.add(bOk);

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        mainPanel.add(tabs);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        mainPanel.add(buttons);

        getContentPane().add(mainPanel);
        getRootPane().setDefaultButton(bOk);
    }

    private void changeImage() {
        if (currentTile == null || currentFrame < 0) {
            return;
        }
        TileImageDialog d = new TileImageDialog(this, tileset,
            currentTile.getImageId(), currentTile.getImageOrientation());
        d.setVisible(true);
        if (d.getImageId() >= 0) {
            currentTile.setAnimationFrame(currentFrame,
                d.getImageId(),
                d.getImageOrientation(),
                currentTile.getAnimationFrameDuration(currentFrame));
        }
    }

    private Image loadImage() {
        JFileChooser ch = new JFileChooser(location);
        int ret = ch.showOpenDialog(this);

        if (ret == JFileChooser.APPROVE_OPTION) {
            File file = ch.getSelectedFile();
            try {
                BufferedImage image = ImageIO.read(file);
                if (image != null) {
                    location = file.getAbsolutePath();
                    return image;
                } else {
                    JOptionPane.showMessageDialog(this, "Error loading image",
                            "Error loading image", JOptionPane.ERROR_MESSAGE);
                    return null;
                }
            } catch (Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(this, e.getMessage(),
                        "Error loading image", JOptionPane.ERROR_MESSAGE);
            }
        }

        return null;
    }

    private void newTile() {
        if (tileset.usesSharedImages()) {
            TileImageDialog d = new TileImageDialog(this, tileset);
            d.setVisible(true);
            if (d.getImageId() >= 0) {
                currentTile = new Tile(tileset);
                currentTile.setAppearance(d.getImageId(),
                    d.getImageOrientation());
                tileset.addNewTile(currentTile);
                queryTiles();
            }
            return;
        }

        File files[];
        JFileChooser ch = new JFileChooser(location);
        ch.setMultiSelectionEnabled(true);
        BufferedImage image = null;

        int ret = ch.showOpenDialog(this);
        files = ch.getSelectedFiles();

        for (int i = 0; i < files.length; i++) {
            try {
                image = ImageIO.read(files[i]);
                // TODO: Support for a transparent color
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, e.getMessage(),
                        "Error!", JOptionPane.ERROR_MESSAGE);
                return;
            }

            Tile newTile = new Tile(tileset);
            int image_id = tileset.addImage(image);
            newTile.setAppearance(image_id, 0);
            tileset.addNewTile(newTile);
        }

        if (files.length > 0) {
            location = files[0].getAbsolutePath();
        }

        queryTiles();
    }

    public void setTileset(TileSet s) {
        tileset = s;

        if (tileset != null) {
            // Find new tile images at the location of the tileset
            if (tileset.getSource() != null) {
                location = tileset.getSource();
            } else if (tileset.getMap() != null) {
                location = tileset.getMap().getFilename();
            }
            tilesetNameEntry.setText(tileset.getName());
            //sharedImagesCheck.setSelected(tileset.usesSharedImages());
            externalBitmapCheck.setSelected(tileset.getTilebmpFile() != null);

            tileAnimation.setRowHeight(tileset.getTileHeight());
        }

        queryTiles();
        queryImages();
        updateEnabledState();
    }

    public void queryTiles() {
        Vector listData;
        int curSlot = 0;

        if (tileset != null && tileset.size() > 0) {
            listData = new Vector();
            Iterator tileIterator = tileset.iterator();

            while (tileIterator.hasNext()) {
                Tile tile = (Tile)tileIterator.next();
                listData.add(tile);
            }

            tileList.setListData(listData);
        }

        if (currentTile != null) {
            tileList.setSelectedIndex(currentTile.getId() - 1);
            tileList.ensureIndexIsVisible(currentTile.getId() - 1);
        }
    }

    public void queryImages() {
        Vector listData = new Vector();
        imageIds = new Vector();
        int curSlot = 0;

        Enumeration ids = tileset.getImageIds();
        while(ids.hasMoreElements()) {
            Object key = ids.nextElement();
            Image img = tileset.getImageById(key);
            listData.add(img);
            imageIds.add(key);
        }

        imageList.setListData(listData);
        if (currentImageIndex != -1) {
            imageList.setSelectedIndex(currentImageIndex);
            imageList.ensureIndexIsVisible(currentImageIndex);
        }
    }

    private void updateAnimation()
    {
        if (currentFrame >= 0) {
            this.tileAnimation.changeSelection(this.currentFrame, 0, false,
                false);
        }
        currentFrame = tileAnimation.getSelectedRow();
        updateEnabledState();
    }

    private void setCurrentTile(Tile tile) {
        currentTile = tile;
        animationModel.setTile(currentTile);
        currentFrame = tileAnimation.getSelectedRow();
        updateTileInfo();
        updateEnabledState();
    }

    private void setImageIndex(int i) {
        currentImageIndex = i;
        updateEnabledState();
    }

    private void setCurrentFrame(int n) {
        currentFrame = n;
        updateEnabledState();
    }

    private void updateEnabledState() {
        // boolean internal = (tileset.getSource() == null);
        boolean tilebmp = (tileset.getTilebmpFile() != null);
        boolean tileSelected = (currentTile != null);
        boolean sharedImages = tileset.usesSharedImages();
        boolean atLeastOneSharedImage = sharedImages
          && tileset.getTotalImages() >= 1;

        bNew.setEnabled(atLeastOneSharedImage || !tilebmp);
        bDelete.setEnabled((sharedImages || !tilebmp) && tileSelected);
        // bChangeI.setEnabled((atLeastOneSharedImage || !tilebmp)
        //     && tileSelected);
        bDuplicate.setEnabled((sharedImages || !tilebmp) && tileSelected);
        tileProperties.setEnabled((sharedImages || !tilebmp) && tileSelected);
        externalBitmapCheck.setEnabled(tilebmp); // Can't turn this off yet
        //sharedImagesCheck.setEnabled(!tilebmp || !sharedImages
        //    || tileset.safeToDisableSharedImages());
        tabs.setEnabledAt(2, sharedImages);
        if (sharedImages) {
            bAddImage.setEnabled(!tilebmp);
            bReplaceImage.setEnabled(!tilebmp && currentImageIndex >= 0);
            bDeleteAllUnusedImages.setEnabled(!tilebmp);
            boolean image_used = false;
            Iterator tileIterator = tileset.iterator();

            while (tileIterator.hasNext()) {
                Tile tile = (Tile)tileIterator.next();
                for (int i = 0; i < tile.countAnimationFrames(); ++i) {
                    if (tile.getAnimationFrameImageId(i)
                            == currentImageIndex) {
                        image_used = true;
                    }
                }
            }
            bDeleteImage.setEnabled(!tilebmp && currentImageIndex >= 0
                && !image_used);
        }

        // Update animation buttons
        frameAddButton.setEnabled(currentTile != null);
        frameChangeImageButton.setEnabled(currentTile != null
            && currentFrame >= 0 && atLeastOneSharedImage);
        frameCloneButton.setEnabled(currentTile != null && currentFrame >= 0);
        frameDelButton.setEnabled(currentTile != null && currentFrame >= 0
            && currentTile.countAnimationFrames() > 1);
        frameUpButton.setEnabled(currentTile != null && currentFrame > 0);
        frameDownButton.setEnabled(currentTile != null && currentFrame >= 0
            && currentFrame < currentTile.countAnimationFrames() - 1);
    }

    /**
     * Updates the properties table with the properties of the current tile.
     */
    private void updateTileInfo() {
        if (currentTile == null) {
            return;
        }

        Properties tileProps = currentTile.getProperties();

        // (disabled making a copy, as properties are changed in place now)
        /*
        Properties editProps = new Properties();
        for (Enumeration keys = tileProps.keys(); keys.hasMoreElements();) {
            String key = (String)keys.nextElement();
            editProps.put(key, tileProps.getProperty(key));
        }
        */

        ((PropertiesTableModel)tileProperties.getModel()).update(tileProps);
    }

    public void actionPerformed(ActionEvent event) {
        Object source = event.getSource();

        if (source == bOk) {
            tileset.setName(tilesetNameEntry.getText());
            this.dispose();
        } else if (source == bDelete) {
            int answer = JOptionPane.showConfirmDialog(
                    this, "Delete tile?", "Are you sure?",
                    JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
            if (answer == JOptionPane.YES_OPTION) {
                Tile tile = (Tile)tileList.getSelectedValue();
                if (tile != null) {
                    tileset.removeTile(tile.getId());
                }
                queryTiles();
            }
//        } else if (source == bChangeI) {
//            changeImage();
        } else if (source == bNew) {
            newTile();
        } else if (source == bDuplicate) {
            Tile n = new Tile(currentTile);
            tileset.addNewTile(n);
            queryTiles();
            // Select the last (cloned) tile
            tileList.setSelectedIndex(tileset.size() - 1);
            tileList.ensureIndexIsVisible(tileset.size() - 1);
        } else if (source == externalBitmapCheck) {
            if (!externalBitmapCheck.isSelected()) {
                int answer = JOptionPane.showConfirmDialog(
                        this,
                        "Warning: this operation cannot currently be reversed.\n" +
                        "Disable the use of an external bitmap?",
                       "Are you sure?",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.WARNING_MESSAGE);
                if (answer == JOptionPane.YES_OPTION) {
                    tileset.setTilesetImageFilename(null);
                    updateEnabledState();
                } else {
                    externalBitmapCheck.setSelected(true);
                }
            }
        /*
        } else if (source == sharedImagesCheck) {
            if (sharedImagesCheck.isSelected()) {
                tileset.enableSharedImages();
                updateEnabledState();
            } else {
                int answer = JOptionPane.YES_OPTION;
                if (!tileset.safeToDisableSharedImages()) {
                    answer = JOptionPane.showConfirmDialog(
                        this, "This tileset uses features that require the "
                        + "use of shared images.  Disable the use of shared "
                        + "images?",
                        "Are you sure?",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.QUESTION_MESSAGE);
                }
                if (answer == JOptionPane.YES_OPTION) {
                    tileset.disableSharedImages();
                    updateEnabledState();
                } else {
                    sharedImagesCheck.setSelected(true);
                }
            }
        }
        */
        } else if (source == bAddImage) {
            Image img = loadImage();
            if (img != null) {
                tileset.addImage(img);
            }
            queryImages();
        } else if (source == bReplaceImage) {
            Image img = loadImage();
            if (img != null) {
                tileset.addImage(img, imageIds.get(currentImageIndex));
            }
            queryImages();
        } else if (source == bDeleteImage) {
            int answer = JOptionPane.showConfirmDialog(
                this, "Delete this image?",
                "Are you sure?",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);
            if (answer == JOptionPane.YES_OPTION) {
                tileset.removeImage(imageIds.get(currentImageIndex));
                queryImages();
            }
        } else if (source == bDeleteAllUnusedImages) {
            int answer = JOptionPane.showConfirmDialog(
                this, "Delete all unused images?",
                "Are you sure?",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);
            if (answer == JOptionPane.YES_OPTION) {
           
                Enumeration ids = tileset.getImageIds();
                while(ids.hasMoreElements()) {
                    int id = Integer.parseInt((String)ids.nextElement());
                    boolean image_used = false;
                    Iterator tileIterator = tileset.iterator();

                    while (tileIterator.hasNext()) {
                        Tile tile = (Tile)tileIterator.next();
                        for (int i = 0; i < tile.countAnimationFrames(); ++i) {
                            if (tile.getAnimationFrameImageId(i) == id) {
                                image_used = true;
                            }
                        }
                    }

                    if (!image_used) {
                        tileset.removeImage(Integer.toString(id));
                    }
                }

                queryImages();
            }
        } else if (source == frameAddButton) {
            TileImageDialog d;
            if (currentFrame >= 0) {
                d = new TileImageDialog(this, tileset,
                    currentTile.getAnimationFrameImageId(currentFrame),
                    currentTile.getAnimationFrameOrientation(currentFrame));
            } else {
                d = new TileImageDialog(this, tileset);
            }
            d.setVisible(true);
            if (d.getImageId() >= 0) {
                if (currentFrame < 0) {
                    currentFrame = currentTile.countAnimationFrames();
                }
                currentTile.insertAnimationFrame(currentFrame,
                    d.getImageId(),
                    d.getImageOrientation(), 1);
                this.updateAnimation();
            }
        } else if (source == frameDelButton) {
            this.currentTile.removeAnimationFrame(this.currentFrame);
            this.updateAnimation();
        } else if (source == frameCloneButton) {
            this.currentTile.insertAnimationFrame(this.currentFrame,
                this.currentTile.getAnimationFrameImageId(this.currentFrame),
                this.currentTile.getAnimationFrameOrientation
                    (this.currentFrame),
                this.currentTile.getAnimationFrameDuration(this.currentFrame));
            ++this.currentFrame;
            this.updateAnimation();
        } else if (source == frameUpButton) {
            this.currentTile.swapAnimationFrames(this.currentFrame,
                this.currentFrame - 1);
            --this.currentFrame;
            this.updateAnimation();
        } else if (source == frameDownButton) {
            this.currentTile.swapAnimationFrames(this.currentFrame,
                this.currentFrame + 1);
            ++this.currentFrame;
            this.updateAnimation();
        } else if (source == frameChangeImageButton) {
            changeImage();
        }

        repaint();
    }

    public void valueChanged(ListSelectionEvent e) {
        if (e.getSource() == tileList) {
            setCurrentTile((Tile)tileList.getSelectedValue());
        } else if (e.getSource() == imageList) {
            setImageIndex(imageList.getSelectedIndex());
        } else if (e.getSource() == tileAnimation.getSelectionModel()) {
            setCurrentFrame(tileAnimation.getSelectedRow());
        }
    }

}
