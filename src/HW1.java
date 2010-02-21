import java.io.*;
import java.util.*;
import java.math.*;
/**
 * Simple class used to store information about 
 * each attribute in an easily accessible manner.
 */
final class Attr
{
    String name; //name of this attribute
    int count; //number of distinct attribute types
    String types; //string of chars representing possible types, char@index n = attribute type that is counted in typecnt[n]
    int[] typecnt; //counts amount of each type of attr seen in examples, indexed by types String
    int[] classcnt; //counts amount of each type of attr that is in class0, indexed by types String
    double gain; //stores gain for this attribute
}
public class HW1 {
	private static int numExs; //stores number of examples in data
	//private static int attrCnt = 0; //stores number of attributes used
	private static ArrayList<Attr> attrs; //stores attr classes for each attribute, ordered index.
	private static char class0; //stores name of one class
	private static char class1; //stores name of the other class
	private static int c0cnt;  //stores # of class 0 ex's counted
	private static int c1cnt; //stores # of class1 ex's counted
	/**
	 * Opens a file for reading input, returning a BufferedReader
	 * @param name The name of the file to open
	 * @return BufferedReader for given file
	 * @throws FileNotFoundException 
	 */
	private static BufferedReader getFile(String name) throws FileNotFoundException{
		if(name == null)
		{
			System.out.println("Error: Invalid filename!\n");
			System.exit(1);
		}
		File ret = new File(name);
		if (!ret.exists() || !ret.canRead())
		{
			System.out.println("Error: Cannot access input file!\n");
			System.exit(1);
		}
		BufferedReader in  = new BufferedReader(new FileReader(name));
		return in;
	}
	
	private static void setAttr(String name) throws IOException {
		String line;
		StringTokenizer s;
		BufferedReader in = getFile(name);
		//int attrCount = 0; //counts number of classifying attributes
		//initial file parsing, counts types of attributes and prepares static data structures
		while((line = in.readLine())!= null){
			s = new StringTokenizer(line, "\t\n\r\f, %#");  //<--will ignore given characters
			if(line.startsWith("//")) //ignore comments
				continue;
			//set global classes
			else if(line.startsWith("%%"))
			{
				String str = s.nextToken();
				class0 = str.charAt(0);
				str = s.nextToken();
				class1 = str.charAt(0);
			}
			//set global example attributes
			else if(line.startsWith("##"))
			{
				String attrname = s.nextToken();
				int typescount = s.countTokens();
				String types = "";
				while(s.hasMoreTokens()) {
					types = types.concat(s.nextToken()); //concatenate next attribute type to this string
				}
				Attr store = new Attr(); //prepare Attr data structure
				store.count = typescount;
				store.types = types;
				store.name = attrname;
				store.typecnt = new int[typescount];
				store.classcnt = new int[typescount];
				int k = attrs.size();
				attrs.add(store);   //store this data structure in the ArrayList
			}
			else //don't parse actual examples, exit loop if one found
				break;
		}
		//attrCnt = attrCount; //set global var
		in.close(); //close reader
	}
	
	private static void parseExs(String name, ArrayList<Attr> l) throws IOException {
		String line;
		StringTokenizer s;
		String attr;
		BufferedReader in = getFile(name);
		//start parsing examples from file
		while((line = in.readLine())!= null){
			s = new StringTokenizer(line, "\t\n\r\f, %#");  //<--will ignore given characters
			if(line.startsWith("//")) //ignore all lines that aren't examples
				continue;
			else if(line.startsWith("%%"))
				continue;
			else if(line.startsWith("##"))
				continue;
			//if here, line is an example, use it!
			numExs++;
			int attCnt = 0; //counts # of attributes read so far
			int j;
			char c;
			boolean inc_classcnt = false; //used to tell code if list.classcnt[i] should be incremented
			String ex = s.nextToken(); //example name, only use if error occurs
			for(int i = 0; i <= l.size(); i++) //read in class+l one by one, update datastructs
			{
				j = i - 1;
				attr = s.nextToken();
				
				if(attr == null || attr.length() != 1)
					errout(1, ex);
				if(i == 0)  //attr is class data
				{
					c = attr.charAt(0);
					if(c == class0)
					{
						inc_classcnt = true;
						c0cnt++;
					}
					else if (c == class1)
						c1cnt++;
					else
						errout(3,ex);
				}
				else //attr is an attribute
				{
					Attr list = l.get(j); //get struct containing info about this attr
					c = attr.charAt(0);
					boolean done = false;
					for(int n = 0; n < list.count; n++)
					{
						if (done)
							break;
						else if(c == list.types.charAt(n)) //find the index for this type
						{
							list.typecnt[n]++; //update count of that type
							if(inc_classcnt)
								list.classcnt[n]++;
							done = true;
						}
					}
					if(!done)
						errout(1,ex);
					else //done parsing attribute, store Attr structure back in ArrayList
						l.set(j, list);
				}
			}//end for
		} //end while
	}
	
