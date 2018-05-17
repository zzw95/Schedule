package schedule.v2;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLObjectProperty;

import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.QueryExecutionException;
import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;

public class UI {
	JFrame frame;
	JPanel choosePanel, ontoPanel, importPanel, cqlPanel, schedulePanel, checkBoxPanel;
	JTextField inputOntoField, inputDbField, inputExcelField, cqlField, outputExcelField, exportExcelField;
	JButton inputOntoButton, loadOntoButton, queryOntoButton, inputDbButton, inputExcelButton, startDbButton, importDbButton, runCqlButton, outputExcelButton, genButton, exportExcelButton, exportButton;
	JComboBox ontoComboBox;
	JTextArea ontoInfoTextArea, cqlInfoTextArea;
	JCheckBox mergeCheckBox, stockCheckBox;  
	File tempDir;
	
	KnowledgeBase kb;
	DatabaseUD db;
	GraphDatabaseService graphDb;
	
	public UI(){
		Initialize();
		kb=null;
		db=null;
		graphDb=null;
		
	}
	
//	protected void finalize() throws Throwable{  
//        super.finalize();  
//        if(db!=null){
//        	db.Shutdown();
//        }
//        System.out.println("END");  
//    } 
	
	private void Initialize(){
		frame = new JFrame();
		frame.getContentPane().setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 15));
		frame.getContentPane().setLayout(new BorderLayout(0,0));
		
		frame.addWindowListener(new windowAL());
		
		choosePanel = new JPanel();
		frame.getContentPane().add(choosePanel, BorderLayout.NORTH);
		GridBagLayout gbl_choosePanel = new GridBagLayout();
		gbl_choosePanel.columnWidths = new int[]{0, 0};
		gbl_choosePanel.rowHeights = new int[]{200, 125, 250,0};
 		gbl_choosePanel.columnWeights = new double[]{1.0, Double.MIN_VALUE};
