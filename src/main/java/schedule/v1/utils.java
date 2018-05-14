package schedule.v1;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.neo4j.graphdb.*;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;

public class utils {
	

	public static Calendar ParseCalenderYMD(String str){
		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
		try{
			Date date =df.parse(str);
			Calendar calendar = Calendar.getInstance();
			calendar.setTime(date);
			return calendar;
		}catch (ParseException ex){
			System.out.println("Fail to parse the calnedar string "+str);
			return null;
		}
		
	}
	
	public static Calendar ParseCalenderYMDHM(String str){
		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm");
		try{
			Date date =df.parse(str);
			Calendar calendar = Calendar.getInstance();
			calendar.setTime(date);
			return calendar;
		}catch (ParseException ex){
			System.out.println("Fail to parse the calnedar string "+str);
			return null;
		}
		
	}
	public static String FormatCalendar(Calendar calendar){
		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm");
		return df.format(calendar.getTime());
	}
	
	public static class ValueComparator implements Comparator<Node> {
	    Map<Node, String> base;
	    public ValueComparator(Map<Node, String> base) {
	        this.base = base;
	    }
	    // Note: this comparator imposes orderings that are inconsistent with equals.
		@Override
		public int compare(Node o1, Node o2) {
			// TODO Auto-generated method stub
			if(base.get(o1).compareTo(base.get(o2))>=0){
				return 1;
			}else{
				return -1;
			}
		}
	}
	
