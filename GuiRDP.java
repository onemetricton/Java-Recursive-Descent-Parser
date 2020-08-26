import java.awt.*;
import javax.swing.*;
import java.util.*;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.FileNotFoundException;

/**
  * Class GuiRDP
  * Author: Zachary Smith
  * Last Modified: 16 September 2019
  * Description: Parses a file using the grammar defined in Project1.pdf
  */
class GuiRDP {
  private static JFrame warning = new JFrame("Warning");
  private static ArrayList<String> tokens = new ArrayList<>();
  private static Gui rootNode;

  //Accepts a single argument for the name of the input file
  public static void main(String args[]) {
    JFrame f;
    JPanel p;
    Integer dims[];
    String fileName = "input";
    ListIterator<String> iterateTokens; //A normal Iterator cannot peek ahead

    Comparable<Integer> asdf = new Comparable<>();

    if(args.length>0) fileName = args[0];
    populate(readFile(fileName));
    iterateTokens = tokens.listIterator();  //must be assigned after calling populate() to avoid throwing a ConcurrentModificationException
    rootNode = new Gui(iterateTokens);

    try{
      if(iterateTokens.hasNext() && rootNode.isKeyword(iterateTokens.next())) {
        rootNode.parse();
      } else throw new ParseError("Improper grammar when defining Window.");
      //also catches ParseError from rootNode.parse() and any methods it calls
    } catch(ParseError err) {
      JOptionPane.showMessageDialog(warning, err);
    }

    dims = rootNode.getDims();
    f = new JFrame(rootNode.getName());

    try {
      f.setSize(new Dimension(dims[0],dims[1]));
    } catch(IndexOutOfBoundsException err) {
      JOptionPane.showMessageDialog(warning, "Improper grammar for dimensions.");
    }

    p = rootNode.getPanel();
		f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		f.add(p);
    f.setResizable(false);
    f.setLocationRelativeTo(null);  //centers frame
		f.setVisible(true);

    warning.dispose();
  }

  //Reads input file 'fileName' and returns an Arraylist, one element per line
  private static ArrayList<String> readFile(String fileName) {
    ArrayList<String> lines = new ArrayList<>();
    String tempLine = null;

    try {
      BufferedReader in = new BufferedReader(new FileReader(fileName));
      while((tempLine = in.readLine()) != null)
        lines.add(tempLine);
      in.close();
      //JOptionPane.showMessageDialog(warning,"File read successfully.");
    } catch(FileNotFoundException err) {
      JOptionPane.showMessageDialog(warning,"File did not open.");
    } catch(IOException err) {
      JOptionPane.showMessageDialog(warning,"There was a problem reading from the file.");
    }

    return lines;
  }

  private static void populate(ArrayList<String> s) {
    Iterator<String> iterateLines = s.iterator();
    ArrayList<String> line;
    String[] lineArray;

    while(iterateLines.hasNext()) {
      lineArray = iterateLines.next().split(" ");
      line = new ArrayList<String>(Arrays.asList(lineArray));
      tokens.addAll(line);
    }
  }
} //END CLASS GuiRDP


/**
  * Class Gui
  * Author: Zachary Smith
  * Last Modified: 16 September 2019
  * Description: Parses a single production, forms root node of parse tree.
  *   gui ::=
  *      Window STRING '(' NUMBER ',' NUMBER ')' layout widgets End '.'
  */
class Gui extends ParseTreeNode {
  private Layout childLayout;
  private Widgets childWidgets;

  public Gui(ListIterator<String> itr) {
    keywords = new String[]{"Window"};
    this.haveAttr = new TreeMap<>();
    this.itr = itr;
  }

