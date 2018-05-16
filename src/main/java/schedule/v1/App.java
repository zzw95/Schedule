package schedule.v1;

import java.awt.Font;
import java.io.PrintStream;
import java.util.ArrayList;

import javax.swing.UIManager;

import org.jb2011.lnf.beautyeye.BeautyEyeLNFHelper;

public class App {

	public static void main(String[] args) {
		
		// TODO Auto-generated method stub
		try {
			
			PrintStream log = new PrintStream("sys.log");
			System.setOut(log);
			
    		BeautyEyeLNFHelper.frameBorderStyle = BeautyEyeLNFHelper.FrameBorderStyle.generalNoTranslucencyShadow;
			org.jb2011.lnf.beautyeye.BeautyEyeLNFHelper.launchBeautyEyeLNF();
			UIManager.put("RootPane.setupButtonVisible", false);
			Font font=new Font("Microsoft YaHei UI", Font.PLAIN, 15);
			UIManager.put("OptionPane.messageFont",font); 
			UIManager.put("OptionPane.buttonFont",font);
			
			UI ui=new UI();
			
			log.close();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		
		
		
		

	}

}
