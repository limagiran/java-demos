package com.limagiran.animations;

import static javax.swing.UIManager.getInstalledLookAndFeels;
import static java.awt.RenderingHints.*;
import static com.limagiran.animations.CircleTranslationAnimationUtils.*;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.util.*;
import java.util.function.BiFunction;
import javax.swing.*;

/**
 *
 * @author Vinicius Silva
 */
public class CircleTranslationAnimationMain {

    //<editor-fold defaultstate="collapsed" desc="static">
    static {
        try {
            for (UIManager.LookAndFeelInfo info : getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception e) {
            //ignore
        }
    }
    //</editor-fold>

    public static void main(String[] args) {
        CircleTranslationAnimation a = new CircleTranslationAnimation();
        SwingUtilities.invokeLater(() -> {
            JFrame f = new JFrame();
            f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            f.setLayout(new BorderLayout(5, 0));
            f.add(a, "Center");
            JPanel cPanel = new JPanel(new FlowLayout());
            SpinnerNumberModel modelCircle1 = new SpinnerNumberModel(a.getCircle1(), 1, 1000, 1);
            SpinnerNumberModel modelCircle2 = new SpinnerNumberModel(a.getCircle2(), 1, 1000, 1);
            SpinnerNumberModel modelRatio = new SpinnerNumberModel(a.getRatio(), 0.01, 100.0, 0.025);
            SpinnerNumberModel modelTickness = new SpinnerNumberModel(a.getThickness(), 1.0, 20.0, 0.5);
            JComboBox cmbbxDelay = new JComboBox(CircleTranslationAnimation.Velocity.values());
            cmbbxDelay.setSelectedItem(a.getVelocity());
            cmbbxDelay.addItemListener(e -> a.setVelocity((CircleTranslationAnimation.Velocity) cmbbxDelay.getSelectedItem()));
            modelTickness.addChangeListener(e -> a.setThickness(modelTickness.getNumber().floatValue()));

            BiFunction<String, Color, Color> function = (s, c) -> Optional.ofNullable(JColorChooser.showDialog(f, s, c)).orElse(c);

            JButton foreground = new JButton("Cor da linha");
            foreground.addActionListener(e -> a.setForeground(function.apply("Selecione a cor da linha", a.getForeground())));

            JButton background = new JButton("Cor do fundo");
            background.addActionListener(e -> a.setBackground(function.apply("Selecione a cor do fundo", a.getBackground())));

            JButton restart = new JButton("Reiniciar");
            restart.addActionListener(e -> a.restart(modelCircle1.getNumber().intValue(), modelCircle2.getNumber().intValue(), modelRatio.getNumber().doubleValue()));

            cPanel.add(cmbbxDelay);
            cPanel.add(foreground);
            cPanel.add(background);
            cPanel.add(new JLabel("Largura da linha"));
            cPanel.add(new JSpinner(modelTickness));
            cPanel.add(new JSeparator(JSeparator.VERTICAL));
            cPanel.add(new JSeparator(JSeparator.VERTICAL));
            cPanel.add(new JLabel("Círculo 1"));
            cPanel.add(new JSpinner(modelCircle1));
            cPanel.add(new JLabel("Círculo 2"));
            cPanel.add(new JSpinner(modelCircle2));
            cPanel.add(new JLabel("Razão"));
            cPanel.add(new JSpinner(modelRatio));
            cPanel.add(restart);
            f.add(cPanel, "First");
            f.pack();
            f.setLocationRelativeTo(null);
            f.setVisible(true);
        });
    }
}

class CircleTranslationAnimation extends JLabel {

    private final List<Point2D.Double> draw = new ArrayList();
    private int circle1;
    private int circle2;
    private float thickness = 1.5f;
    private int progress = 0;
    private Velocity velocity = Velocity.FAST;
    private double ratio;
    private boolean started = true;
    private boolean restart = true;
    private boolean painting = false;

    public CircleTranslationAnimation(int circle1, int circle2, double ratio, Dimension size) {
        this.circle1 = circle1;
        this.circle2 = circle2;
        this.ratio = ratio;
        init(size);
    }

    public CircleTranslationAnimation() {
        this(80, 35, 0.55, new Dimension(400, 300));
    }

    public final void init(Dimension size) {
        setSize(size);
        setPreferredSize(size);
        setForeground(Color.WHITE);
        setBackground(Color.BLACK);
        new Thread(() -> {
            long process = 0L;
            long refresh = 0L;
            while (started) {
                if ((System.nanoTime() - process) > velocity.time) {
                    process();
                    process = System.nanoTime();
                }
                if (!painting && ((System.currentTimeMillis() - refresh) > 25)) {
                    SwingUtilities.invokeLater(CircleTranslationAnimation.this::repaint);
                    refresh = System.currentTimeMillis();
                }
            }
        }).start();
    }