	/**
	 * Finds gain for given attribute index and Array. Updates array with gain.
	 * If index < 0, finds class entropy
	 * @param index given attribute, as indexed in ArrayList
	 * @param l the Arraylist with data on attribute
	 * @return double containing gain or class entropy
	 */
	private static double calcGain(int index, ArrayList<Attr> l) {
		double class_e;
		double p;
		try {
			p = (double)c0cnt/(double)numExs; //Probability of ex in class0
		}
		catch (ArithmeticException e) //for divide by 0 errors
		{
			p = 0;
		}
		double q = 1 - p; //Probability of ex in class1
		class_e = -(p*Math.log(p)/Math.log(2) + q*Math.log(q)/Math.log(2));
		if (index < 0) //entropy for class
			return class_e;
		else //conditional entropy (class|attr) w/ attr specified by index
		{
			double cond_e = 0; //stores sum of individual conditional entropies for this attribute type
			Attr a = l.get(index);
			for(int x = 0; x < a.count; x++)
			{
				double prob;
				try{
					prob = (double)a.typecnt[x]/(double)numExs; //P(an example has this attribute)
				}
				catch (ArithmeticException e)
				{
					prob = 0;
				}
				double pos;
				if(prob != 0)
					pos = (double)a.classcnt[x]/(double)a.typecnt[x]; //P(an example with this attribute is in class0)
				else 
					pos = 0;
				double neg  = 1 - pos;
				if(pos < 1 && pos > 0)	//prevent NaN from appearing
					cond_e += -1*prob*(pos*Math.log(pos)/Math.log(2) + neg*Math.log(neg)/Math.log(2));
			}
			double gain = class_e - cond_e;
			a.gain = gain;
			l.set(index, a); //update Array with new gain
			return gain;
		}
	}
	
	/**
	 * Finds attribute with best Gain
	 * @param l List of attributes
	 * @return int containing index of best attribute
	 */
	private static int findMaxGain(ArrayList<Attr> l) {
		double max = 0;
		int best = 0;
		for (int i = 0; i < l.size(); i++)
		{
			double g = calcGain(i, l);
			if (g > max)
			{
				best = i;
				max = g;
			}
		}
		return best;
	}
	
	private static void printGain(ArrayList<Attr> l) {
		for (int n = 0; n < l.size(); n++)
		{
			Attr a = l.get(n);
			String name = a.name;
			double gain = a.gain;
			System.out.println(name + " " + gain);
		}
	}
	
	private static void buildTree() {
		
	}
	
	private static void printTree() {
		
	}
	/**
	 * Method for printing any errors encountered and exiting program
	 * @param x the error code I use
	 * @param s String with extra info for that error
	 **/
	private static void errout(int x, String s) {
		switch(x)
		{
		case 0: //program called incorrectly
			System.out.println("Usage: HW1 <modeflag> <train file> <tune file> <test file>");
			break;
		case 1: //example missing an attribute
			System.out.println("Error: example " + s + " has missing or invalid attribute! Exiting...");
			System.exit(1);
			break;
		case 2: //invalid mode
			System.out.println("Error: modeFlag must be between 0 and 6!");
			System.exit(1);
			break;
		case 3: //example has invalid class
			System.out.println("Error: example " + s + " has unknown class! Exiting...");
			System.exit(1);
			break;
		case 4:
			System.out.println("Error: Modes 5 and 6 not implemented! Exiting...");
			System.exit(1);
			
		default: break;
		}
		System.exit(1);
	}
	
	/**
	 * Used for mode 4 - Displays info about myself and any bugs
	 */
	private static void dispInfo()
	{
		System.out.println("CS 540 Homework 1 - Programming Portion, 2/22/2010");
		System.out.println("Jacob David Friedman, jdfriedman3@wisc.edu, ID: 9038285020, MyUW login: jdfriedman3");
		System.out.println("Known bugs: None");
		System.out.println();
		System.out.println("Exiting program...");
		System.exit(0);
	}
	
	public static void main(String[] args) throws NumberFormatException, IOException{
		
		//check for valid command line arguments
		if (args.length != 4)
			errout(0,null);
		int mode = Integer.parseInt(args[0]);
		if(mode > 6 || mode < 0)
			errout(2,null);
		attrs = new ArrayList<Attr>();
		setAttr(args[1]); //Initialize static class data from training data
		switch(mode)
		{
		case 0: 
			parseExs(args[1], attrs);
			findMaxGain(attrs);
			printGain(attrs);
			break;
		case 1:
			buildTree();
			printTree();
			break;
		case 2:
			parseExs(args[1], attrs);
			buildTree();
			//classify(test);
			break;
		case 3:
			//findGain(args[1]);
			buildTree();
			break;
		case 4:
			dispInfo();
			break;
		default:
			errout(4, null);
			break;
		}
		System.exit(0);
	}
}
