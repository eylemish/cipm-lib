

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

public class DatasetLoader {

   
    public static double[] loadColumnFromCsv(String csvPath, int columnIndex) throws Exception {
        List<Double> dataList = new ArrayList<>();
        
        try (BufferedReader br = new BufferedReader(new FileReader(csvPath))) {
            String line;
            boolean isHeader = true;
            
            while ((line = br.readLine()) != null) {
                if (isHeader) { 
                    isHeader = false; 
                    continue; 
                }
                
                String[] parts = line.trim().split(",");
                
                if (parts.length > columnIndex) {
                    try {
                    
                        String valueStr = parts[columnIndex].trim().replace(",", ".");
                        dataList.add(Double.parseDouble(valueStr));
                    } catch (NumberFormatException e) {
                        continue; 
                    }
                }
            }
        }
        
        return dataList.stream().mapToDouble(Double::doubleValue).toArray();
    }
}