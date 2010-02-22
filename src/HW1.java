/**
 * Jacob Friedman
 * jdfriedman3
 * ID: 9038285020
 * CS 540 HW1
 */

import java.io.*;
import java.util.*;
/**
 * Simple class used to store information about each attribute in an easily accessible manner.
 */
final class Attr
{
    String name; //name of this attribute
    int count; //number of distinct attribute types
    String types; //string of chars representing possible types, char@index n = attribute type that is counted in typecnt[n]
    int[] typecnt; //counts amount of each type of attr seen in examples, indexed by types String
    int[] classcnt; //counts amount of each type of attr that is in class0, indexed by types String
    int index; //where attribute is in file line (how many nextToken() from example name needed from start of line)
    double gain; //stores gain for this attribute
}

/**
 * Class used as tree nodes and leaves
 */
final class Tree 
{
	int branches; //number of branches from this node
	String name; //name of node
	ArrayList<Tree> trees; //output for each branch
	boolean isTree; //true if not leaf node
	char[] types; //types of branches for this node
	char answer; //answer for leaf nodes
	boolean isRoot; //true if root
	char[] result; //used for learning curve, stores plurality for types
	int level;  //depth of node
}
public class HW1 {
	private static ArrayList<Attr> attribs; //initial attribute list
	private static int numExs; //stores number of examples in data for last parseEx
	private static char class0; //stores name of one class
	private static char class1; //stores name of the other class
	private static int c0cnt;  //stores # of class 0 ex's counted for last parseEx
	private static int c1cnt; //stores # of class1 ex's counted for last parseEx
	private static int node_cnt = 0; //number of nodes in tree
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
	
