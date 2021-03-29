package utils;

import java.io.InputStream;
import java.net.URI;
import java.util.Properties;

public class Config {

	public static final String DEFAULT_SMILES = "CCCNCC(=O)NC1=CC(=CC=C1)S(=O)(=O)NC2=NCCCCC2";

	public static Properties getProperties() {
		try (InputStream propsStream = new Config().getClass().getClassLoader().getResourceAsStream("resources/config.properties");){
			Properties props = new Properties();
			props.load(propsStream);
			return props;
		} catch (Exception e) {
			System.err.println("Failed setting up environment correctly - cannot run example(s)\nPlease look over the config.properties to make sure everything is OK");
			System.err.println("Error message: " + e.getMessage());
			System.exit(1);
			return null;
		}
	}

	public static void main(String[] args) {
		Config.getProperties();
	}

	public static int getInt(String property, int def) {
		try {
			return Integer.getInteger(getProperties().getProperty(property));
		} catch (Exception e) {return def;}
	}

	public static double getDouble(String property, double def) {
		try {
			return Double.parseDouble(getProperties().getProperty(property));
		} catch (Exception e) {return def;}
	}

	public static URI getURI(String property, URI def) {
		try {
			return Utils.getURI(getProperties().getProperty(property));
		} catch(Exception e) {
			return def;
		}
	}

	public static String getProperty(String property) {
		return getProperties().getProperty(property);
	}

}
