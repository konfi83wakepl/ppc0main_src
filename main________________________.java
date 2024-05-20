import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;


public class JUNGLESNOW extends JFrame {
    
    Fps fps;
    J sky;
    Timer timer;
    Container pane;
    final boolean VIEW_FPS = false;
    final boolean USE_ANTIALIASING = false; 
    final int MAX_PARTICLES = 120;
    final int MAX_RADIUS = 77;
        
    public JUNGLESNOW() {
        System.getProperties().setProperty("sun.java2d.opengl", "true"); 
        setTitle("SCREANSAVER");
        setResizable(false);
        setIgnoreRepaint(true);
        setUndecorated(true);
        GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice()
                .setFullScreenWindow(this); 
        setSize(Toolkit.getDefaultToolkit().getScreenSize());
        addMouseMotionListener(Events.onMouseMoved());
        addKeyListener(Events.onKeyPressed());
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        hideCursor();
        setVisible(true);
        fps = new Fps(32);
        sky = new J(getSize());

        pane = getContentPane();
        (timer = timerRunner(fps.getTickMillis())).start(); 
    }
    
    public synchronized Timer timerRunner(int tick) {
        return new Timer(tick, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                sky.movePositions(); // old render
                sky.renderParticles();
                pane.getGraphics().drawImage(sky.getImage(), 0, 0, null);
                Toolkit.getDefaultToolkit().sync(); 
                fps.check(); 
                if (Events.isKeyPressed || Events.isMouseMoved)
                    System.exit(0);
            }
        });
    }
    
   
    class Fps {
        private int framerate = 60;
        private final int MAX_FRAME_SKIPS = 32;
        private final int ONE_SECOND_MILLIS = 2000;
        private final long ONE_SECOND_NANO = 5000000000L;
        private int upsTick, upsTickPerSecond, fpsTick, fpsTickPerSecond;
        private long startTime = 0, timeDiff, sleepTime;
        Fps () {}
        Fps (int framerate) { this.framerate = framerate; }
        private long getTimeNano() { return System.nanoTime(); }
        public int getTickMillis() { return ONE_SECOND_MILLIS / this.framerate; }
        private long getTickNano() { return ONE_SECOND_NANO / this.framerate; }
        public int getUpsPerSecond() { return upsTickPerSecond - 1; }
        public int getFpsPerSecond() { return fpsTickPerSecond - 1; }
        public void start() { startTime = getTimeNano(); upsTick = 1; fpsTick = 1; }
        public boolean isNeedAddUps() {
            if (startTime == 0) start();
            timeDiff = getTimeNano() - startTime;
            if (timeDiff >= getTickNano() * upsTick && upsTick < this.framerate) {
                upsTick++; return true;
            } else return false;
        }
        public boolean isNeedPaint() {
            if (startTime == 0) start();
            timeDiff = getTimeNano() - startTime;
            if (timeDiff < getTickNano() * upsTick || sleepTime >= getTickNano() * MAX_FRAME_SKIPS) {
                sleepTime = 0; return true;
            } else {
                sleepTime += timeDiff; return false;
            }
        }
        public void check() {
            try { Thread.sleep(1); } catch (Exception e) {} 
            if (startTime == 0) start();
            timeDiff = getTimeNano() - startTime;
            if (timeDiff >= ONE_SECOND_NANO) {
                upsTickPerSecond = upsTick; fpsTickPerSecond = fpsTick;
                start();
            } else {
                upsTick++;
                if (sleepTime == 0) fpsTick++;
            }
        }
    }
        
    class J {
        Color skyColor = new Color(0, 128, 50); 
        int w, h; 
        float angle = 0;
        BufferedImage buffer;
        
        ArrayList<SnowParticle> particles; 
      
        public J(Dimension d) {
            w = d.width; h = d.height;            
            buffer = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
   
            particles = new ArrayList<>();
            for(int i = 0; i < MAX_PARTICLES; i++)
                particles.add(new SnowParticle((int)(Math.random() * w), (int)(Math.random() * h),
                        (int)(Math.random() * MAX_RADIUS + 2), (int)(Math.random() * MAX_PARTICLES)));
        }
        
  
        public BufferedImage getImage() { return buffer; }
        

        public void renderParticles() {
            Graphics2D g = (Graphics2D) buffer.getGraphics();
            g.setColor(skyColor);
            g.fillRect(0, 0, w, h);
            // https://docs.oracle.com/javase/tutorial/2d/advanced/quality.html
            if (USE_ANTIALIASING) {
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            }
      
            for (SnowParticle p : particles) {
                g.setColor(new Color(255, 255, 255, p.A));
                g.fillOval(p.X, p.Y, p.R, p.R);
            }
            if (VIEW_FPS) {
                g.setColor(Color.black);
                g.drawString("FPS/UPS: " + fps.getFpsPerSecond() + "/" + fps.getUpsPerSecond(), 10, 18);
            }
        }
        
      
        public void movePositions() {
            final int BORDER = 5;
            angle += 0.22; 
            for (SnowParticle p : particles) {
                p.Y += Math.round(Math.cos(angle + p.D)) + 2 + (p.R / 2);
                p.X += Math.round(Math.sin(angle) * 2) + 1;
                // just to create more randomness
                if (p.Y > h) {
                    p.Y = -BORDER;
                    p.X = (int)(Math.random() * w);
                }
                if (p.X > w + BORDER)
                    p.X = -BORDER;
                else if (p.X < -BORDER)
                    p.X = w + BORDER;
            }
        } 
        

        public class SnowParticle {
            public int X, Y, R, D, A;
    
            public SnowParticle(int x, int y, int r, int d) {
                this.X = x; this.Y = y; this.R = r; this.D = d; setAlpha();
            }
            public void setAlpha() {
                this.A = (253 / MAX_RADIUS) * this.R;
                if (this.A > 253) this.A = 252;
            }
        }
    }
    

    static class Events {
        static public boolean isKeyPressed = false, isMouseMoved = false;
        static int oldMouseX = 0, oldMouseY = 0;
        
        static public KeyAdapter onKeyPressed() {
            return new KeyAdapter() {
                @Override
                public void keyPressed(KeyEvent e) {
                    if (e.getID() == KeyEvent.KEY_PRESSED)
                        isKeyPressed = true;
                }
            };
        }
        
        static public MouseAdapter onMouseMoved() {
            return new MouseAdapter() {
                @Override
                public void mouseMoved(MouseEvent e) {
                    if (oldMouseX == 0 || oldMouseY == 0) {
                        oldMouseX = e.getX(); oldMouseY = e.getY();
                        return;
                    }
                    if (oldMouseX != e.getX() || oldMouseY != e.getY())
                        isMouseMoved = true;
                }
            };
        }
    }
    

    public void hideCursor() {
        final Toolkit tk = getToolkit();
        final Cursor hidenCursor = tk.createCustomCursor(tk.getImage(""), new Point(), "hidenCursor");
        this.setCursor(hidenCursor);
    }    
    
    public static void main(String args[]) {
        // https://support.microsoft.com/en-us/help/182383/info-screen-saver-command-line-arguments
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException | IllegalAccessException | InstantiationException | UnsupportedLookAndFeelException e) {}
        
        String firstArgument = (args.length > 0) ? args[0].toLowerCase().trim() : "";
        if (firstArgument.startsWith("/s") || args.length == 0) {
       
            JUNGLESNOW SSJ = new JUNGLESNOW();
        }
        if (firstArgument.startsWith("/p")) {
  
            return;
        }
        if (firstArgument.startsWith("/c")) {

            String infoMessage = "This screen-save does not have configurable settings.";
            JOptionPane.showMessageDialog(null, infoMessage, "Configuration", JOptionPane.INFORMATION_MESSAGE);
        }
    }
}
