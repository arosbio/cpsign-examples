package examples;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.font.TextAttribute;
import java.awt.image.BufferedImage;
import java.io.File;
import java.text.AttributedString;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.imageio.ImageIO;

import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.smiles.SmilesParser;

import com.arosbio.bloom.ColorGradient;
import com.arosbio.chem.io.in.SDFile;
import com.arosbio.chem.io.out.GradientFigureBuilder;
import com.arosbio.chem.io.out.MoleculeFigure;
import com.arosbio.chem.io.out.SignificantSignatureFigureBuilder;
import com.arosbio.chem.io.out.depictors.MoleculeGradientDepictor;
import com.arosbio.chem.io.out.depictors.MoleculeSignificantSignatureDepictor;
import com.arosbio.chem.io.out.fields.ColorGradientField;
import com.arosbio.chem.io.out.fields.HighlightExplanationField;
import com.arosbio.chem.io.out.fields.OrnamentField;
import com.arosbio.chem.io.out.fields.TitleField;
import com.arosbio.depict.GradientFactory;
import com.arosbio.depict.MoleculeDepictor;
import com.arosbio.io.image.CustomLayout;
import com.arosbio.io.image.CustomLayout.Boarder;
import com.arosbio.io.image.CustomLayout.Boarder.BoarderShape;
import com.arosbio.io.image.CustomLayout.Margin;
import com.arosbio.io.image.CustomLayout.Padding;
import com.arosbio.io.image.Position.Vertical;
import com.arosbio.modeling.cheminf.NamedLabels;
import com.arosbio.modeling.cheminf.SignaturesCPClassification;
import com.arosbio.modeling.cheminf.SignaturesCPClassification.SignificantSignatureCPClassification;
import com.arosbio.modeling.cheminf.SignificantSignature;
import com.arosbio.modeling.ml.cp.acp.ACPClassification;
import com.arosbio.modeling.ml.ds_splitting.RandomSampling;

import utils.BaseTest;
import utils.Config;

public class GeneratePredictionImages extends BaseTest {