  //Checks each token of ArrayList<String> against the definition of gui, recursive to children of this node on the parse tree, childLayout and childWidgets
  //TODO: remember to check at the end that we have all class variables needed in the tree and that layoutType adheres to its type definition, same for widget, widgets and layout_type.
  public boolean parse() throws ParseError {
    this.haveAttr.put("name",false);
    this.haveAttr.put("width",false);
    this.haveAttr.put("height",false);
    this.haveAttr.put("childLayout",false);
    this.haveAttr.put("childWidgets",false);
    this.hangingParen = false;
    this.haveComma = false;

    //Begin parsing tokens sequentially and building parse tree top-down
    while(itr.hasNext()) {
      this.currToken = itr.next();

      /* NEW REGEX METHODS
      if(!this.haveAttr.get("name")) parseName();
      parseOpenParenMk2();
      while(this.hangingParen) parseNumber();
      //after revamping parseNumber(), the iteration below became needed
      if(this.itr.hasNext()) this.currToken = this.itr.next();
      else throw new ParseError("Improper layout definition.");
      //*/

      //* OLD REGEX METHODS
      if(!this.haveAttr.get("name") && parseName()) continue;
      else parseOpenParen();
      if(parseCommaAndTokens()) continue;
      else if(parseFirstNumber()) continue;
      parseComma();
      if(parseSecondNumber()) continue;
      else parseCloseParen();
      //*/

      //Ensure we have name, width, height, and that parens are closed - new regex method calls should do this
      if(this.haveAttr.get("name") && this.haveAttr.get("width") && this.haveAttr.get("height") && this.hangingParen==false) {// && this.haveComma==true) {

        //Gather the last of the information needed to define a JPanel
        if(!this.haveAttr.get("childLayout")) {
          this.childLayout = new Layout(this.itr);
          if(this.childLayout.isKeyword(this.currToken) && this.childLayout.parse()) this.pnl = childLayout.getPanel();
          else throw new ParseError("Improper grammar when defining layout.");
          this.haveAttr.put("childLayout",true);
        }

        if(!this.haveAttr.get("childWidgets")) {
          this.childWidgets = new Widgets(this.itr,this.pnl);
          //*
          if(this.itr.hasNext()) this.currToken = this.itr.next();
          else throw new ParseError("Improper grammar when defining widgets.1");
          //*/
        }   //we want more than one Widget to ping isKeyword and thus parse
        if(this.childWidgets.isKeyword(this.currToken) && this.childWidgets.parse()) this.haveAttr.put("childWidgets",true);
        else throw new ParseError("Improper grammar when defining widgets.2");
        //} //we want more than one Widget to ping isKeyword and thus parse
      }

      //Match "End." or "End ." to exit the program successfully
      //TODO - will never make it here as long as "End" is a Widgets keyword. Do i need a "done with widgets" flag?
      else if(this.currToken.matches("^End\\.*$")) {
        if(!this.currToken.endsWith(".")) {
          if(this.itr.hasNext()) this.currToken = this.itr.next();
          else throw new ParseError("Could not find End of program.");
          if(!this.currToken.equals(".")) throw new ParseError("Could not find End of program.");
        }
        System.out.println("File parsed successfully");
        return true;
      }
    } //end loop over iterator

    itr = null;
    return false;
  } //end method parse
} //END CLASS Gui


/**
  * Class Layout
  * Author: Zachary Smith
  * Last Modified: 16 September 2019
  * Description: Parses a single production.
  *   layout ::=
  *      Layout layout_type ':'
  */
class Layout extends ParseTreeNode {
  private LayoutType childLT;

  public Layout(ListIterator<String> itr) {
    keywords = new String[]{"Layout"};
    this.itr = itr;
    this.haveAttr = new TreeMap<>();
  }

  public boolean parse() throws ParseError {
    Integer dims[], gaps[];

    if(this.itr.hasNext()) this.currToken = this.itr.next();
    else throw new ParseError("Improper grammar when defining layout.");

    this.childLT = new LayoutType(this.itr);
    this.haveColon = false;

    if(!this.childLT.isKeyword(this.currToken) || !this.childLT.parse()) return false;

    if(this.childLT.getKeyword().startsWith("Flow")) {
      this.pnl = new JPanel(new FlowLayout());
    } else if(this.childLT.getKeyword().startsWith("Grid")) {
      dims = this.childLT.getDims();
      gaps = this.childLT.getGaps();

      if(dims==null || dims.length!=2) throw new ParseError("Wrong number of of layout arguments.");

      if(gaps!=null) {
        if(gaps.length!=2) throw new ParseError("Wrong number of of layout arguments.");
        this.pnl = new JPanel(new GridLayout(dims[0],dims[1],gaps[0],gaps[1]));
      } else {
        this.pnl = new JPanel(new GridLayout(dims[0],dims[1]));
      }
    } //endif

    return true;
  }
} //END CLASS Layout