//		gbl_choosePanel.rowWeights = new double[]{1.0, 1.0, 1.0,Double.MIN_VALUE};
		choosePanel.setLayout(gbl_choosePanel);
		
		ontoPanel = new JPanel();
		ontoPanel.setBorder(new TitledBorder(null, "Ontology Query", TitledBorder.CENTER, TitledBorder.TOP, new Font("Microsoft YaHei UI", Font.PLAIN, 18), null));
		GridBagConstraints gbc_ontoPanel = new GridBagConstraints();
		gbc_ontoPanel.ipady = 5;
		gbc_ontoPanel.ipadx = 5;
		gbc_ontoPanel.insets = new Insets(5, 5, 0, 5);
		gbc_ontoPanel.fill = GridBagConstraints.BOTH;
		gbc_ontoPanel.gridx = 0;
		gbc_ontoPanel.gridy = 0;
		choosePanel.add(ontoPanel, gbc_ontoPanel);
		GridBagLayout gbl_ontoPanel = new GridBagLayout();
		gbl_ontoPanel.columnWidths = new int[]{200, 0};
		gbl_ontoPanel.rowHeights = new int[]{0, 0, 45,0};
		gbl_ontoPanel.columnWeights = new double[]{0.0, 1.0};
		//gbl_ontoPanel.rowWeights = new double[]{0.0, 0.0, 0.0,0.0};
		ontoPanel.setLayout(gbl_ontoPanel);
		
		inputOntoButton = new JButton("Choose Ontology");
		inputOntoButton.addActionListener(new inputOntoButtonAL());
		inputOntoButton.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 15));
		GridBagConstraints gbc_inputOntoButton = new GridBagConstraints();
		gbc_inputOntoButton.fill = GridBagConstraints.BOTH;
		gbc_inputOntoButton.insets = new Insets(5, 5, 5, 5);
		gbc_inputOntoButton.gridx = 0;
		gbc_inputOntoButton.gridy = 0;
		ontoPanel.add(inputOntoButton, gbc_inputOntoButton);
		
		inputOntoField = new JTextField();
		inputOntoField.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 15));
		GridBagConstraints gbc_inputOntoField = new GridBagConstraints();
		gbc_inputOntoField.insets = new Insets(5, 5, 5, 5);
		gbc_inputOntoField.fill = GridBagConstraints.BOTH;
		gbc_inputOntoField.gridx = 1;
		gbc_inputOntoField.gridy = 0;
		ontoPanel.add(inputOntoField, gbc_inputOntoField);
		
		loadOntoButton = new JButton("Load");
		loadOntoButton.addActionListener(new loadOntoButtonAL());
		loadOntoButton.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 15));
		GridBagConstraints gbc_loadOntoButton = new GridBagConstraints();
		gbc_loadOntoButton.fill = GridBagConstraints.BOTH;
		gbc_loadOntoButton.insets = new Insets(5, 5, 5, 5);
		gbc_loadOntoButton.gridx = 0;
		gbc_loadOntoButton.gridy = 1;
		ontoPanel.add(loadOntoButton, gbc_loadOntoButton);
		
		ontoComboBox = new JComboBox();
		ontoComboBox.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 15));
		GridBagConstraints gbc_ontoComboBox = new GridBagConstraints();
		gbc_ontoComboBox.fill = GridBagConstraints.BOTH;
		gbc_ontoComboBox.insets = new Insets(5, 5, 5, 5);
		gbc_ontoComboBox.gridx = 0;
		gbc_ontoComboBox.gridy = 2;
		ontoPanel.add(ontoComboBox, gbc_ontoComboBox);
		
		queryOntoButton = new JButton("Query");
		queryOntoButton.addActionListener(new queryOntoButtonAL());
		queryOntoButton.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 15));
		GridBagConstraints gbc_queryOntoButton = new GridBagConstraints();
		gbc_queryOntoButton.fill = GridBagConstraints.BOTH;
		gbc_queryOntoButton.insets = new Insets(5, 5, 5, 5);
		gbc_queryOntoButton.gridx = 0;
		gbc_queryOntoButton.gridy = 3;
		ontoPanel.add(queryOntoButton, gbc_queryOntoButton);
		
		ontoInfoTextArea = new JTextArea();
		ontoInfoTextArea.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 15));
		ontoInfoTextArea.setWrapStyleWord(true);
		JScrollPane scrollPane = new JScrollPane(ontoInfoTextArea);
		scrollPane.setViewportBorder(null);
		GridBagConstraints gbc_scrollPane = new GridBagConstraints();
		gbc_scrollPane.insets = new Insets(5, 5, 5, 5);
		gbc_scrollPane.anchor = GridBagConstraints.CENTER;
		gbc_scrollPane.fill = GridBagConstraints.BOTH;
		gbc_scrollPane.gridx = 1;
		gbc_scrollPane.gridy = 1;
		gbc_scrollPane.gridheight = GridBagConstraints.REMAINDER;
		gbc_scrollPane.gridwidth = GridBagConstraints.REMAINDER;
		ontoPanel.add(scrollPane, gbc_scrollPane);
		
		importPanel = new JPanel();
		importPanel.setBorder(new TitledBorder(null, "Graph Data Import", TitledBorder.CENTER, TitledBorder.TOP, new Font("Microsoft YaHei UI", Font.PLAIN, 18), null));
		GridBagConstraints gbc_importPanel = new GridBagConstraints();
		gbc_importPanel.ipady = 5;
		gbc_importPanel.ipadx = 5;
		gbc_importPanel.insets = new Insets(5, 5, 0, 5);
		gbc_importPanel.fill = GridBagConstraints.BOTH;
		gbc_importPanel.gridx = 0;
		gbc_importPanel.gridy = 1;
		choosePanel.add(importPanel, gbc_importPanel);
		GridBagLayout gbl_importPanel = new GridBagLayout();
		gbl_importPanel.columnWidths = new int[]{150, 0,100};
		gbl_importPanel.rowHeights = new int[]{0, 0, 0};
		gbl_importPanel.columnWeights = new double[]{0.0, 1.0,0.0};
		gbl_importPanel.rowWeights = new double[]{0.0, 0.0, 0.0};
		importPanel.setLayout(gbl_importPanel);
		
		inputDbButton = new JButton("Choose Database");
		inputDbButton.addActionListener(new inputDbButtonAL());
		inputDbButton.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 15));
		GridBagConstraints gbc_inputDbButton = new GridBagConstraints();
		gbc_inputDbButton.fill = GridBagConstraints.BOTH;
		gbc_inputDbButton.insets = new Insets(5, 5, 5, 5);
		gbc_inputDbButton.gridx = 0;
		gbc_inputDbButton.gridy = 0;
		importPanel.add(inputDbButton, gbc_inputDbButton);
		
		inputDbField = new JTextField();
		inputDbField.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 15));
		GridBagConstraints gbc_inputDbField = new GridBagConstraints();
		gbc_inputDbField.insets = new Insets(5, 5, 5, 5);
		gbc_inputDbField.fill = GridBagConstraints.BOTH;
		gbc_inputDbField.gridx = 1;
		gbc_inputDbField.gridy = 0;
		importPanel.add(inputDbField, gbc_inputDbField);
		
		startDbButton = new JButton("Start");
		startDbButton.addActionListener(new startDbButtonAL());
		startDbButton.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 15));
		GridBagConstraints gbc_startDbButton = new GridBagConstraints();
		gbc_startDbButton.fill = GridBagConstraints.BOTH;
		gbc_startDbButton.insets = new Insets(5, 5, 5, 5);
		gbc_startDbButton.gridx = 2;
		gbc_startDbButton.gridy = 0;
		importPanel.add(startDbButton, gbc_startDbButton);
		
		inputExcelButton = new JButton("Choose Excel");
		inputExcelButton.addActionListener(new inputExcelButtonAL());
		inputExcelButton.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 15));
		GridBagConstraints gbc_inputExcelButton = new GridBagConstraints();
		gbc_inputExcelButton.fill = GridBagConstraints.BOTH;
		gbc_inputExcelButton.insets = new Insets(5, 5, 5, 5);
		gbc_inputExcelButton.gridx = 0;
		gbc_inputExcelButton.gridy = 1;
		importPanel.add(inputExcelButton, gbc_inputExcelButton);
		
		inputExcelField = new JTextField();
		inputExcelField.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 15));
		GridBagConstraints gbc_inputExcelField = new GridBagConstraints();
		gbc_inputExcelField.insets = new Insets(5, 5, 5, 5);
		gbc_inputExcelField.fill = GridBagConstraints.BOTH;
		gbc_inputExcelField.gridx = 1;
		gbc_inputExcelField.gridy = 1;
		importPanel.add(inputExcelField, gbc_inputExcelField);
		
		importDbButton = new JButton("Import");
		importDbButton.addActionListener(new importDbButtonAL());
		importDbButton.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 15));
		GridBagConstraints gbc_importDbButton = new GridBagConstraints();
		gbc_importDbButton.fill = GridBagConstraints.BOTH;
		gbc_importDbButton.insets = new Insets(5, 5, 5, 5);
		gbc_importDbButton.gridx = 2 ;
		gbc_importDbButton.gridy = 1;
		importPanel.add(importDbButton, gbc_importDbButton);
		
		cqlPanel = new JPanel();
		cqlPanel.setBorder(new TitledBorder(null, "Database Query", TitledBorder.CENTER, TitledBorder.TOP, new Font("Microsoft YaHei UI", Font.PLAIN, 18), null));
		GridBagConstraints gbc_cqlPanel = new GridBagConstraints();
		gbc_cqlPanel.ipady = 5;
		gbc_cqlPanel.ipadx = 5;
		gbc_cqlPanel.insets = new Insets(5, 5, 0, 5);
		gbc_cqlPanel.fill = GridBagConstraints.BOTH;
		gbc_cqlPanel.gridx = 0;
		gbc_cqlPanel.gridy = 2;
		choosePanel.add(cqlPanel, gbc_cqlPanel);
		GridBagLayout gbl_cqlPanel = new GridBagLayout();
		gbl_cqlPanel.columnWidths = new int[]{150, 0, 100};
		gbl_cqlPanel.rowHeights = new int[]{50, 150,50};
		gbl_cqlPanel.columnWeights = new double[]{0.0, 1.0,0.0};
		gbl_cqlPanel.rowWeights = new double[]{1.0,3.0,1.0};
		cqlPanel.setLayout(gbl_cqlPanel);
		
		JLabel jlabel = new JLabel("Cypher Query", JLabel.CENTER);
		jlabel.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 15));
		GridBagConstraints gbc_jlabel = new GridBagConstraints();
		gbc_jlabel.fill = GridBagConstraints.BOTH;
		gbc_jlabel.insets = new Insets(5, 5, 5, 5);
		gbc_jlabel.gridx = 0;
		gbc_jlabel.gridy = 0;
		cqlPanel.add(jlabel, gbc_jlabel);
		
		cqlField = new JTextField();
		cqlField.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 15));
		GridBagConstraints gbc_cqlField = new GridBagConstraints();
		gbc_cqlField.insets = new Insets(5, 5, 5, 5);
		gbc_cqlField.fill = GridBagConstraints.BOTH;
		gbc_cqlField.gridx = 1;
		gbc_cqlField.gridy = 0;
		cqlPanel.add(cqlField, gbc_cqlField);
		
		runCqlButton = new JButton("Run");
		runCqlButton.addActionListener(new runCqlButtonAL());
		runCqlButton.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 15));
		GridBagConstraints gbc_runCqlButton = new GridBagConstraints();
		gbc_runCqlButton.fill = GridBagConstraints.BOTH;
		gbc_runCqlButton.insets = new Insets(5, 5, 5, 5);
		gbc_runCqlButton.gridx = 2;
		gbc_runCqlButton.gridy = 0;
		cqlPanel.add(runCqlButton, gbc_runCqlButton);
		

		cqlInfoTextArea = new JTextArea();
		cqlInfoTextArea.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 15));
		cqlInfoTextArea.setWrapStyleWord(true);
		JScrollPane scrollPane1 = new JScrollPane(cqlInfoTextArea);
		scrollPane1.setViewportBorder(null);
		GridBagConstraints gbc_scrollPane1 = new GridBagConstraints();
		gbc_scrollPane1.insets = new Insets(5, 5, 5, 5);
		gbc_scrollPane1.anchor = GridBagConstraints.CENTER;
		gbc_scrollPane1.fill = GridBagConstraints.BOTH;
		gbc_scrollPane1.gridx = 0;
		gbc_scrollPane1.gridy = 1;