	@Test
	public void depict() throws Exception {
		
		File imageDir = null;
		try {
			imageDir = Config.getFile("output.img.dir", null);
		} catch(Exception e) {
			imageDir = new File("");
		}
		FileUtils.forceMkdir(imageDir);
		
		IAtomContainer testMolecule = new SmilesParser(SilentChemObjectBuilder.getInstance()).parseSmiles(Config.DEFAULT_SMILES);

		// Init the Predictor
		ACPClassification predictor = factory.createACPClassification(
				factory.createNegativeDistanceToHyperplaneNCM(factory.createLinearSVC()), 
				new RandomSampling(Config.getInt("modeling.sampling.num.models", 10), Config.getDouble("modeling.sampling.calib.ratio", 0.2))); 

		// Wrap the predictor in Signatures-wrapper
		SignaturesCPClassification signPredictor = factory.createSignaturesCPClassification(predictor, 1, 3);

		// Load data
		signPredictor.fromMolsIterator(new SDFile(Config.getURI("classification.dataset", null)).getIterator(), 
				Config.getProperty("classification.endpoint"), 
				new NamedLabels(Config.getProperty("classification.labels").split("[\\s,]")));

		// Train the aggregated ICPs
		signPredictor.train();

		// When we're generating images, we need to compute ranges for the dataset 
		// (e.g. by computing the training file again on the trained models)
		signPredictor.computePercentiles(new SDFile(Config.getURI("classification.dataset", null)).getIterator());



		////// Standard images


		// Predict the testMol and depict it
		SignificantSignature ss = signPredictor.predictSignificantSignature(testMolecule);

		// Rainbow image with a "bloom" around the image
		GradientFigureBuilder gradBuilder = new GradientFigureBuilder(new MoleculeGradientDepictor(GradientFactory.getRainbowGradient()));
		String titleText = "Rainbow gradient";
		AttributedString title = new AttributedString(titleText);
		// See available attributes at: https://docs.oracle.com/javase/8/docs/api/java/awt/font/TextAttribute.html
		title.addAttribute(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON, 0, titleText.length()); // Add underline
		title.addAttribute(TextAttribute.FOREGROUND, Color.RED, 0, 7); // Add red text for "Rainbow"
		title.addAttribute(TextAttribute.POSTURE, TextAttribute.POSTURE_OBLIQUE, 0, 7);

		// Add a boarder around the molecule
		Boarder molBoarder = new Boarder(BoarderShape.ROUNDED_RECTANGLE, new BasicStroke(3f), Color.BLUE);
		gradBuilder.getDepictor().addLayout(new CustomLayout(new Padding(0), molBoarder, new Margin(5)));
		TitleField tf = new TitleField(title);
		tf.setAlignment(Vertical.LEFT_ADJUSTED); // default is a centered title
		gradBuilder.addFieldOverImg(tf);
		gradBuilder.addFieldUnderImg(new ColorGradientField(gradBuilder.getDepictor()));
		gradBuilder.setFigureHeight(550);
		gradBuilder.setFigureWidth(499);
		MoleculeFigure fig = gradBuilder.build(testMolecule, ss.getMoleculeGradient());
		fig.saveToFile(new File(imageDir, "FullGradientRainbow.png"));
		// or as Base64
		System.out.println("Image 1: " + fig.getBase64());

		// Change colors Blue->Red-gradient
		gradBuilder.setFieldsOverImg(Arrays.asList((OrnamentField) new TitleField("Blue-Red Gradient")));
		gradBuilder.getDepictor().setColorGradient(GradientFactory.getBlueRedGradient());
		gradBuilder.setFieldsUnderImg(Arrays.asList((OrnamentField) new ColorGradientField(gradBuilder.getDepictor())));
		gradBuilder.setFigureWidth(550);
		MoleculeFigure blueRedFig = gradBuilder.build(testMolecule, ss.getMoleculeGradient());
		blueRedFig.saveToFile(new File(imageDir,"full-gradient-plain.png"));
		// or as Base64
		System.out.println("Image 2: " + blueRedFig.getBase64());

		// Depict only the atoms of the significant signature, add a color
		SignificantSignatureFigureBuilder signBuilder = new SignificantSignatureFigureBuilder(new MoleculeSignificantSignatureDepictor());
		signBuilder.addFieldOverImg(new TitleField("Significant Signature only"));
		signBuilder.addFieldUnderImg(new HighlightExplanationField(signBuilder.getDepictor().getHighlightColor(), 
				((SignificantSignatureCPClassification) ss).getLabelUsedForComputing()));
		MoleculeFigure ssFig = signBuilder.build(testMolecule, ss.getAtoms());
		ssFig.saveToFile(new File(imageDir,"significant-signature.png"));
		// or as Base64
		System.out.println("Image 3: " + ssFig.getBase64());

	////// CUSTOM DEPICTIONS

		// Change Map<Integer, Double> and Map<Integer, Integer> into Map<IAtom, Double>
		Map<IAtom,Double> atomGradients = new HashMap<>(), significantSignatureMap = new HashMap<>();
		for(Entry<Integer, Double> entry: ss.getMoleculeGradient().entrySet()){
			atomGradients.put(testMolecule.getAtom(entry.getKey()), entry.getValue());
		}
		for (int atom: ss.getAtoms()){
			significantSignatureMap.put(testMolecule.getAtom(atom), 1d);
		}


		// Set up a bigger image that contains sub-images
		int imageSize = 400; //px
		int padding = 10; 
		int pageWidth = imageSize*2 + 3*padding;
		int pageHeight = imageSize*4 + 5*padding;

		MoleculeDepictor [] depictors = new MoleculeDepictor[4];
		depictors[0] = MoleculeDepictor.createDepictor(); // Uses the default gradient (blue->red)
		depictors[1] = MoleculeDepictor.createDepictor(GradientFactory.getRainbowGradient());
		depictors[2] = MoleculeDepictor.createDepictor(GradientFactory.getCyanMagenta());
		// this is the textual description of the red-blue gradient
		depictors[3] = MoleculeDepictor.createDepictor(
				GradientFactory.getCustomGradient("[{\"color\":\"#FF0004\",\"pos\":-0.58},{\"color\":\"#E5E5E5\",\"pos\":0.2},{\"color\":\"#0000FF\",\"pos\":0.6},{\"color\":\"#E6E6E6\",\"pos\":-0.2}]"));
		// do further tweaks
		depictors[1].setShowAtomNumbers(true);
		depictors[1].setAtomNumberColor(Color.MAGENTA); //something that will really show

		// Draw the background white (no background set by default by MoleculeDepictor)
		BufferedImage pageImage = new BufferedImage(pageWidth, pageHeight, BufferedImage.TYPE_4BYTE_ABGR);
		Graphics2D graphics = pageImage.createGraphics();
		graphics.setColor(Color.WHITE);
		graphics.fillRect(0, 0, pageWidth, pageHeight);


		// Add all sub-images
		BufferedImage imgToAdd1, imgToAdd2;
		for(int i=0; i< depictors.length; i++){
			// Color the first column with the complete molecules gradient
			imgToAdd1 = depictors[i].depict(testMolecule, atomGradients);
			graphics.drawImage(imgToAdd1, padding, (i+1)*padding + i*imageSize, null);

			// Color second column with only the significant signature values
			depictors[i].setImageHeight(imageSize/2);
			imgToAdd2 = depictors[i].depict(testMolecule, significantSignatureMap);
			graphics.drawImage(imgToAdd2, padding*2 + imageSize, (i+1)*padding + i*imageSize, null);

			// Add a depiction with black background underneath
			BufferedImage blackBackgroundImg = new BufferedImage(imageSize, imageSize/2, BufferedImage.TYPE_4BYTE_ABGR);
			Graphics2D graphicsBack = blackBackgroundImg.createGraphics();
			graphicsBack.setColor(Color.BLACK);
			graphicsBack.fillRect(0, 0, imageSize, imageSize/2);
			graphicsBack.drawImage(imgToAdd2, 0, 0, null);
			// Add the black-background-image to the page
			graphics.drawImage(blackBackgroundImg, padding*2 + imageSize, (int)((i+1)*padding + (i+0.5)*imageSize), null);

			// Add a color-scale to each sub-plot
			BufferedImage gradient = drawGradient(imageSize, depictors[i].getColorGradient());
			graphics.drawImage(gradient, 
					padding, (i+1)*padding + (i+1)*imageSize,imageSize,padding,
					null);
			graphics.drawImage(gradient, 
					padding*2+imageSize, (i+1)*padding + (i+1)*imageSize,imageSize,padding,
					null);
		}

		File outputfile = new File(imageDir, "subplots.png");
		ImageIO.write(pageImage, "png", outputfile);

	}

	private static BufferedImage drawGradient(int width, ColorGradient gradient) {
		BufferedImage image = new BufferedImage(width, 1, BufferedImage.TYPE_INT_ARGB);
		for(int i=0;i<width;i++) {
			double val = -1d + 2*((double)i)/width;
			Color color = Color.MAGENTA;
			if(gradient!=null) 
				color = gradient.getColor(val);
			image.setRGB(i, 0, color.getRGB());
		}
		return image;
	}


}
