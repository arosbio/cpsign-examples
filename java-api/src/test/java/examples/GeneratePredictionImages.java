package examples;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.font.TextAttribute;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.text.AttributedString;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.imageio.ImageIO;

import org.apache.commons.io.FileUtils;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.smiles.SmilesParser;

import com.arosbio.chem.io.in.SDFile;
import com.arosbio.chem.io.out.image.AtomContributionRenderer;
import com.arosbio.chem.io.out.image.RendererTemplate.MolRendering;
import com.arosbio.chem.io.out.image.RendererTemplate.RenderInfo;
import com.arosbio.chem.io.out.image.SignificantSignatureRenderer;
import com.arosbio.chem.io.out.image.fields.ColorGradientField;
import com.arosbio.chem.io.out.image.fields.HighlightExplanationField;
import com.arosbio.chem.io.out.image.fields.TextField;
import com.arosbio.chem.io.out.image.layout.CustomLayout;
import com.arosbio.chem.io.out.image.layout.CustomLayout.Boarder;
import com.arosbio.chem.io.out.image.layout.CustomLayout.Boarder.BoarderShape;
import com.arosbio.chem.io.out.image.layout.CustomLayout.Margin;
import com.arosbio.chem.io.out.image.layout.Position.Vertical;
import com.arosbio.cheminf.ChemCPClassifier;
import com.arosbio.cheminf.SignificantSignature;
import com.arosbio.color.gradient.ColorGradient;
import com.arosbio.color.gradient.GradientFactory;
import com.arosbio.data.NamedLabels;
import com.arosbio.depict.MoleculeDepictor;
import com.arosbio.ml.algorithms.svm.LinearSVC;
import com.arosbio.ml.cp.acp.ACPClassifier;
import com.arosbio.ml.cp.nonconf.classification.NegativeDistanceToHyperplaneNCM;
import com.arosbio.ml.sampling.RandomSampling;

import utils.Config;

public class GeneratePredictionImages {

	static IAtomContainer testMolecule;
	static File imageDir = null;
	static String label;
	static SignificantSignature ss;

	@BeforeClass
	public static void generateDepictionInfo() throws IOException, IllegalStateException, NullPointerException, CDKException {
		// Set up a directory to write depictions to
		try {
			imageDir = Config.getFile("output.img.dir", null);
		} catch(Exception e) {
			imageDir = new File("");
		}
		FileUtils.forceMkdir(imageDir);

		// Load a test molecule
		testMolecule = new SmilesParser(SilentChemObjectBuilder.getInstance()).parseSmiles(Config.DEFAULT_SMILES);

		// Init the Predictor
		ACPClassifier predictor = new ACPClassifier(
				new NegativeDistanceToHyperplaneNCM(new LinearSVC()),
				new RandomSampling(
					Config.getInt("modeling.sampling.num.models", 10), // number of models to aggregate
					Config.getDouble("modeling.sampling.calib.ratio", 0.2))); // proportion used for calibration in each ICP

		// Wrap the predictor in Signatures-wrapper
		ChemCPClassifier chemPredictor = new ChemCPClassifier(predictor);

		// Load data
		chemPredictor.addRecords(new SDFile(Config.getURI("classification.dataset", null)).getIterator(), 
				Config.getProperty("classification.endpoint"), 
				new NamedLabels(Config.getProperty("classification.labels").split("[\\s,]")));

		// Train the aggregated ICPs
		chemPredictor.train();

		// When we're generating images, we need to compute ranges for the dataset 
		// (e.g. by computing the training file again on the trained models)
		chemPredictor.computePercentiles(new SDFile(Config.getURI("classification.dataset", null)).getIterator());

		// Predict the testMolecule 
		label = chemPredictor.getDataset().getTextualLabels().getLabels().values().iterator().next();
		ss = chemPredictor.predictSignificantSignature(testMolecule,label);
	}

	@Test
	public void depictAtomContributions() throws IOException {

		// Rainbow image with a "bloom" around the image
		AtomContributionRenderer.Builder builder = new AtomContributionRenderer.Builder()
			.colorScheme(GradientFactory.getRainbowGradient()) // Decide which gradient or color scheme to use
			.height(550)
			.width(499);
		String titleText = "Rainbow gradient";
		AttributedString title = new AttributedString(titleText);
		// See available attributes at: https://docs.oracle.com/javase/8/docs/api/java/awt/font/TextAttribute.html
		title.addAttribute(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON, 0, titleText.length()); // Add underline
		title.addAttribute(TextAttribute.FOREGROUND, Color.RED, 0, 7); // Add red text for "Rainbow"
		title.addAttribute(TextAttribute.POSTURE, TextAttribute.POSTURE_OBLIQUE, 0, 7);

		// Add a boarder around the molecule
		Boarder molBoarder = new Boarder.Builder().shape(BoarderShape.ROUNDED_RECTANGLE).stroke(new BasicStroke(3f)).color(Color.BLUE).build();
		builder.molLayout(new CustomLayout.Builder().boarder(molBoarder).margin(new Margin(5)).build());
		// Set a "title" with the color scheme that is used
		TextField titleField = new TextField.Immutable.Builder(title).alignment(Vertical.LEFT_ADJUSTED).build();

		builder.addFieldOverMol(titleField);
		builder.addFieldUnderMol(new ColorGradientField.Builder(builder.colorScheme()).build());
		MolRendering fig = builder.build().render(new RenderInfo.Builder(testMolecule, ss).build()); 
		fig.saveToFile(new File(imageDir, "FullGradientRainbow.png"));
		// or as Base64
		// System.out.printf("%n%nFigure 1:%n%s%n",fig.getBase64());

		// Change color to Blue->Red-gradient
		ColorGradient newGrad = GradientFactory.getBlueRedGradient();
		AttributedString newTitle = new AttributedString("Blue-Red Gradient");
		builder.withFieldsOverMol(Arrays.asList( new TextField.Immutable.Builder(newTitle).build()))
			.colorScheme(newGrad)
			.withFieldsUnderMol(Arrays.asList(new ColorGradientField.Builder(newGrad).build()))
			.width(550);
		MolRendering blueRedFig = builder.build().render(new RenderInfo.Builder(testMolecule, ss).build());
		blueRedFig.saveToFile(new File(imageDir,"full-gradient-plain.png"));
		// or as Base64
		// System.out.printf("%n%nFigure 2:%n%s%n",blueRedFig.getBase64());

	}