/**
  * Class LayoutType
  * Author: Zachary Smith
  * Last Modified: 16 September 2019
  * Description: Parses a single production.
  *   layout_type ::=
  *      Flow |
  *      Grid '(' NUMBER ',' NUMBER [',' NUMBER ',' NUMBER] ')'
  */
class LayoutType extends ParseTreeNode {
  public LayoutType(ListIterator<String> itr) {
    this.keywords = new String[]{"Flow","Grid"};
    this.haveAttr = new TreeMap<>();
    this.itr = itr;
  }

  public String getKeyword() {return this.keyword;}
  public Integer[] getGaps() {
    if(!this.haveAttr.get("gridHGap") || !this.haveAttr.get("gridVGap")) return null;
    else return new Integer[]{this.gridHGap,this.gridVGap};
  }

  //this.isKeyword(String) must have returned true before this method.
  public boolean parse() throws ParseError {
    this.haveAttr.put("keyword",false);
    this.haveAttr.put("width",false);
    this.haveAttr.put("height",false);
    this.haveAttr.put("gridHGap",false);
    this.haveAttr.put("gridVGap",false);
    this.currToken = this.keyword;
    parseOpenParenMk2();
    while(this.hangingParen) parseNumber();
    if(!this.keyword.equals("Flow") && (!this.haveAttr.get("width") || !this.haveAttr.get("height") || (this.haveAttr.get("gridHGap") ^ this.haveAttr.get("gridVGap"))))
      throw new ParseError("Improper number of dimensions");
    if(this.haveColon || (this.itr.hasNext() && this.itr.next().equals(":"))) {
      return true;
    }
    return false;
  }
} //END CLASS LayoutType


/**
  * Class Widgets
  * Author: Zachary Smith
  * Last Modified: 16 September 2019
  * Description: Parses a single production.
  *   widgets ::=
  *      widget widgets |
  *      widget
  */
class Widgets extends ParseTreeNode {
  private Widget childW;
  private Widgets childWs;  //might not exist

  public Widgets(ListIterator<String> itr, JPanel pnl) {
    this.keywords = new String[]{"Button","Group","Label","Panel","Textfield","End","Radio","Layout"};
    this.haveAttr = new TreeMap<>();
    this.itr = itr;
    this.pnl = pnl;
    //this.childW = new Widget(itr); //Allocating Widgets.childW and Widget.childWs in contructor leads to recursive behavior
    this.haveAttr.put("childW",false);
    this.haveAttr.put("childWs",false);
  }

  //public boolean isKeyword(String s) {return this.childW.isKeyword(s);}

  //Be careful not to match childW and childWs in the same loop iteration. A consequence of invoking isKeyword down the call stack each time is that all Widgets instances will have the same keyword as the most recent Widget - not important for building gui.
  public boolean parse() throws ParseError {
    this.currToken = this.keyword;

    System.out.println("\n"+this+": have childW - "+this.haveAttr.get("childW"));

    if(!this.haveAttr.get("childW")) {
      this.childW = new Widget(this.itr,this.pnl);
      if(this.childW.isKeyword(currToken) && this.childW.parse()) this.pnl = childW.getPanel();
      else throw new ParseError("Improper grammar when defining widget.");
      this.haveAttr.put("childW",true);
      System.out.println("\n"+this+"...\nchildW: "+this.childW);
    } else {

      System.out.println(this+": have childWs - "+this.haveAttr.get("childWs"));

      if(!this.haveAttr.get("childWs")) {
        this.childWs = new Widgets(this.itr, this.pnl);
        this.haveAttr.put("childWs",true);
        System.out.println("\n"+this+"...\nchildWs: "+this.childWs);
      }
      if(!this.childWs.isKeyword(this.currToken) || !this.childWs.parse()) return false;
    }

    return true;
  }
} //END CLASS Widgets