	public static void ReadTable(String fileName, ArrayList<ArrayList<String>> sheetTitles, ArrayList<ArrayList<ArrayList<String>>> sheetData) throws Exception{
		if(fileName.endsWith("xlsx")){
			ReadTableXlsx(fileName, sheetTitles, sheetData);
		}else if(fileName.endsWith("xls")){
			ReadTableXls(fileName, sheetTitles, sheetData);
		}else{
			throw new Exception(String.format("Failed to read the tabublar file %s, unsupported format!", fileName));
		}
	
	}
	public static void ReadTableXls(String xlsFile, ArrayList<ArrayList<String>> sheetTitles, ArrayList<ArrayList<ArrayList<String>>> sheetData) throws Exception{
		try {
	        FileInputStream fileInputStream = new FileInputStream(xlsFile);
	        HSSFWorkbook workbook= new HSSFWorkbook(fileInputStream);
	        
	        for(int sheetNum=0;sheetNum<workbook.getNumberOfSheets();sheetNum++){
	        	ArrayList<String> titles = new ArrayList<String>();
	        	ArrayList<ArrayList<String>> data = new ArrayList<ArrayList<String>>();
	        	HSSFSheet sheet=workbook.getSheetAt(sheetNum);
	        	List<CellRangeAddress> cras = sheet.getMergedRegions();
	        	for(CellRangeAddress cra:cras){
	        		int fr = cra.getFirstRow();
	        		int lr = cra.getLastRow();
	        		int fc = cra.getFirstColumn();
	        		int lc = cra.getLastColumn();
	        		Cell firstCell = sheet.getRow(fr).getCell(fc);
	        		CellType cellType=firstCell.getCellTypeEnum();
	        		if(cellType==CellType.STRING){
	        			for(int i=fc;i<=lc;i++){
		        			for(int j=fr+1;j<=lr;j++){
		        				sheet.getRow(j).getCell(i).setCellValue(firstCell.getStringCellValue());;
		        			}
		        		}
        			}else if(cellType==CellType.NUMERIC){
        				for(int i=fc;i<=lc;i++){
		        			for(int j=fr+1;j<=lr;j++){
		        				sheet.getRow(j).getCell(i).setCellValue(firstCell.getNumericCellValue());;
		        			}
		        		}
        			}else if(cellType==CellType.BOOLEAN){
        				for(int i=fc;i<=lc;i++){
		        			for(int j=fr+1;j<=lr;j++){
		        				sheet.getRow(j).getCell(i).setCellValue(firstCell.getBooleanCellValue());;
		        			}
		        		}
        			}else{
        				throw new Exception("Wrong cell type");
        			}
	        		
	        	}
	        	
	        	HSSFRow titleRow=sheet.getRow(0);
	        	for(Cell cell:titleRow){
	        		titles.add(cell.getStringCellValue());
	        	}
	        	System.out.println(titles);
	        	for(int colIdx=0; colIdx<titles.size();colIdx++){
	        		ArrayList<String> colData=new ArrayList<String>();
	        		for(int rowIdx=1;rowIdx<=sheet.getLastRowNum();rowIdx++){
	        			Cell cell= sheet.getRow(rowIdx).getCell(colIdx);
	        			if(cell==null){
	        				colData.add(null);
	        				continue;
	        			}
	        			CellType cellType=cell.getCellTypeEnum();
	        			if(cellType==CellType.STRING){
	        				colData.add(cell.getStringCellValue());
	        			}else if(cellType==CellType.NUMERIC){
	        				colData.add(String.valueOf(cell.getNumericCellValue()));
	        			}else if(cellType==CellType.BOOLEAN){
	        				colData.add(String.valueOf(cell.getBooleanCellValue()));
	        			}else{
	        				colData.add(null);
	        				//colData.add(colData.get(colData.size()-1));
	        			}
	        		}
	        		data.add(colData);
	        	}
	        	
	        	sheetTitles.add(titles);
	        	sheetData.add(data);
	        }
	        fileInputStream.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
	public static void ReadTableXlsx(String xlsxFile, ArrayList<ArrayList<String>> sheetTitles, ArrayList<ArrayList<ArrayList<String>>> sheetData) throws Exception{
		try {
	        FileInputStream fileInputStream = new FileInputStream(xlsxFile);
	        XSSFWorkbook workbook= new XSSFWorkbook(fileInputStream);
	        
	        for(int sheetNum=0;sheetNum<workbook.getNumberOfSheets();sheetNum++){
	        	ArrayList<String> titles = new ArrayList<String>();
	        	ArrayList<ArrayList<String>> data = new ArrayList<ArrayList<String>>();
	        	XSSFSheet sheet=workbook.getSheetAt(sheetNum);
	        	List<CellRangeAddress> cras = sheet.getMergedRegions();
	        	for(CellRangeAddress cra:cras){
	        		int fr = cra.getFirstRow();
	        		int lr = cra.getLastRow();
	        		int fc = cra.getFirstColumn();
	        		int lc = cra.getLastColumn();
	        		Cell firstCell = sheet.getRow(fr).getCell(fc);
	        		CellType cellType=firstCell.getCellTypeEnum();
	        		if(cellType==CellType.STRING){
	        			for(int i=fc;i<=lc;i++){
		        			for(int j=fr+1;j<=lr;j++){
		        				sheet.getRow(j).getCell(i).setCellValue(firstCell.getStringCellValue());;
		        			}
		        		}
        			}else if(cellType==CellType.NUMERIC){
        				for(int i=fc;i<=lc;i++){
		        			for(int j=fr+1;j<=lr;j++){
		        				sheet.getRow(j).getCell(i).setCellValue(firstCell.getNumericCellValue());;
		        			}
		        		}
        			}else if(cellType==CellType.BOOLEAN){
        				for(int i=fc;i<=lc;i++){
		        			for(int j=fr+1;j<=lr;j++){
		        				sheet.getRow(j).getCell(i).setCellValue(firstCell.getBooleanCellValue());;
		        			}
		        		}
        			}else{
        				throw new Exception("Wrong cell type");
        			}
	        		
	        	}
	        	
	        	XSSFRow titleRow=sheet.getRow(0);
	        	for(Cell cell:titleRow){
	        		titles.add(cell.getStringCellValue());
	        	}
	        	System.out.println(titles);
	        	for(int colIdx=0; colIdx<titles.size();colIdx++){
	        		ArrayList<String> colData=new ArrayList<String>();
	        		for(int rowIdx=1;rowIdx<=sheet.getLastRowNum();rowIdx++){
	        			Cell cell= sheet.getRow(rowIdx).getCell(colIdx);
	        			if(cell==null){
	        				colData.add(null);
	        				continue;
	        			}
	        			CellType cellType=cell.getCellTypeEnum();
	        			if(cellType==CellType.STRING){
	        				colData.add(cell.getStringCellValue());
	        			}else if(cellType==CellType.NUMERIC){
	        				colData.add(String.valueOf(cell.getNumericCellValue()));
	        			}else if(cellType==CellType.BOOLEAN){
	        				colData.add(String.valueOf(cell.getBooleanCellValue()));
	        			}else{
	        				colData.add(null);
	        				//colData.add(colData.get(colData.size()-1));
	        			}
	        		}
	        		data.add(colData);
	        	}
	        	
	        	sheetTitles.add(titles);
	        	sheetData.add(data);
	        }
	        fileInputStream.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
}

