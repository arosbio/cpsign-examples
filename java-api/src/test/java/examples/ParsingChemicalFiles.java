package examples;

import java.io.FileReader;
import java.io.IOException;
import java.net.URI;

import org.apache.commons.csv.CSVFormat;
import org.junit.Test;

import com.arosbio.chem.io.in.CSVChemFileReader;
import com.arosbio.chem.io.in.CSVFile;
import com.arosbio.chem.io.in.ChemFile;
import com.arosbio.chem.io.in.ChemFileIterator;
import com.arosbio.chem.io.in.SDFile;
import com.arosbio.commons.CollectionUtils;

import utils.Config;

/*
 * Parsing of chemical data can be done using any custom code or CDK based readers. 
 * CPSign also includes some utility code for reading molecular data from SDF and CSV 
 * file formats. The main reason for this is handling gzipped data transparently and 
 * managing counters for failed molecules etc. The CSV parsing is fairly flexible, 
 * based on the commons-csv project (https://commons.apache.org/proper/commons-csv/), 
 * allowing it to read several flavors of CSV
 */
public class ParsingChemicalFiles {

	@Test
	public void loadSDF() throws IOException {
		// SDFile can be either in V2000 or V3000 format, the code can find out itself
		URI dataset = Config.getURI("classification.dataset", null);
		ChemFile sdf = new SDFile(dataset);

		System.out.println("Number of molecules in SDF file: " + countInFile(sdf));
	}

	@Test
	public void loadCSV() throws IOException {
		// CSV is not a strict format, there are many parameters that can be tweaked.
		// The only requirement is that there is one column that contains a SMILES 
		// that can be converted to an IAtomContainer

		// The CSV file
		URI dataset = Config.getURI("csv.dataset", null);
		
		// There are two available classes that can be used;
		// The CSVFile - which uses tabs as default delimiter and with a limited set 
		// of parameters that can be configured
		CSVFile csv = new CSVFile(dataset);
		csv.setDelimiter(';');
		System.out.println("Number of molecules in CSV file: " + countInFile(csv));
		
		// Or the CSVChemFileReader class with fully customizable parameters
		try(FileReader reader = new FileReader(dataset.getPath());){
			CSVChemFileReader molIterator = new CSVChemFileReader(
					CSVFormat.DEFAULT.builder().setDelimiter(';').setSkipHeaderRecord(true).build(), 
					reader);
			
			int count = CollectionUtils.count(molIterator);
			System.out.println("Number of molecules in CSV file (using CSVChemFileReader): " + count);
		}
		
	}
	
	private static int countInFile(ChemFile file) throws IOException {
		int num = 0;
		
		// The chem file iterator implemented AutoClosable - to close all underlying resources once done
		try (
				ChemFileIterator it = file.getIterator();
				) {
			while (it.hasNext()) {
				it.next();
				num++;
			}
		}
		return num;
	}

}