/**
  * Class Widget
  * Author: Zachary Smith
  * Last Modified: 16 September 2019
  * Description: Parses a single production.
  *   widget ::=
  *      Button STRING ';' |
  *      Group radio_buttons End ';' |
  *      Label STRING ';' |
  *      Panel layout widgets End ';' |
  *      Textfield NUMBER ';'
  */
class Widget extends ParseTreeNode {
  private RadioButtons childRBs;
  private Layout childL;
  private Widgets childWs;
  private JComponent widgetComponent;
  private ButtonGroup rbGroup;

  public Widget(ListIterator<String> itr, JPanel pnl) {
    this.keywords = new String[]{"Button", "Group", "Label", "Panel", "Textfield", "End", "Radio", "Layout"};
    this.haveAttr = new TreeMap<>();
    this.itr = itr;
    this.pnl = pnl;
  }

  public String getKeyword() {return this.keyword;}

  //TODO
  public boolean parse() throws ParseError {
    this.haveAttr.put("keyword",false);
    this.haveAttr.put("name",false);
    this.haveAttr.put("width",false);
    this.haveAttr.put("height",false);
    this.haveAttr.put("childRBs",false);
    this.haveAttr.put("childL",false);
    this.haveAttr.put("childWs",false);
    this.haveSemi = false;

    if(this.itr.hasNext()) this.currToken = this.itr.next();
    if(this.currToken.endsWith(";")) {
      this.haveSemi = true;
      this.currToken = this.currToken.replace(";","");
    }

    System.out.println("\n"+this+":\nkeyword: "+this.keyword+"\ncurrToken: "+this.currToken);

    switch(this.keyword) {
      case "Button":    parseButton();
                        break;
      case "Label":     parseLabel();
                        break;
      case "Textfield": parseTextfield();
                        break;
      case "Group":     parseGroup();
                        break;
      case "Panel":     parsePanel();
                        break;
      default:          //return false;
    }
    this.haveAttr.put("keyword",true);
    return true;
  } //end method parse

  void parsePanel() throws ParseError {
    boolean done = false;

    do {
      if(this.currToken.startsWith("End")) {
        if(!this.currToken.equals("End;")) {
          if(this.itr.hasNext()) {
            this.currToken = this.itr.next();
          } else {
            throw new ParseError("Improper grammar for End statement.");
          }
        }
        if(this.currToken.endsWith(";")) {
          done = true;
          continue;
        }
      }

      if(!this.haveAttr.get("childL")) {
        this.childL = new Layout(this.itr);
        if(this.childL.isKeyword(this.currToken) && this.childL.parse()) this.haveAttr.put("childL",true);
        else throw new ParseError("Improper grammar when defining layout.");
      }
      if(!this.haveAttr.get("childWs")) {
        this.childWs = new Widgets(this.itr,this.childL.getPanel());
        if(this.itr.hasNext()) this.currToken = this.itr.next();
        else throw new ParseError("Improper grammar when defining widgets.1");
      }
      if(this.childWs.isKeyword(this.currToken) && this.childWs.parse()) this.haveAttr.put("childWs",true);
      else throw new ParseError("Improper grammar when defining widgets.2");

      if(this.itr.hasNext()) this.currToken = this.itr.next();
    } while(!done);

    this.widgetComponent = this.childL.getPanel();
    this.pnl.add(this.widgetComponent);
    System.out.println("Successfully parsed a panel.");
  }

  void parseTextfield() throws ParseError {
    parseNumber();
    if(!this.haveSemi) {
      if(this.itr.hasNext()) {
        this.currToken = this.itr.next();
      } else throw new ParseError("Improperly terminated statement.");
    }
    if(this.haveAttr.get("width")) {
      this.widgetComponent = new JTextField(this.width);
      this.pnl.add(this.widgetComponent);
      System.out.println("Successfully parsed a text field.");
    }
  }