	@Test
	public void depictSignificantSignature() throws IOException {

		// Depict only the atoms of the significant signature 
		
		// Pick the highlight color
		Color highlight = Color.ORANGE;
		SignificantSignatureRenderer.Builder signBuilder = new SignificantSignatureRenderer.Builder()
			.highlight(highlight)
			.addFieldOverMol(new TextField.Immutable.Builder("Significant Signature only").build())
			.addFieldUnderMol(new HighlightExplanationField.Builder(highlight).basedOnPValueOfClass(label).build());
		MolRendering ssFig = signBuilder.build().render(new RenderInfo.Builder(testMolecule, ss).build());
		ssFig.saveToFile(new File(imageDir,"significant-signature.png"));
		// or as Base64
		// System.out.printf("%n%nSignificant signature fig:%n%s%n",ssFig.getBase64());

	}

	@Test
	public void customDepictions() throws IOException {
		////// CUSTOM DEPICTIONS

		// Create a map from all atoms part of the significant signature -> highest value (1)
		Map<Integer,Double> significantSignatureMap = new HashMap<>();		
		for (int atom : ss.getAtoms()){
			significantSignatureMap.put(atom, 1d);
		}


		// Set up a large image that contains sub-images, here they are 
		// intentionally set a bit small which gives some blurriness - larger images
		// should be used for higher resolution images
		int depictionW = 400; // px
		int depictionH = 200; // px
		int padding = 10; 
		int pageWidth = depictionW*2 + 3*padding;
		int pageHeight = depictionH*4 + 5*padding;
		BufferedImage pageImage = new BufferedImage(pageWidth, pageHeight, BufferedImage.TYPE_4BYTE_ABGR);
		Graphics2D graphics = pageImage.createGraphics();
		// Draw the background white (no background set by default by MoleculeDepictor)
		graphics.setColor(Color.WHITE);
		graphics.fillRect(0, 0, pageWidth, pageHeight);

		MoleculeDepictor [] depictors = new MoleculeDepictor[4];
		depictors[0] = new MoleculeDepictor.Builder().h(depictionH).w(depictionW).build(); // Uses the default gradient (blue->red with gray in the middle)
		depictors[1] = new MoleculeDepictor.Builder().h(depictionH).w(depictionW).color(GradientFactory.getRainbowGradient()).showAtomNumbers(true).numberColor(Color.MAGENTA).build();
		depictors[2] = new MoleculeDepictor.Builder().h(depictionH).w(depictionW).color(GradientFactory.getCyanMagenta()).build();
		// this is the textual description of the red-blue gradient
		depictors[3] = new MoleculeDepictor.Builder()
			.h(depictionH)
			.w(depictionW)
			.color(
				GradientFactory.getCustomGradient("[{\"color\":\"#FF0004\",\"pos\":-0.58},{\"color\":\"#E5E5E5\",\"pos\":0.2},{\"color\":\"#0000FF\",\"pos\":0.6},{\"color\":\"#E6E6E6\",\"pos\":-0.2}]")
			)
			.background(Color.BLACK) // see how things look with a different background
			.build();

		// Add all sub-images
		for(int i=0; i< depictors.length; i++){
			
			// Use the full atom gradient in the left column
			depictors[i].depict(testMolecule, 
				ss.getAtomContributions(),
				null,
				graphics,
				new java.awt.geom.Rectangle2D.Float(padding, (i+1)*padding + i*depictionH, depictionW, depictionH - padding));
			// Add a gradient field underneath of "padding" height
			BufferedImage gradient = drawGradient(depictionW, depictors[i].getColorGradient());
			graphics.drawImage(gradient, 
						padding, (i+1)*depictionH + i*padding, depictionW, padding,
						null);

			// Depict only the significant signature in the right column
			depictors[i].depict(testMolecule, 
				significantSignatureMap,
				pageImage,
				graphics,
				new java.awt.geom.Rectangle2D.Float(padding*2+depictionW, (i+1)*padding + i*depictionH, depictionW, depictionH));
			
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