//		gbc_scrollPane1.gridheight = GridBagConstraints.REMAINDER;
 		gbc_scrollPane1.gridwidth = GridBagConstraints.REMAINDER;
		cqlPanel.add(scrollPane1, gbc_scrollPane1);
		
		exportExcelButton = new JButton("Choose Excel");
		exportExcelButton.addActionListener(new exportExcelButtonAL());
		exportExcelButton.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 15));
		GridBagConstraints gbc_exportExcelButton = new GridBagConstraints();
		gbc_exportExcelButton.fill = GridBagConstraints.BOTH;
		gbc_exportExcelButton.insets = new Insets(5, 5, 5, 5);
		gbc_exportExcelButton.gridx = 0;
		gbc_exportExcelButton.gridy = 2;
		cqlPanel.add(exportExcelButton, gbc_exportExcelButton);
		
		exportExcelField = new JTextField();
		exportExcelField.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 15));
		GridBagConstraints gbc_exportExcelField = new GridBagConstraints();
		gbc_exportExcelField.insets = new Insets(5, 5, 5, 5);
		gbc_exportExcelField.fill = GridBagConstraints.BOTH;
		gbc_exportExcelField.gridx = 1;
		gbc_exportExcelField.gridy = 2;
		cqlPanel.add(exportExcelField, gbc_exportExcelField);
		
		exportButton = new JButton("Export");
		exportButton.addActionListener(new exportButtonAL());
		exportButton.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 15));
		GridBagConstraints gbc_exportButton = new GridBagConstraints();
		gbc_exportButton.fill = GridBagConstraints.BOTH;
		gbc_exportButton.insets = new Insets(5, 5, 5, 5);
		gbc_exportButton.gridx = 2 ;
		gbc_exportButton.gridy = 2;
		cqlPanel.add(exportButton, gbc_exportButton);
		
		
		
		schedulePanel = new JPanel();
		schedulePanel.setBorder(new TitledBorder(null, "Schedule", TitledBorder.CENTER, TitledBorder.TOP, new Font("Microsoft YaHei UI", Font.PLAIN, 18), null));
		GridBagConstraints gbc_schedulePanel = new GridBagConstraints();
		gbc_schedulePanel.ipady = 5;
		gbc_schedulePanel.ipadx = 5;
		gbc_schedulePanel.insets = new Insets(5, 5, 0, 5);
		gbc_schedulePanel.fill = GridBagConstraints.BOTH;
		gbc_schedulePanel.gridx = 0;
		gbc_schedulePanel.gridy = 3;
		choosePanel.add(schedulePanel, gbc_schedulePanel);
		GridBagLayout gbl_schedulePanel = new GridBagLayout();
		gbl_schedulePanel.columnWidths = new int[]{150, 0,100};
		gbl_schedulePanel.rowHeights = new int[]{50, 50};
		gbl_schedulePanel.columnWeights = new double[]{0, 1.0,0};
		gbl_schedulePanel.rowWeights = new double[]{1.0, 1.0};
		schedulePanel.setLayout(gbl_schedulePanel);
		
		checkBoxPanel = new JPanel();
		mergeCheckBox = new JCheckBox("Merge Orders");
		mergeCheckBox.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 15));
		checkBoxPanel.add(mergeCheckBox);
		stockCheckBox = new JCheckBox("Stock Constrains");
		stockCheckBox.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 15));
		checkBoxPanel.add(stockCheckBox);
		GridBagConstraints gbc_checkBoxPanel = new GridBagConstraints();
		gbc_checkBoxPanel.fill = GridBagConstraints.BOTH;
		gbc_checkBoxPanel.insets = new Insets(5, 5, 5, 5);
		gbc_checkBoxPanel.gridx = 0;
		gbc_checkBoxPanel.gridy = 0;
		gbc_checkBoxPanel.gridwidth = GridBagConstraints.REMAINDER;
		schedulePanel.add(checkBoxPanel, gbc_checkBoxPanel);
		
		outputExcelButton = new JButton("Set Output");
		outputExcelButton.addActionListener(new outputExcelButtonAL());
		outputExcelButton.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 15));
		GridBagConstraints gbc_outputExcelButton = new GridBagConstraints();
		gbc_outputExcelButton.fill = GridBagConstraints.BOTH;
		gbc_outputExcelButton.insets = new Insets(5, 5, 5, 5);
		gbc_outputExcelButton.gridx = 0;
		gbc_outputExcelButton.gridy = 1;
		schedulePanel.add(outputExcelButton, gbc_outputExcelButton);
		
		outputExcelField = new JTextField();
		outputExcelField.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 15));
		GridBagConstraints gbc_outputExcelField = new GridBagConstraints();
		gbc_outputExcelField.insets = new Insets(5, 5, 5, 5);
		gbc_outputExcelField.fill = GridBagConstraints.BOTH;
		gbc_outputExcelField.gridx = 1;
		gbc_outputExcelField.gridy = 1;
		schedulePanel.add(outputExcelField, gbc_outputExcelField);
		
		genButton = new JButton("Generate ");
		genButton.addActionListener(new genButtonAL());
		genButton.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 15));
		GridBagConstraints gbc_genButton = new GridBagConstraints();
		gbc_genButton.fill = GridBagConstraints.BOTH;
		gbc_genButton.insets = new Insets(5, 5, 5, 5);
		gbc_genButton.gridx = 2;
		gbc_genButton.gridy = 1;
		schedulePanel.add(genButton, gbc_genButton);
		

		frame.setResizable(false);
		frame.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 15));
		frame.setTitle("Neo4j Graph Database Application");
		//frame.setBounds(100, 100, 750, 750);
		frame.setSize(850, 850);
		frame.setLocationRelativeTo(null);
		//frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setVisible(true);
	}
	
	class inputOntoButtonAL implements ActionListener{
		public void actionPerformed(ActionEvent e) {
			// TODO Auto-generated method stub
			JFileChooser fileChooser=new JFileChooser();
			fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
			if(tempDir!=null){
				fileChooser.setCurrentDirectory(tempDir);
			}
			FileNameExtensionFilter filter = new FileNameExtensionFilter(
			        "owl文件(*.owl)", "owl");
			fileChooser.setFileFilter(filter);
			int result=fileChooser.showOpenDialog(frame);
			if(result==JFileChooser.APPROVE_OPTION){
				inputOntoField.setText(fileChooser.getSelectedFile().getPath());
				tempDir=fileChooser.getCurrentDirectory();
			}
		}
	}
	
	class inputDbButtonAL implements ActionListener{
		public void actionPerformed(ActionEvent e) {
			// TODO Auto-generated method stub
			JFileChooser fileChooser=new JFileChooser();
			fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			if(tempDir!=null){
				fileChooser.setCurrentDirectory(tempDir);
			}
			int result=fileChooser.showOpenDialog(frame);
			if(result==JFileChooser.APPROVE_OPTION){
				
				inputDbField.setText(fileChooser.getSelectedFile().getPath());
				tempDir=fileChooser.getCurrentDirectory();
			}
		}
	}
	
	class inputExcelButtonAL implements ActionListener{
		public void actionPerformed(ActionEvent e) {
			// TODO Auto-generated method stub
			JFileChooser fileChooser=new JFileChooser();
			fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
			if(tempDir!=null){
				fileChooser.setCurrentDirectory(tempDir);
			}
			FileNameExtensionFilter filter1 = new FileNameExtensionFilter(
			        "Excel 97-2003 工作簿(*.xls)", "xls");
			fileChooser.setFileFilter(filter1);
			FileNameExtensionFilter filter2 = new FileNameExtensionFilter(
			        "Excel 工作簿(*.xlsx)", "xlsx");
			fileChooser.setFileFilter(filter2);
			int result=fileChooser.showOpenDialog(frame);
			if(result==JFileChooser.APPROVE_OPTION){
				inputExcelField.setText(fileChooser.getSelectedFile().getPath());
				tempDir=fileChooser.getCurrentDirectory();
			}
		}
	}
	
	class loadOntoButtonAL implements ActionListener{

		@Override
		public void actionPerformed(ActionEvent e) {
			// TODO Auto-generated method stub
			File owlFile=new File(inputOntoField.getText());
			if(!owlFile.exists()){
				//JOptionPane.showMessageDialog(null, "The ontology owl file doesn't exsit!");
				JOptionPane.showMessageDialog(frame, "The ontology owl file doesn't exsit!", "Error", JOptionPane.ERROR_MESSAGE);
			}else{
				ontoComboBox.removeAllItems();
				kb=new KnowledgeBase(owlFile);
				ontoInfoTextArea.setText("Load ontology successfully!\n");
				Set<String> entities = kb.ReadEntities();
				for(String name:entities){
					ontoComboBox.addItem(name);;
				}
			}
		}	
	}
	
	class queryOntoButtonAL implements ActionListener{

		@Override
		public void actionPerformed(ActionEvent e) {
			// TODO Auto-generated method stub
			if(kb==null){
				//JOptionPane.showMessageDialog(null, "Please load the ontology!");
				JOptionPane.showMessageDialog(frame, "Please load the ontology!", "Error", JOptionPane.ERROR_MESSAGE);
			}else{
				String entity = (String)ontoComboBox.getSelectedItem();
				
								
				//ontoInfoTextArea.append(entity);
				try {
					ontoInfoTextArea.append(kb.QueryEntity(entity));
				} catch (Exception e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			}
			
		}	
	}
	
	class startDbButtonAL implements ActionListener{

		@Override
		public void actionPerformed(ActionEvent e) {
			// TODO Auto-generated method stub
			if(!inputDbField.getText().equals("")){
				db = new DatabaseUD(inputDbField.getText());
				graphDb = db.graphDb;
				JOptionPane.showMessageDialog(frame, "Neo4j database loaded successfully!", "Info", JOptionPane.INFORMATION_MESSAGE);
			}
			
			
		}	
	}
	
	class importDbButtonAL implements ActionListener{

		@Override
		public void actionPerformed(ActionEvent e) {
			// TODO Auto-generated method stub
			if(db==null){
				JOptionPane.showMessageDialog(frame, "Please start the database!", "Error", JOptionPane.ERROR_MESSAGE);
			}else if(kb==null){
				JOptionPane.showMessageDialog(frame, "Please load the ontology!", "Error", JOptionPane.ERROR_MESSAGE);
			}
			else{
				File excelFile=new File(inputExcelField.getText());
				if(!excelFile.exists()){
					JOptionPane.showMessageDialog(frame, "The excel file doesn't exsit!", "Error", JOptionPane.ERROR_MESSAGE);
				}else{
					try{
						Importer importer = new Importer(inputExcelField.getText(), db, kb);
						importer.Run();
						JOptionPane.showMessageDialog(frame, "Excel data imported!", "Info", JOptionPane.INFORMATION_MESSAGE);
					}catch (Exception e1){
						JOptionPane.showMessageDialog(frame, e1.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
					}// end catch
				}// end else
			}//end else
			
		}	
	}
	
	class runCqlButtonAL implements ActionListener{

		@Override
		public void actionPerformed(ActionEvent e) {
			// TODO Auto-generated method stub
			if(db==null){
				JOptionPane.showMessageDialog(frame, "Please start the database!", "Error", JOptionPane.ERROR_MESSAGE);
			}else{
				if(!cqlField.getText().equals("")){
					try (Transaction tx = graphDb.beginTx()) {	
						try{
							Result result = graphDb.execute(cqlField.getText());
							cqlInfoTextArea.setText(result.resultAsString());
						}catch (QueryExecutionException e2){
							cqlInfoTextArea.setText(e2.getMessage());
						}

					}
				}
			}
			
		}
		
	}
	
	
	class outputExcelButtonAL implements ActionListener{

		@Override
		public void actionPerformed(ActionEvent e) {
			// TODO Auto-generated method stub
			JFileChooser fileChooser=new JFileChooser();
			fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
			if(tempDir!=null){
				fileChooser.setCurrentDirectory(tempDir);
			}
			FileNameExtensionFilter filter2 = new FileNameExtensionFilter(
			        "Excel 工作簿(*.xlsx)", "xlsx");
			fileChooser.setFileFilter(filter2);
			int result=fileChooser.showOpenDialog(frame);
			if(result==JFileChooser.APPROVE_OPTION){
				String path = fileChooser.getSelectedFile().getPath();
				if(!path.endsWith(".xlsx")){
					path=path+".xlsx";
				}
				outputExcelField.setText(path);
				tempDir=fileChooser.getCurrentDirectory();
			}
		}
		
	}
	
	class genButtonAL implements ActionListener{

		@Override
		public void actionPerformed(ActionEvent e) {
			// TODO Auto-generated method stub
			if(db==null){
				JOptionPane.showMessageDialog(frame, "Please start the database!", "Error", JOptionPane.ERROR_MESSAGE);
			}else{
				if(!outputExcelField.getText().equals("")){
					Scheduler sch = new Scheduler(db, outputExcelField.getText());
					try {
						sch.Generate(mergeCheckBox.isSelected(), stockCheckBox.isSelected());
					} catch (IOException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
					JOptionPane.showMessageDialog(frame, "Finish!", "Info", JOptionPane.INFORMATION_MESSAGE);
				}
			}
			
		}
		
	}
	
	class exportExcelButtonAL implements ActionListener{

		@Override
		public void actionPerformed(ActionEvent e) {
			// TODO Auto-generated method stub
			JFileChooser fileChooser=new JFileChooser();
			fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
			if(tempDir!=null){
				fileChooser.setCurrentDirectory(tempDir);
			}
			FileNameExtensionFilter filter2 = new FileNameExtensionFilter(
			        "Excel 工作簿(*.xlsx)", "xlsx");
			fileChooser.setFileFilter(filter2);
			int result=fileChooser.showOpenDialog(frame);
			if(result==JFileChooser.APPROVE_OPTION){
				exportExcelField.setText(fileChooser.getSelectedFile().getPath());
				tempDir=fileChooser.getCurrentDirectory();
			}
		}
		
	}
	
	class exportButtonAL implements ActionListener{

		@Override
		public void actionPerformed(ActionEvent e) {
			// TODO Auto-generated method stub
			if(db==null){
				JOptionPane.showMessageDialog(frame, "Please start the database!", "Error", JOptionPane.ERROR_MESSAGE);
			}else if(kb==null){
				JOptionPane.showMessageDialog(frame, "Please load the ontology!", "Error", JOptionPane.ERROR_MESSAGE);
			}
			else{
				File excelFile=new File(exportExcelField.getText());
				if(!excelFile.exists()){
					JOptionPane.showMessageDialog(frame, "The excel file doesn't exsit!", "Error", JOptionPane.ERROR_MESSAGE);
				}else{
					try{
						Exporter exporter = new Exporter(exportExcelField.getText(), db, kb);
						exporter.Run();
						JOptionPane.showMessageDialog(frame, "Excel data exported!", "Info", JOptionPane.INFORMATION_MESSAGE);
					}catch (Exception e1){
						JOptionPane.showMessageDialog(frame, e1.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
					}// end catch
				}// end else
			}//end else
		}
		
	}
	
	class windowAL implements WindowListener{

		@Override
		public void windowOpened(WindowEvent e) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void windowClosing(WindowEvent e) {
			// TODO Auto-generated method stub
			System.out.println("Close");
			if(db!=null){
				db.Shutdown();
				graphDb.shutdown();
			}
		}

		@Override
		public void windowClosed(WindowEvent e) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void windowIconified(WindowEvent e) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void windowDeiconified(WindowEvent e) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void windowActivated(WindowEvent e) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void windowDeactivated(WindowEvent e) {
			// TODO Auto-generated method stub
			
		}
		
	}

}