  void parseButton() throws ParseError {
    parseName();
    if(!this.haveSemi) {
      if(this.itr.hasNext()) {
        this.currToken = this.itr.next();
      } else throw new ParseError("Improperly terminated statement.");
    }
    if(this.haveAttr.get("name")) {
      this.widgetComponent = new JButton(this.name);
      this.pnl.add(this.widgetComponent);
      System.out.println("Successfully parsed a button.");
    }
  }

  void parseLabel() throws ParseError {
      parseName();
      if(!this.haveSemi) {
        if(this.itr.hasNext()) {
          this.currToken = this.itr.next();
        } else throw new ParseError("Improperly terminated statement.");
      }
      if(this.haveAttr.get("name")) {
        this.widgetComponent = new JLabel(this.name);
        this.pnl.add(this.widgetComponent);
        System.out.println("Successfully parsed a label.");
      }
  }

  void parseGroup() throws ParseError {
    boolean done = false;
    this.rbGroup = new ButtonGroup();

    do {
      if(this.currToken.startsWith("End")) {
        if(!this.currToken.equals("End;")) {
          if(this.itr.hasNext()) {
            this.currToken = this.itr.next();
          } else {
            throw new ParseError("Improper grammar for End statement.");
          }
        }
        if(this.currToken.endsWith(";")) {
          done = true;
          continue;
        }
      }

      System.out.println(this+" currToken: "+currToken);

      if(!this.haveAttr.get("childRBs")) this.childRBs = new RadioButtons(this.itr,this.pnl,this.rbGroup);

      if(this.childRBs.isKeyword(this.currToken) && this.childRBs.parse()) this.haveAttr.put("childRBs",true);
      else throw new ParseError("Improper grammar when defining radio button.");

      if(this.itr.hasNext()) this.currToken = this.itr.next();
    } while(!done);


    System.out.println("Successfully parsed a group.");
  }
} //END CLASS Widget


/**
  * Class RadioButtons
  * Author: Zachary Smith
  * Last Modified: 16 September 2019
  * Description: Parses a single production.
  *   radio_buttons ::=
  *      radio_button radio_buttons |
  *      radio_button
  */
class RadioButtons extends ParseTreeNode {
  private RadioButton childRB; //has to look ahead at next token
  private RadioButtons childRBs = null;  //might not exist
  private ButtonGroup rbGroup;

  public RadioButtons(ListIterator<String> itr, JPanel pnl, ButtonGroup rbGroup) {
    this.keywords = new String[]{"Radio"};
    this.haveAttr = new TreeMap<>();
    this.itr = itr;
    this.pnl = pnl;
    this.rbGroup = rbGroup;
    this.haveAttr.put("childRB",false);
    this.haveAttr.put("childRBs",false);
  }

  //TODO
  public boolean parse() throws ParseError {
    this.currToken = this.keyword;

    System.out.println("\n"+this+": have childRB - "+this.haveAttr.get("childRB"));

    if(!this.haveAttr.get("childRB")) {
      this.childRB = new RadioButton(this.itr,this.pnl,this.rbGroup);
      if(this.childRB.isKeyword(currToken) && this.childRB.parse()) this.haveAttr.put("childRB",true);
      else throw new ParseError("Improper grammar when defining radio button.");
      System.out.println("\n"+this+"...\nchildRB: "+this.childRB);
    } else {

      System.out.println(this+": have childRBs - "+this.haveAttr.get("childRBs"));

      if(!this.haveAttr.get("childRBs")) {
        this.childRBs = new RadioButtons(this.itr, this.pnl, this.rbGroup);
        this.haveAttr.put("childRBs",true);
        System.out.println("\n"+this+"...\nchildRBs: "+this.childRBs);
      }
      if(!this.childRBs.isKeyword(this.currToken) || !this.childRBs.parse()) return false;
    }

    return true;
  }
} //END CLASS RadioButtons


