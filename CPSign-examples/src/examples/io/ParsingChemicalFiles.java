package examples.io;

import java.io.IOException;

import com.arosbio.chem.io.in.CSVFile;
import com.arosbio.chem.io.in.ChemFile;
import com.arosbio.chem.io.in.ChemFileIterator;
import com.arosbio.chem.io.in.JSONFile;
import com.arosbio.chem.io.in.SDFile;

import examples.utils.Config;
import examples.utils.Utils;

public class ParsingChemicalFiles {

	public static void main(String[] args) throws IOException {
		Utils.getFactory();

		// Chemical files are parsed with either of these classes:
		loadSDF();
		loadCSV();
		loadJSON();
		
	}

	public static void loadSDF() throws IOException {
		// SDFile can be either in V2000 or V3000 format, the code can find out itself
		ChemFile sdf = new SDFile(Config.CLASSIFICATION_DATASET);

		System.out.println("Number of molecules in SDF file: " + countInFile(sdf));
	}

	public static void loadCSV() throws IOException {
		// CSV is not a strict format, there are many parameters that can be tweaked

		// The default CSV format is tab-delimited data, no CSVFormat needs to be passed in that case
		ChemFile tsv = new CSVFile(Config.CLASSIFICATION_CSV);
		System.out.println("Number of molecules in TSV file: " + countInFile(tsv));

		// For any other type of format, here delimited by semicolon, we need to supply the explicit format
		CSVFile csv = new CSVFile(Config.CLASSIFICATION_CSV_SEMICOLON);
		csv.setDelimiter(';');
		System.out.println("Number of molecules in CSV file: " + countInFile(csv));
	}
	
	public static void loadJSON() throws IOException {
		// The JSON format is fairly strict, look at the documentation website for more info
		// Requires a JSON-Array as top-level object and then objects containing "smiles" key at the first level
		ChemFile json = new JSONFile(Config.JSON_EXAMPLE_FORMAT);
		System.out.println("Number of molecules in JSON file: " + countInFile(json));
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
