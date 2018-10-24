import java.awt.Color;
import java.awt.Image;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JSlider;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;

/*
 * WeFax slant corrector and position modifier
 * Made by HA8MZ (Mate Pinter) 
 * 2017 07 25   v1.0
 */


public class SlantCorrector extends JFrame{
	
	
	private static final long serialVersionUID = 8979277578722035760L;
	private BufferedImage input;
	private int lastSlant = 0, lastPos = 0;
	private File url;
	private JButton openBtn, saveBtn, deleteBtn, denoiseBtn;
	private JCheckBox rangeBox;
	private JSlider slantSlider;
	private JSlider posSlider, deleteRows;
	private JLabel picLabel, infoLabel, slantLabel, posLabel;
	private Thread thrd;
	private boolean runThread = false;
	private int[] xDirs = {0,  1, 1, 1, 0,-1,-1,-1};
	private int[] yDirs = {-1,-1, 0, 1, 1, 1, 0,-1};
	
	public SlantCorrector(){													//Constructor
		this.setLayout(null);
		this.setTitle("HA8MZ WeFax slant corrector v1.2 - Please open image!");
		this.setSize(1024, 768);
		this.setDefaultCloseOperation(EXIT_ON_CLOSE);
		this.setResizable(false);
		Image ic = null;
		try {
		    ic = ImageIO.read(this.getClass().getResource("icon.bmp"));
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		this.setIconImage(ic);
		infoLabel = new JLabel("Please open image!");
		infoLabel.setBounds(10, 30, 200, 20);
		this.add(infoLabel);
		slantLabel = new JLabel("Slant");
		slantLabel.setBounds(290, 30, 100, 20);
		this.add(slantLabel);
		posLabel = new JLabel("Position");
		posLabel.setBounds(790, 30, 100, 20);
		this.add(posLabel);
		openBtn = new JButton("Open");
		openBtn.setBounds(10, 10, 100, 20);
		openBtn.addActionListener((l)->{
			openClick();
		});
		this.add(openBtn);
		deleteBtn = new JButton("Delete updated files");
		deleteBtn.setBounds(350, 30, 150, 20);
		deleteBtn.addActionListener((l)->{
			List<File> searched = new ArrayList<File>();
			if(url == null) return;
			File dir = url.getParentFile();
			for(File act : dir.listFiles()) {
				if(!act.isDirectory()) searched.add(act);
			}
			for(File deleteable : searched) {
				for(File similar : searched) {
					if(!deleteable.equals(similar) && deleteable.getName().length() > 4) {
						if(similar.getName().equals(deleteable.getName().substring(0, deleteable.getName().length()-4)+ "_correct.png")) {
							int result = JOptionPane.showConfirmDialog(this, 
							   "Are you sure you wish to delete "+ deleteable.getName() + " ?",null, JOptionPane.YES_NO_OPTION);
							if(result == JOptionPane.YES_OPTION) {
							    deleteable.delete();
							} 
						}
					}
				}
			}
		});
		this.add(deleteBtn);
		saveBtn = new JButton("Save");
		saveBtn.setBounds(500, 10, 100, 20);
		saveBtn.addActionListener((e)->{
			if(input == null) return;
			if(!runThread){
				runThread = true;
				thrd = new Thread(new Runnable() {
					@Override
					public void run() {
						saveClick();
						runThread = false;
						infoLabel.setText("Done");
						slantSlider.setEnabled(true);
						posSlider.setEnabled(true);
					}
				});					
				slantSlider.setEnabled(false);
				posSlider.setEnabled(false);
				thrd.start();
			}
		});
		this.add(saveBtn);
		rangeBox = new JCheckBox("Bigger range");
		rangeBox.setBounds(500,30,100,20);	
		rangeBox.addItemListener((e)-> {
			if(rangeBox.isSelected()){
				slantSlider.setMaximum(1500);
				slantSlider.setMinimum(-1500);
				posSlider.setMaximum(2000);
				posSlider.setMinimum(-2000);
			} else {
				slantSlider.setMaximum(1000);
				slantSlider.setMinimum(-1000);
				posSlider.setMaximum(800);
				posSlider.setMinimum(-800);
			}		
		});
		rangeBox.setRolloverEnabled(false);
		this.add(rangeBox);
		denoiseBtn = new JButton("Denoise");
		denoiseBtn.setBounds(600, 30, 80, 20);
		denoiseBtn.addActionListener((e)->{
			if(input == null) return;
			if(!runThread){
				runThread = true;
				thrd = new Thread(new Runnable() {
					@Override
					public void run() {
						denoise();
						//deleteRow();
						picLabel.setIcon(new ImageIcon(resize()));
						repaint();
						runThread = false;
						infoLabel.setText("Done");
					}
				});
				infoLabel.setText("Working...");
				thrd.start();
			}
		});
		this.add(denoiseBtn);
		deleteRows = new JSlider();
		deleteRows.setMinimum(1);
		deleteRows.setMaximum(100);
		deleteRows.setValue(90);
		deleteRows.setBounds(680, 30, 100, 20);
		deleteRows.addChangeListener((e)->{
			if(input == null) return;
			if(!runThread){
				runThread = true;
				thrd = new Thread(new Runnable() {
			         @Override
			         public void run() {
			        	 deleteRow();
			        	 picLabel.setIcon(new ImageIcon(resize()));
				        repaint();
		              	runThread = false;
		              	infoLabel.setText("Done");
			         }
				});
				infoLabel.setText("Working...");
				thrd.start();
			}
		});
		
		this.add(deleteRows);
		this.url = new File("");
		this.input = null;
		this.picLabel = new JLabel("Input image");
		this.add(picLabel);
		slantSlider = new JSlider();
		slantSlider.setMaximum(1000);
		slantSlider.setMinimum(-1000);
		slantSlider.setValue(0);
		slantSlider.addChangeListener((e)-> {
			if(input == null) return;
			if(!runThread){
				runThread = true;
				thrd = new Thread(new Runnable() {
					@Override
					public void run() {
						slantLabel.setText("Slant: " + slantSlider.getValue());
						slantSliderChanged();
						runThread = false;
						infoLabel.setText("Done");
					}
				});
				infoLabel.setText("Working...");
				thrd.start();
			}

		});
		slantSlider.setBounds(120, 10, 370, 20);
		this.add(slantSlider);
		posSlider = new JSlider();
		posSlider.setMaximum(800);
		posSlider.setMinimum(-800);
		posSlider.setValue(0);
		posSlider.addChangeListener((e)-> {
			if(input == null) return;
			if(!runThread){
				runThread = true;
				thrd = new Thread(new Runnable() {
					@Override
					public void run() {
						posLabel.setText("Position: " + posSlider.getValue());
						posSliderChanged();
						runThread = false;
						infoLabel.setText("Done");
					}
				});
				infoLabel.setText("Working...");
				thrd.start();
			}

		});
		posSlider.setBounds(610,10,400,20);
		this.add(posSlider);
	}
	
	private void openClick(){														//Image open method
		lastSlant = 0;
		lastPos = 0;
		slantSlider.setValue(0);
		posSlider.setValue(0);
		deleteRows.setValue(90);
		JFileChooser jc = new JFileChooser();
		FileFilter imageFilter = new FileNameExtensionFilter(
			    "Image files", ImageIO.getReaderFileSuffixes());
		jc.setFileFilter(imageFilter);
		jc.setAcceptAllFileFilterUsed(false);
		if(!url.toString().equals("")){
			jc.setSelectedFile(url);			
		} else {
			 Path startingDir = Paths.get("C:\\ProgramData\\JVComm32\\");
			Finder finder = new Finder("HF-Fax");
			 try {
				Files.walkFileTree(startingDir, finder);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		     if(finder.done()!=null) {
		    	 jc.setCurrentDirectory(finder.done());
		     };
		}
		int returnVal = jc.showOpenDialog(this);
		if(returnVal == JFileChooser.APPROVE_OPTION){
			url = jc.getSelectedFile();
			try{
				input = ImageIO.read(url);
				BufferedImage o = resize();
				picLabel.setBounds(10, 50, o.getWidth(), o.getHeight());
				picLabel.setIcon(new ImageIcon(resize()));
				infoLabel.setText("Done loading!");
				this.setTitle("HA8MZ WeFax slant corrector v1.1 - " + url.toString());
				slantSlider.setValue(0);
				posSlider.setValue(0);
				rangeBox.setSelected(false);
				repaint();
				
			} catch (IOException e){
				infoLabel.setText("Error loading image");
			}
			
		}
	}
	
	private void saveClick(){														//Image save method
		if(input == null) return;
		JFileChooser jc = new JFileChooser();
		FileFilter imageFilter = new FileNameExtensionFilter(
			    "Image files", ImageIO.getReaderFileSuffixes());
		jc.setFileFilter(imageFilter);
		jc.setAcceptAllFileFilterUsed(false);
		File urlOut2 = new File(url.toString().substring(0, url.toString().length()-4)+"_correct.png");
		jc.setSelectedFile(urlOut2);
		int returnVal = jc.showSaveDialog(this);
		if(returnVal == JFileChooser.APPROVE_OPTION){
			try{
				infoLabel.setText("Save in progress...");
				String urlOut = jc.getSelectedFile().toString();
				if(!urlOut.contains(".png")) urlOut += ".png";
				ImageIO.write(input, "PNG", new File(urlOut));
			} catch (IOException e){
				System.out.println("Error in write process!");
			}
		}
	}
	
	private void slantSliderChanged(){													//Slant slider changed
		this.slant(-lastSlant, false); 
        this.slant(slantSlider.getValue(), false);
        lastSlant = slantSlider.getValue();
        picLabel.setIcon(new ImageIcon(resize()));
        this.repaint();
	}
	
	private void posSliderChanged(){													//Position slider changed
		this.slant(-lastPos, true); 
        this.slant(posSlider.getValue(), true);
        lastPos = posSlider.getValue();
        picLabel.setIcon(new ImageIcon(resize()));
        this.repaint();
	}
	
	 public void slant(int var, boolean noSlide){        								//Slant algorythm
        BufferedImage newImg = new BufferedImage(input.getWidth(), input.getHeight(), BufferedImage.TYPE_BYTE_INDEXED);       
        int adder = 0;
        int start = 0;
        for(int y = 0; y < input.getHeight();y++){
            int newX = start;
            for(int x = 0; x < input.getWidth(); x++){
                if(var >= 0){
                    if(newX >= input.getWidth()){
                        newX = 0;
                    }
                     Color in = new Color(input.getRGB(x, y), true);
                     newImg.setRGB(newX, y, in.getRGB());
                     newX++;
                } else {
                    if(newX >= input.getWidth()){
                        newX = 0;
                    }
                     Color in = new Color(input.getRGB(newX, y), true);
                     newImg.setRGB(x, y, in.getRGB());
                     newX++;
                }
            }
            if(!noSlide){
	            adder += var;
	            start = Math.abs(adder/1000);
            } else start = Math.abs(var);
            if(start >= input.getWidth() || start <= 0) start = 0;
        }
        input = newImg;       
    }
	 
	 private void denoise() {
	     BufferedImage newImg = new BufferedImage(input.getWidth(), input.getHeight(), BufferedImage.TYPE_BYTE_INDEXED);       
	   
		     for(int x = 0; x < input.getWidth(); x++){					//Delete grey pixels
		    	 for(int y = 0; y < input.getHeight(); y+=1) {
		    		 Color actual = new Color(input.getRGB(x, y), true);
		    		 if(isIsolatedPixel(x,y) && actual.getRed() < 200) {
			    			newImg.setRGB(x, y, new Color(255,255,255).getRGB());
			    		} else {
			    			newImg.setRGB(x, y, actual.getRGB());
			    		}
		    		
		    	 }
		     }
	    
	     input = newImg;
	     
	 //    invert();
	 }
	 
	 private void deleteRow() {
	     BufferedImage newImg = new BufferedImage(input.getWidth(), input.getHeight(), BufferedImage.TYPE_BYTE_INDEXED);       
		   
	     				//Delete grey pixels
	     for(int y = 0; y < input.getHeight(); y+=1) {
	    	 int black = 0;
	    	 for(int x = 0; x < input.getWidth(); x++){	
	    		 Color actual = new Color(input.getRGB(x, y), true);
	    		 if(actual.getRed() < 100) {
		    			black++;
		    		}
	    		
	    	 }
	    	 double num = black/(double)input.getWidth();
	    	 if(num > (double)deleteRows.getValue()/100.0 && num < 0.8) {
	    		 for(int x = 0; x < input.getWidth(); x++){	
		    		newImg.setRGB(x, y, new Color(255,255,255).getRGB());
		    	 }
	    	 } else {
	    		 for(int x = 0; x < input.getWidth(); x++){	
			    	newImg.setRGB(x, y, input.getRGB(x, y));
	    		 }
	    	 }
	     }
    
     input = newImg;
     
	 }
	 
	 private boolean isIsolatedPixel(int x, int y) {
		 boolean hasNeighbor = false;
		 for(int i = 0; i < 8; i++) {
			 int newX = x + xDirs[i];
			 int newY = y + yDirs[i];
			 if(newX >= 0 && newX < input.getWidth() && newY >= 0 && newY < input.getHeight()) {
				 Color c = new Color(input.getRGB(newX, newY), true);
				 if(isDark(c)) {
					 hasNeighbor = true;
					 break;
				 }
			 }
		 }
		 return !hasNeighbor;
	 }
	 
	 private boolean isDark(Color c) {
		 return c.getRed() < 150;
	 }
	 
	
	 
	 private BufferedImage resize(){												//Resize for panel
		BufferedImage after = new BufferedImage(input.getWidth(), input.getHeight(), BufferedImage.TYPE_BYTE_INDEXED);
	    AffineTransform at = new AffineTransform();
	    float xScale = 1024.0f/input.getWidth();
	    float yScale = 768.0f/input.getHeight();
	    float scaleVal = Math.min(xScale, yScale);
	    if(scaleVal>1.0) scaleVal = 1.0f;
	    at.scale(scaleVal, scaleVal);
	    AffineTransformOp scale = new AffineTransformOp(at, AffineTransformOp.TYPE_BILINEAR);
	    after = scale.filter(input, after);
	    return after;
	 }
	
	public static void main(String[] args){										    //Main method
		SlantCorrector sc = new SlantCorrector();
		sc.setVisible(true);
	}	
}