/**
  * Class RadioButton
  * Author: Zachary Smith
  * Last Modified: 16 September 2019
  * Description: Parses a single production.
  *   radio_button ::=
  *      Radio STRING ';'
  */
class RadioButton extends ParseTreeNode {
  JRadioButton rb;
  ButtonGroup rbGroup;

  public RadioButton(ListIterator<String> itr, JPanel pnl, ButtonGroup rbGroup) {
    this.keywords = new String[]{"Radio"};
    this.haveAttr = new TreeMap<>();
    this.itr = itr;
    this.pnl = pnl;
    this.rbGroup = rbGroup;
    this.haveSemi = false;
  }

  //TODO
  public boolean parse() throws ParseError {
    if(this.itr.hasNext()) this.currToken = this.itr.next();
    else throw new ParseError("Improper grammar when defining radio button group.");
    parseName();
    if(!this.haveSemi) {
      if(this.itr.hasNext()) {
        this.currToken = this.itr.next();
      } else throw new ParseError("Improperly terminated statement.");
    }
    if(this.haveAttr.get("name")) {
      this.rb = new JRadioButton(this.name);
      this.pnl.add(this.rb);
      this.rbGroup.add(this.rb);
      System.out.println("Successfully parsed a radio button.");
      return true;
    }
    return false;
  }
} //END CLASS RadioButton


/**
  * Class ParseTreeNode
  * Author: Zachary Smith
  * Last Modified: 16 September 2019
  * Description: Contains behavior common to nodes of the parse tree, parse tokens when building the tree
  */
abstract class ParseTreeNode {
  ListIterator<String> itr;
  JPanel pnl;
  TreeMap<String,Boolean> haveAttr;// = new TreeMap<>(); //keep track of what tokens have been parsed as window component attributes - name,width,height,childLayout,etc.
  boolean haveComma = false;
  boolean hangingParen = false;
  boolean haveColon = false;
  boolean haveSemi = false;
  String currToken = "";
  String[] keywords;
  String keyword = ""; //the string being tested in isKeyword(String) if matched
  String name = "";
  Integer width;
  Integer height;
  Integer gridHGap;
  Integer gridVGap;

  abstract boolean parse() throws ParseError; //must be implemented by children
  public String getName() {return this.name;}
  public Integer[] getDims() {return new Integer[]{this.width,this.height};}
  public JPanel getPanel() {return this.pnl;}

  //Check with a ParseTreeNode child whether a token matches one of its keywords
  //Edited 21 Sep 2019 so returns true if string BEGINS WITH a keyword instead of a 1-1 match, so that calling LayoutType.isKeyword(s) will set this.keyword as the layout type specified
  public boolean isKeyword(String s) {
      for(int i=0; i<this.keywords.length; i++) {
        if(s.startsWith(this.keywords[i])) {//if(keywords[i].equals(s)) {
          this.keyword = s;
          return true;
        }
      }
      return false;
  }

  //Match a STRING of zero or more characters surrounded by double quotes. MODIFIES ATTRIBUTE name
  boolean parseName() {
    if(this.currToken.matches("^\".*\";?$")) {
      if(this.currToken.endsWith(";")) {
        this.currToken = this.currToken.replace(";","");
        this.haveSemi = true;
      }
      this.name = this.currToken.replace("\"","");
      this.haveAttr.put("name",true);
      System.out.println("parseName found name "+this.name+" in "+this);
      return true;
    }
    return false;
  }

  //* Match an open paren with possible TOKEN, to be ready for parsing dimensions
  //TODO: also parse a leading TOKEN
  boolean parseOpenParen() throws ParseError  {
    if(this.currToken.matches("^\\(.*")) {
      if(!this.haveAttr.get("name")) throw new ParseError("Could not find name/label.");
      this.hangingParen = true;
      if(this.currToken.length() > 1) this.currToken = this.currToken.replace("(","");
      else if(itr.hasNext()) this.currToken = itr.next();
      else throw new ParseError("Improper grammar for dimensions.");
      return true;
    }
    return false;
  }