	/**
	 * Called by main() sets up initial attribute list
	 * @param name name of training file
	 * @param attrs attribute list to configure
	 * @throws IOException
	 */
	private static void setAttr(String name, ArrayList<Attr> attrs) throws IOException {
		String line;
		StringTokenizer s;
		boolean initial = false;
		if(attribs == null)
		{
			attribs = new ArrayList<Attr>();
			initial = true;
		}
		int token = 0;
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
				token++;
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
				store.index = token;
				attrs.add(store);   //store this data structure in the ArrayList
				if(initial)
					attribs.add(store);
			}
			else //don't parse actual examples, exit loop if one found
				break;
		}
		in.close(); //close reader
	}
	
	/**
	 * Converts file to an array of examples (Strings)
	 * @param name name of file to use
	 * @return ArrayList of examples found in file
	 * @throws IOException
	 */
	private static ArrayList<String> fileToList(String name) throws IOException{
		BufferedReader in = getFile(name);
		String line;
		ArrayList<String> l = new  ArrayList<String>();
		while((line = in.readLine())!= null){
			if(line.startsWith("//")) //ignore all lines that aren't examples
				continue;
			else if(line.startsWith("%%"))
				continue;
			else if(line.startsWith("##"))
				continue;
			//add to array list
			l.add(line);
		}
		in.close();
		return l;
	}
	
	/**
	 * Calculates counts of examples (class 0 or 1), updates Attribute array
	 * @param exs examples to use
	 * @param l Attribute array to use/update
	 * @throws IOException
	 */
	private static void parseExs(ArrayList<String> exs, ArrayList<Attr> l) throws IOException {
		String line;
		StringTokenizer s;
		String attr;
		ListIterator<String> in = exs.listIterator();
		c0cnt = 0;
		c1cnt = 0;
		numExs = 0;
		for(int i = 0; i < l.size(); i++)
		{
			Attr a = l.get(i);
			for (int k = 0; k < a.count; k++)
			{
				a.typecnt[k] = 0;
				a.classcnt[k] = 0;
			}
		}
		ListIterator<Attr> iter;
		//start parsing examples from file
		while(in.hasNext()){
			line = in.next();
			s = new StringTokenizer(line, "\t\n\r\f, %#");  //<--will ignore given characters
			numExs++;
			char c;
			boolean inc_classcnt = false; //used to tell code if list.classcnt[i] should be incremented
			String ex = s.nextToken(); //example name, only use if error occurs
			iter = l.listIterator();
			int loop = s.countTokens();
			for(int i = 0; i < loop; i++) //read in class+attrs one by one, update datastructs
			{
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
				else if (l.size() != 0)//attr is an attribute
				{
					Attr list = iter.next(); //get struct containing info about this attr
					if(list.index == i)
					{
						c = attr.charAt(0);
						boolean done = false;
						for(int n = 0; n < list.count; n++)
						{
							if(c == list.types.charAt(n)) //find the index for this type
							{
								list.typecnt[n]++; //update count of that type
								if(inc_classcnt)
									list.classcnt[n]++;
								done = true;
								break;
							}
						}
						if(!done)
							errout(1,ex);
						else //done parsing attribute, store Attr structure back in ArrayList
							iter.set(list);
					}
					else
						iter.previous(); //move iterator back and try again
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
	
	/**
	 * Prints out the gain using given Attr ArrayList
	 * @param l ArrayList of Attrs
	 */
	private static void printGain(ArrayList<Attr> l) {
		for (int n = 0; n < l.size(); n++)
		{
			Attr a = l.get(n);
			String name = a.name;
			double gain = a.gain;
			System.out.println(name + " " + gain);
		}
	}
	
	/**
	 * Returns new example list of examples matching specific attribute type
	 * @param exs Initial example list
	 * @param root index(for example list) of attribute to look for
	 * @param c attribute's type to look for
	 * @return
	 */
	private static ArrayList<String> newExs(ArrayList<String> exs,int root,char c) {
		//numExs = 0;
		ListIterator<String> in = exs.listIterator();
		ArrayList<String> ret = new ArrayList<String>();
		StringTokenizer s;
		String line;
		while(in.hasNext()){
			line = in.next();
			s = new StringTokenizer(line, "\t\n\r\f, %#");  //<--will ignore given characters
			s.nextToken(); //skip name
			//s.nextToken(); //skip class
			for (int k = 0; k < root; k++) //iterate to correct token
				s.nextToken();
			String str = s.nextToken();
			if(str.charAt(0) == c)
				ret.add(line);
		}
		return ret;
	}
	
	/**
	 * Calculates plurality of examples
	 * @param exs examples to use
	 * @return
	 */
	private static char plurality(ArrayList<String> exs) {
		ListIterator<String> in;
		if (exs != null)
			in = exs.listIterator();
		else
			return class0;
		StringTokenizer s;
		String line;
		int c0count = 0;
		int c1count = 0;
		while(in.hasNext()){
			line = in.next();
			s = new StringTokenizer(line, "\t\n\r\f, %#");  //<--will ignore given characters
			s.nextToken(); //skip name
			String clas = s.nextToken();
			if(clas.charAt(0) == class0)
				c0count++;
			else
				c1count++;
		}
		if(c0count >= c1count)
			return class0;
		else
			return class1;
	}
	
	/**
	 * Method to recursively build Decision tree
	 * @param attr attributes for this node
	 * @param exs examples for this node
	 * @param parent examples for parent node
	 * @param level level node is on, initial call with 1
	 * @return Tree node/leaf
	 * @throws IOException
	 */
	@SuppressWarnings("unchecked")
	private static Tree buildTree(ArrayList<Attr> attr, ArrayList<String> exs, ArrayList<String> parent, int level) throws IOException {
		Tree t = new Tree();
		ListIterator<String> in = exs.listIterator();
		t.isRoot = false;
		if(!in.hasNext())
		{
			t.branches = 0;
			t.answer = plurality(parent);
			t.isTree = false;
			if(parent == null)
				t.isRoot = true;
			return t;
		}
		else
		{
			parseExs(exs, attr);
			if(c0cnt == 0)
			{
				t.branches = 0;
				t.answer = class1;
				t.isTree = false;
				if(parent == null)
					t.isRoot = true;
				return t;
			}
			else if (c1cnt == 0)
			{
				t.branches = 0;
				t.answer = class0;
				t.isTree = false;
				if(parent == null)
					t.isRoot = true;
				return t;
			}
			else if (attr.size() == 0)
			{
				t.branches = 0;
				t.answer = plurality(exs);
				t.isTree = false;
				if(parent == null)
					t.isRoot = true;
				return t;
			}
			else 
			{
				int root = findMaxGain(attr);
				Attr use = attr.get(root);
				t.branches = use.count;
				t.name = use.name;
				ArrayList<Attr> nextlist = (ArrayList<Attr>) attr.clone();
				nextlist.remove(root);
				if(parent == null)
					t.isRoot = true;
				t.isTree = true;
				t.level = level;
				t.trees = new ArrayList<Tree>();
				t.types = new char[t.branches];
				t.result = new char [t.branches];
				node_cnt++;
				for(int n = 0; n < t.branches; n++)
				{
					t.types[n] = use.types.charAt(n);
					ArrayList<String> next = newExs(exs, use.index, use.types.charAt(n));
					t.result[n] = plurality(next);
					Tree next_tr = buildTree(nextlist,next,exs,level + 1);
					t.trees.add(next_tr);
				}
				return t;
			}
		}
	}
	
	/**
	 * Used to recursively print out tree (mode2)
	 * @param root node to print
	 * @param indent how much to indent printed info
	 */
	private static void printTree(Tree root, int indent) {
		if(!root.isTree)
		{
			System.out.println("(" + root.answer +")");
		}
		
		else 
		{
			if(root.isRoot)
				System.out.print("Root ");
			System.out.println(root.name + "?");
			indent++;
			for(int i = 0; i < root.branches; i++)
			{
				for(int k = 0; k < indent; k++)
					System.out.print("\t");
				System.out.print(root.types[i] + " ");
				printTree(root.trees.get(i), indent);
			}
		}
			
	}
	
	/**
	 * Method to classify all test examples and print results (mode2)
	 * @param file name of test file
	 * @param root root of tree
	 * @param attribs tree's attributes array
	 * @throws IOException
	 */
	private static void classify(String file, Tree root, ArrayList<Attr> attribs) throws IOException
	{
		ArrayList<String> exs = fileToList(file);
		String line;
		for(int k = 0; k < exs.size(); k++)
		{
			line = exs.get(k);
			char answer[] = classify_ex(line, root, attribs);
			System.out.println(answer[0]);
		}
	}
	
	/**
	 * Recursive Method to return classification of an example
	 * @param line example, unparsed
	 * @param root root of tree
	 * @param attribs attributes for tree
	 * @return char[] contains both actual class and guess
	 * @throws IOException
	 */
	private static char[] classify_ex(String line, Tree root, ArrayList<Attr> attribs) throws IOException
	{
	
		StringTokenizer s;
		String test = root.name;
		s = new StringTokenizer(line, "\t\n\r\f, %#");  //<--will ignore given characters
		String ex = s.nextToken(); //ignore ex name
		char actual = s.nextToken().charAt(0); //class data
		if(root.isTree == false)
		{
			char[] ret = new char[2];
			ret[0] = root.answer;
			ret[1] = actual;
			return ret;
		}
		int loop = s.countTokens();
		String []attribute = new String[loop];
		Attr atts;
		char use = ' ';
		//ListIterator<Attr> l = attribs.listIterator();
		for(int i = 0; i < loop; i++) //read in attrs
		{
			attribute[i] = s.nextToken(); //store all attributes
		}
		for(int i = 0; i < attribs.size(); i++)
		{
			atts = attribs.get(i);
			if(atts.name.equals(test)) //attribute we are looking at
			{
				use = attribute[i].charAt(0);
			}
		}
		for(int n = 0; n < root.branches; n++)
		{
			if(root.types[n] == use)
				return classify_ex(line, root.trees.get(n), attribs);
		}
		errout(5,ex); //if here, error occurred
		return new char[2];
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
			break;
		case 5:
			System.out.println("Error: classifying error on example " + s + ". Exiting...");
			System.exit(1);
			break;
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
		System.out.println("Known bugs: I do not think mode 3 does not work properly, Could not figure out good way to calculate accuracy with my Tree class structure, I think my method claims nodes not directly out connected to root have same accuracy ");
		System.out.println();
		System.out.println("Exiting program...");
		System.exit(0);
	}
	
		
	/**
	 * Recursive method to calculate learning curve, !!!!!!has bugs!!!!!!
	 * @param t Tree to look at (call with root)
	 * @param attrs Attributes in tree
	 * @param test Test data
	 * @param train Training data
	 * @param node used for recursion (call with 1)
	 * @return int -1 on error, 0 otherwise
	 * @throws IOException
	 */
	private static int curve(Tree t, ArrayList<Attr> attrs, ArrayList<String> test, ArrayList<String> train, int node) throws IOException
	{
		if(!t.isTree) //if its a leaf, ignore it
			return -1;
		double trn;
		double tst;
		double trn_cor = 0;
		double tst_cor = 0;
		String line;
		int remove = 0;
		ListIterator<String> it = test.listIterator();
		while(it.hasNext())  //test examples
		{			
			int use = -1;
			String name = t.name;
			for(int i = 0; i < attrs.size(); i++)
			{
				if (attrs.get(i).name.equals(name))
					use = attrs.get(i).index;
			}
			line = it.next();
			StringTokenizer s = new StringTokenizer(line, "\t\n\r\f, %#");  //<--will ignore given characters
			s.nextToken(); //example name
			char [] res = new char[2];
			res[0] = s.nextToken().charAt(0); //class
			char att = ' ';
			for (int i = 0; i < use; i++)
				att = s.nextToken().charAt(0);
			for (int i = 0; i < t.branches; i++)
			{
				if(att == t.types[i])
					res[1] = t.result[i];
			}
			if(res[0] == res[1])
				tst_cor++;
		}
		it = train.listIterator();
		while(it.hasNext()) //training examples
		{
			int use = -1;
			
			String name = t.name;
			for(int i = 0; i < attrs.size(); i++)
			{
				if (attrs.get(i).name.equals(name))
				{
					use = attrs.get(i).index;
					remove = i;
				}
			}
			line = it.next();
			StringTokenizer s = new StringTokenizer(line, "\t\n\r\f, %#");  //<--will ignore given characters
			s.nextToken(); //example name
			char [] res = new char[2];
			res[0] = s.nextToken().charAt(0); //class
			char att = ' ';
			for (int i = 0; i < use; i++)
				att = s.nextToken().charAt(0);
			for (int i = 0; i < t.branches; i++)
			{
				if(att == t.types[i])
					res[1] = t.result[i];
			}
			if(res[0] == res[1])
				trn_cor++;
		}
		try{
			attrs.remove(remove);
		}
		catch(IndexOutOfBoundsException e) {
			return 0;
		}
		tst = tst_cor/(double)test.size();
		trn = trn_cor/(double)train.size();
		System.out.println(node + " " + trn + " " + tst);
		int br = t.branches;
		for(int k = 0; k < br; k++) //check branches of this tree as well
		{
			node++;
			if(curve(t.trees.get(k), attrs,test,train, node) == -1)
				node--;
			
		}
		return 0;
	}
/**
 * Main method, parses initial arguments from command line and executes desired code
 * @param args
 * @throws NumberFormatException
 * @throws IOException
 */
	public static void main(String[] args) throws NumberFormatException, IOException{
		
		//check for valid command line arguments
		if (args.length != 4)
			errout(0,null);
		int mode = Integer.parseInt(args[0]);
		if(mode > 6 || mode < 0)
			errout(2,null);
		ArrayList<Attr> attrs = new ArrayList<Attr>();  //initial attribute list
		ArrayList<String> exs;
		setAttr(args[1], attrs); //Initialize initial class and attribute data from training data
		switch(mode)
		{
		case 0: 
			exs = fileToList(args[1]);
			parseExs(exs, attrs);
			findMaxGain(attrs);
			printGain(attrs);
			break;
		case 1:
			exs = fileToList(args[1]);
			Tree root1 = buildTree(attrs,exs,null,1);
			printTree(root1, 0);
			break;
		case 2:
			exs = fileToList(args[1]);
			Tree root2 = buildTree(attrs,exs,null,1);
			classify(args[3], root2, attribs);
			break;
		case 3:
			exs = fileToList(args[1]);
			ArrayList<String> test = fileToList(args[3]);
			ArrayList<String> init = new ArrayList<String>();
			for(int n = 0 ; n < exs.size(); n++)
			{
				init.add(exs.get(n));
			}
			Tree root3 = buildTree(attrs,exs,null,1);
			curve(root3, attribs,test, init, 1);
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
