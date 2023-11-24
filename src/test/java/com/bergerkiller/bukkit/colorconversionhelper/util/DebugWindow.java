package com.bergerkiller.bukkit.colorconversionhelper.util;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;

/**
 * Displays int[] image contents in a Java window
 */
public class DebugWindow {
    private double _x_dirty = 0.0, _y_dirty = 0.0;
    private int _z_dirty = 0;
    private int _xint = 0, _yint = 0, _zint = 0;
    private double _x = 0.0, _y = 0.0;
    private boolean _event = true;
    private boolean _closing = false;
    private final JLabel label;
    private final Image image;
    private final int scale;

    private DebugWindow(final JLabel label, Image image, int scale) {
        this.image = image;
        this.scale = scale;
        this.label = label;
        this.label.addMouseMotionListener(new MouseMotionListener() {
            @Override
            public void mouseDragged(MouseEvent e) {
                _x_dirty = (double) e.getX() / (double) label.getWidth();
                _y_dirty = (double) e.getY() / (double) label.getHeight();
                signal();
            }

            @Override
            public void mouseMoved(MouseEvent e) {
            }
        });
        this.label.addMouseWheelListener(e -> {
            if (e.getWheelRotation() > 0) {
                _z_dirty++;
                signal();
            } else if (e.getWheelRotation() < 0) {
                _z_dirty--;
                signal();
            }
        });
        updateImage();
    }

    private void updateImage() {
        this.label.setIcon(new ImageIcon(image.getScaledInstance(image.getWidth(null) * scale, image.getHeight(null) * scale, 0)));
    }

    public synchronized boolean waitNext() {
        this.updateImage();
        _event = false;
        while (!_event) {
            try {
                this.wait();
            } catch (InterruptedException ignore) {}
        }
        _x = _x_dirty;
        _y = _y_dirty;
        _xint = (int) (this._x * this.image.getWidth(null));
        _yint = (int) (this._y * this.image.getHeight(null));
        _zint = _z_dirty;
        return !_closing;
    }

    public int x() {
        return this._xint;
    }

    public int y() {
        return this._yint;
    }

    public int z() {
        return this._zint;
    }

    public static int clamp(int value, int limit) {
        return clamp(value, -limit, limit);
    }

    public static int clamp(int value, int min, int max) {
        return value < min ? min : (value > max ? max : value);
    }


    public int x(int min, int max) {
        return clamp(this._xint, min, max);
    }

    public int y(int min, int max) {
        return clamp(this._yint, min, max);
    }

    public int z(int min, int max) {
        return clamp(this._zint, min, max);
    }

    public double fx() {
        return this._x;
    }

    public double fy() {
        return this._y;
    }

    private synchronized void signal() {
        _event = true;
        this.notifyAll();
    }

    public void waitForever() {
        while (waitNext());
    }

    public static void showMapForeverAutoScale(Image image) {
        showMapAutoScale(image).waitForever();
    }

    public static void showMapForever(Image image) {
        showMap(image).waitForever();
    }

    public static void showMapForever(Image image, int scale) {
        showMap(image, scale).waitForever();
    }

    public static DebugWindow showMapAutoScale(Image image) {
        java.awt.Dimension screenSize = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
        double width = screenSize.getWidth();
        double height = screenSize.getHeight();
        double scale = Math.min(width / (image.getWidth(null) + 32), height / (image.getHeight(null) + 64));
        return showMap(image, Math.max(1, (int) Math.floor(scale)));
    }

    public static DebugWindow showMap(Image image) {
        return showMap(image, 1);
    }

    public static DebugWindow showMap(Image image, int scale) {
        final DebugWindow window = new DebugWindow(new JLabel(), image, scale);

        JFrame f = new JFrame();
        f.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        f.getContentPane().setLayout(new GridLayout(1,2));
        f.getContentPane().add(window.label);
        f.pack();
        f.setLocationRelativeTo(null);
        f.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                window._closing = true;
                window.signal();
            }
        });
        f.setVisible(true);
        return window;
    }
}