  //Match a TOKEN comma TOKEN. Complains if not NUMBERs. May end with close paren, but must have been preceded with an open paren. MODIFIES ATTRIBUTES width,height
  boolean parseCommaAndTokens() throws ParseError {
    if(this.hangingParen && this.currToken.matches("^.+\\,.+\\)?$")) {
      if(this.currToken.endsWith(")")) {
        this.currToken = this.currToken.replace(")","");
        this.hangingParen = false;
      }
      if(this.haveAttr.get("width") || this.haveAttr.get("height")) throw new ParseError("Too many arguments for dimensions.");
      String dimensions[] = this.currToken.split("\\,");
      try {
        this.width = Integer.parseInt(dimensions[0]);
        this.height = Integer.parseInt(dimensions[1]);
      } catch(NumberFormatException nfe) {
        System.out.println(nfe);
        throw new ParseError("Improper grammar for dimensions.");
      } catch(IndexOutOfBoundsException ioobe) {
        System.out.println(ioobe);
        throw new ParseError("Improper grammar for dimensions.");
      }
      this.haveComma = true;
      this.haveAttr.put("width",true);
      this.haveAttr.put("height",true);
      System.out.println("Found dimensions "+dimensions[0]+", "+dimensions[1]);
      return true;
    }
    return false;
  }

  //Match a TOKEN. Complains if not a NUMBER. May end with a comma, but must have been preceded with an open paren. The method must not have found width. MODIFIES ATTRIBUTE width
  boolean parseFirstNumber() throws ParseError {
    if(!this.haveAttr.get("width") && this.hangingParen && this.currToken.matches("^.+\\,?$")) {
      if(this.currToken.endsWith(",")) {
        this.currToken = this.currToken.replace(",","");
        this.haveComma = true;
      }
      try {
        this.width = Integer.parseInt(this.currToken);
      } catch(NumberFormatException nfe) {
        System.out.println(nfe);
        throw new ParseError("Improper grammar for window dimensions.");
      }
      this.haveAttr.put("width",true);
      System.out.println("parseFirstNumber found width "+this.width);
      return true;
    }
    return false;
  }

  //Match a lone comma.
  boolean parseComma() throws ParseError {
    if(currToken.equals(",")) {
      if(this.haveComma) throw new ParseError("Improper grammar for dimensions.");
      if(this.itr.hasNext()) this.currToken = this.itr.next();
      this.haveComma = true;
      return true;
    }
    return false;
  }

  //Match a TOKEN. Complains if not a NUMBER. May begin with a comma, may end with a close paren. Must have been preceded with an open paren. The method must have found width but not height.
  boolean parseSecondNumber() throws ParseError {
    if(this.haveAttr.get("width") && !this.haveAttr.get("height") && this.hangingParen && currToken.matches("^\\,?.+\\)?$")) {
      if(this.currToken.startsWith(",")) {
        if(this.haveComma) throw new ParseError("Improper grammar for window dimensions.");
        this.currToken = this.currToken.replace(",","");
        this.haveComma = true;
      }
      if(this.currToken.endsWith(")")) {
        this.currToken = currToken.replace(")","");
        this.hangingParen = false;
      }
      try {
        this.height = Integer.parseInt(currToken);
      } catch(NumberFormatException nfe) {
        System.out.println(nfe);
        throw new ParseError("Improper grammar for window dimensions.");
      }
      this.haveAttr.put("height",true);
      System.out.println("parseSecondNumber found height "+this.height);
      return true;
    }
    return false;
  }

  // Match a close paren. May end with a TOKEN
  boolean parseCloseParen() throws ParseError {
    if(this.currToken.matches("^\\).*$")) {
      if(!this.hangingParen) throw new ParseError("Found a close paren without a matching open paren.");
      else if(!this.haveAttr.get("width")) throw new ParseError("Could not find width.");
      else if(!this.haveAttr.get("height")) throw new ParseError("Could not find height.");
      if(this.currToken.length() > 1) this.currToken = this.currToken.replace(")","");
      else if(this.itr.hasNext()) this.currToken = this.itr.next();
      else throw new ParseError("Improper grammar for layout.");
      this.hangingParen = false;
      return true;
    }
    return false;
  } //*/

