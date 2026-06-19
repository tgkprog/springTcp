package com.tcp.tgk.client.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.JPanel;

public class BooleanIndicator extends JPanel {
    private boolean status;
    private Color trueColor;
    private Color falseColor;

    public BooleanIndicator(boolean initialStatus) {
        this.status = initialStatus;
        this.trueColor = new Color(16, 185, 129); // Modern Emerald Green
        this.falseColor = new Color(245, 158, 11); // Modern Amber Yellow
        setOpaque(false);
    }

    public void setStatus(boolean status) {
        this.status = status;
        repaint();
    }

    public boolean isStatus() {
        return status;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();

        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int size = Math.min(getWidth(), getHeight()) - 4;
        int x = (getWidth() - size) / 2;
        int y = (getHeight() - size) / 2;

        g2.setColor(status ? trueColor : falseColor);
        g2.fillOval(x, y, size, size);

        g2.setColor(Color.DARK_GRAY);
        g2.drawOval(x, y, size, size);

        g2.dispose();
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(20, 20);
    }
}