    @Override
    protected void paintComponent(Graphics g) {
        painting = true;
        synchronized (this) {
            Graphics2D g2 = (Graphics2D) g;
            paintAnimation(g2, getSize());
            g2.dispose();
        }
        painting = false;
    }

    public synchronized void paintAnimation(Graphics2D g, Dimension d) {
        int _progress = progress;
        setupGraphics(g);
        g.setColor(getBackground());
        g.fillRect(0, 0, d.width, d.height);
        g.setStroke(new BasicStroke(thickness, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(getForeground());
        List<Point2D.Double> list = new ArrayList(draw.subList(0, (int) Math.min(draw.size(), _progress)));
        Point2D.Double p = new Point2D.Double((d.width / 2), (d.height / 2));
        GeneralPath pol = new GeneralPath(GeneralPath.WIND_EVEN_ODD);
        for (int i = 0; i < list.size(); i++) {
            if (i == 0) {
                pol.moveTo((list.get(i).x + p.x), (list.get(i).y + p.y));
            } else if (i != (list.size() - 1)) {
                pol.lineTo((list.get(i).x + p.x), (list.get(i).y + p.y));
            }
            if (i == (list.size() - 1)) {
                g.draw(pol);
            }
        }
        if (!list.isEmpty()) {
            Point2D.Double p2;
            if (list.size() == draw.size()) {
                p2 = list.get(((list.size() + _progress) % list.size()));
            } else {
                p2 = list.get(list.size() - (_progress % list.size()) - 1);
            }
            g.setStroke(new BasicStroke(thickness + 12f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.drawRect((int) Math.round(p2.x + p.x), (int) Math.round(p2.y + p.y), 0, 0);
        }
    }

    private void process() {
        if (restart) {
            CircleTranslationAnimation.this.restart();
        }
        progress++;
    }

    private synchronized void restart() {
        restart = false;
        progress = 0;
        draw.clear();
        draw.addAll(getPointsToDraw(circle1, circle2, ratio));
    }

    public void restart(int circle1, int circle2, double ratio) {
        this.circle1 = circle1;
        this.circle2 = circle2;
        this.ratio = ratio;
        restart = true;
    }

    public void stop() {
        started = false;
    }

    public float getThickness() {
        return thickness;
    }

    public void setThickness(float thickness) {
        this.thickness = thickness;
    }

    public Velocity getVelocity() {
        return velocity;
    }

    public void setVelocity(Velocity velocity) {
        this.velocity = velocity;
    }

    public int getCircle1() {
        return circle1;
    }

    public int getCircle2() {
        return circle2;
    }

    public double getRatio() {
        return ratio;
    }

    public static enum Velocity {
        SUPERFAST(100000), FAST(500000), NORMAL(1000000), SLOW(5000000), SUPERSLOW(10000000);

        final int time;

        private Velocity(int time) {
            this.time = time;
        }
    }
}

interface CircleTranslationAnimationUtils {

    public static List<Point2D.Double> generateCircleCoordinates(int radius) {
        List<Point2D.Double> _return = new ArrayList();
        for (double angle = 0.0; angle <= (Math.PI * 2); angle += 0.004) {
            _return.add(new Point2D.Double(radius * Math.cos(angle), radius * Math.sin(angle)));
        }
        return _return;
    }

    public static void setupGraphics(Graphics2D g) {
        g.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON);
        g.setRenderingHint(KEY_INTERPOLATION, VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(KEY_RENDERING, VALUE_RENDER_QUALITY);
        g.setRenderingHint(KEY_ALPHA_INTERPOLATION, VALUE_ALPHA_INTERPOLATION_QUALITY);
    }

    public static List<Point2D.Double> getPointsToDraw(int circle1, int circle2, double ratio) {
        final List<Point2D.Double> _return = new ArrayList();
        final List<Point2D.Double> circlePoints1 = generateCircleCoordinates(circle1);
        final List<Point2D.Double> circlePoints2 = generateCircleCoordinates(circle2);
        Long _countCircle1 = null, _countCircle2 = null;
        int countCircle1 = 0;
        double countCircle2 = 0.0;
        while (true) {
            countCircle2 = (countCircle2 + ratio) % circlePoints1.size();
            int reverseCount = (int) Math.abs(circlePoints1.size() - countCircle2) % circlePoints1.size();
            Point2D.Double circlePoint1 = circlePoints1.get(countCircle1);
            Point2D.Double circlePoint2 = circlePoints2.get(reverseCount);
            if (_countCircle1 == null) {
                _countCircle1 = new Long(countCircle1);
                _countCircle2 = (long) countCircle2;
            } else if ((_countCircle1 == countCircle1) && (_countCircle2 == (long) countCircle2)) {
                break;
            }
            countCircle1 = ++countCircle1 % circlePoints1.size();
            _return.add(new Point2D.Double(circlePoint1.x + circlePoint2.x, circlePoint1.y + circlePoint2.y));
        }
        return _return;
    }
}