  boolean parseOpenParenMk2() throws ParseError {
    //if(this.currToken.matches("^[a-zA-Z]*(\\(([0-9]*\\,)*[0-9]*)?\\)?$")) {
      String operands[] = this.currToken.split("\\(");
      if(operands.length>2) {
        throw new ParseError("Too many open parens were found.");
      } else if(operands.length==2) { //found one open paren
        this.hangingParen=true;
        this.currToken=operands[1];
        parseNumber();
      } else if(!operands[0].startsWith("Flow")) {
        if(this.itr.hasNext()) this.currToken = this.itr.next(); //would returning be better?
        else throw new ParseError("Improper grammar when defining layout type.");
      }
      if(operands[0].length()>1) { //this block is troublesome in how it loosly assigns to keyword
        if(operands[0].endsWith(":")) this.haveColon = true;
        this.keyword=operands[0].replace(":","");
        this.haveAttr.put("keyword",true);
        System.out.println("parseOpenParen found keyword "+this.keyword);
      } //could be either Flow or Grid at this point
      if(this.keyword.equals("Flow")) {
        return true;
      }
      if(!this.hangingParen) {
        return parseOpenParenMk2();
      } //else this.currToken = this.itr.previous();
      return true;
    //} else return false;
  }

  boolean parseNumber() throws ParseError {
    String operands[] = this.currToken.split("\\,");
    String operand = "";
    for(int i=0;i<operands.length;i++) {
      if(!operands[i].isEmpty()) {
        operand = operands[i];
        break;
      }
    }
    if(operand.isEmpty()) {  //if encounter a lone comma
      if(this.itr.hasNext()) this.currToken = this.itr.next();
      else throw new ParseError("Improper grammar for dimensions.");
    }
    if(operand.endsWith(":")) {
      operand = operand.replace(":","");
      this.haveColon = true;
    }
    if(operand.endsWith(")")) {
      operand = operand.replace(")","");
      this.hangingParen = false;
    }
    try {
      if(!this.haveAttr.get("width")) {
        this.width = Integer.parseInt(operand);
        this.haveAttr.put("width",true);
        System.out.println("parseNumber found width "+this.width);
      } else if(!this.haveAttr.get("height")) {
        this.height = Integer.parseInt(operand);
        this.haveAttr.put("height",true);
        System.out.println("parseNumber found height "+this.height);
      } else if(this.keyword.equals("Grid")) {
        if(!this.haveAttr.get("gridHGap")) {
          this.gridHGap = Integer.parseInt(operand);
          this.haveAttr.put("gridHGap",true);
          System.out.println("parseNumber found horizontal gap "+this.gridHGap);
        } else if(!this.haveAttr.get("gridVGap")) {
          this.gridVGap = Integer.parseInt(operand);
          this.haveAttr.put("gridVGap",true);
          System.out.println("parseNumber found vertical gap "+this.gridVGap);
        } else throw new ParseError("Too many number arguments.1");
      } else throw new ParseError("Too many number arguments.2");
    } catch(NumberFormatException nfe) {
      System.out.println(nfe);
      throw new ParseError("Improper grammar for dimensions.");
    }
    this.currToken = this.currToken.replaceFirst(".*?\\,",""); //needs to be lazy
    if(this.hangingParen && (operands.length<2 || operands[1].isEmpty())) {
      if(this.itr.hasNext()) this.currToken = this.itr.next();
      else throw new ParseError("Improper grammar for dimensions.");
    }
    return true;
  }
} //END CLASS ParseTreeNode


/**
  * Class ParseError
  * Author: http://courses.ics.hawaii.edu/ReviewICS111/javanotes7.0.1-web-site/c9/s5.html
  * Last Modified: 16 September 2019
  * Description: Indicates that the input file's grammar is not properly formed.
  */
class ParseError extends Exception {
  ParseError(String message) {
    super(message);
  }
} //END CLASS ParseError
