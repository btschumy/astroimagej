package ij.plugin;
import java.awt.*;
import java.io.*;
import java.awt.event.*;
import ij.*;
import ij.gui.*;
import ij.macro.*;
import ij.text.*;
import ij.util.Tools;
import ij.io.*;
import ij.macro.MacroConstants;
import ij.plugin.frame.*;
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                import java.util.*;                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                            

/** This plugin implements the Plugins/Macros/Install Macros command. It is also used by the Editor
	class to install macros in menus and by the ImageJ class to install macros at startup. */
public class MacroInstaller implements PlugIn, MacroConstants, ActionListener {

	public static final int MAX_SIZE = 28000, MAX_MACROS=100, XINC=10, YINC=18;
	public static final char commandPrefix = '^';
	static final String commandPrefixS = "^";
	static final int MACROS_MENU_COMMANDS = 7; // number of commands in Plugins>Macros submenu
	
	private int[] macroStarts;
	private String[] macroNames;
	private MenuBar mb = new MenuBar();
	private int nMacros;
	private Program pgm;
	private boolean firstEvent = true;
	private String shortcutsInUse;
	private int inUseCount;
	private int nShortcuts;
	private int toolCount;
	private String text;
	private String anonymousName;
	private Menu macrosMenu;
	private int autoRunCount, autoRunAndHideCount;
	private boolean openingStartupMacrosInEditor;
	private boolean installTools = true;
	
	private static String defaultDir, fileName;
	private static MacroInstaller instance, listener;
	private Thread macroToolThread;
	private ArrayList<Menu> subMenus = new ArrayList();
	
	private static Program autoRunPgm;
	private static int autoRunAddress;
	private static String autoRunName;
	private boolean autoRunOnCurrentThread;
	
	public void run(String path) {
		if (path==null || path.equals(""))
			path = showDialog();
		if (path==null) return;
		openingStartupMacrosInEditor = path.indexOf("StartupMacros")!=-1;
		String text = open(path);
		if (text!=null) {
			String functions = Interpreter.getAdditionalFunctions();
			if (functions!=null) {
				if (!(text.endsWith("\n") || functions.startsWith("\n")))
					text = text + "\n" + functions;
				else
					text = text + functions;
			}
			install(text);
		}
	}
			
	void install() {
		subMenus.clear();
		if (text!=null) {
			Tokenizer tok = new Tokenizer();
			pgm = tok.tokenize(text);
		}
		if (macrosMenu!=null)
			IJ.showStatus("");
		int[] code = pgm.getCode();
		Symbol[] symbolTable = pgm.getSymbolTable();
		int count=0, token, nextToken, address;
		String name;
		Symbol symbol;
		shortcutsInUse = null;
		inUseCount = 0;
		nShortcuts = 0;
		toolCount = 0;
		macroStarts = new int[MAX_MACROS];
		macroNames = new String[MAX_MACROS];
		boolean isPluginsMacrosMenu = false;
		if (macrosMenu!=null) {
			int itemCount = macrosMenu.getItemCount();
			isPluginsMacrosMenu = macrosMenu==Menus.getMacrosMenu();
			int baseCount =isPluginsMacrosMenu?MACROS_MENU_COMMANDS:Editor.MACROS_MENU_ITEMS;
			if (itemCount>baseCount) {
				for (int i=itemCount-1; i>=baseCount; i--)
					macrosMenu.remove(i);
			}
		}
		if (pgm.hasVars() && pgm.macroCount()>0 && pgm.getGlobals()==null)
			new Interpreter().saveGlobals(pgm);
		ArrayList tools = new ArrayList();
		for (int i=0; i<code.length; i++) {
			token = code[i]&TOK_MASK;
			if (token==MACRO) {
				nextToken = code[i+1]&TOK_MASK;
				if (nextToken==STRING_CONSTANT) {
					if (count==MAX_MACROS) {
						if (isPluginsMacrosMenu)
							IJ.error("Macro Installer", "Macro sets are limited to "+MAX_MACROS+" macros.");
						break;
					}
					address = code[i+1]>>TOK_SHIFT;
					symbol = symbolTable[address];
					name = symbol.str;
					macroStarts[count] = i + 2;
					macroNames[count] = name;
					if (name.indexOf('-')!=-1 && (name.indexOf("Tool")!=-1||name.indexOf("tool")!=-1)) {
						tools.add(name);
						toolCount++;
					} else if (name.startsWith("AutoRun")) {
						if (autoRunCount==0 && !openingStartupMacrosInEditor && !IJ.isMacro()) {
							if (autoRunOnCurrentThread) { //autoRun() method will run later
								autoRunPgm = pgm;
								autoRunAddress = macroStarts[count];
								autoRunName = name;
							} else
								new MacroRunner(pgm, macroStarts[count], name, (String)null); // run on separate thread
							if (name.equals("AutoRunAndHide"))
								autoRunAndHideCount++;
						}
						autoRunCount++;
						count--;
					} else if  (name.equals("Popup Menu"))
						installPopupMenu(name, pgm);
					else if (!name.endsWith("Tool Selected")) { 
						if (macrosMenu!=null) {
							addShortcut(name);
							int pos = name.indexOf(">");
							boolean inSubMenu = name.startsWith("<") && (pos>1);
							if (inSubMenu) {
								Menu parent = macrosMenu;
								Menu subMenu = null;
								String parentStr = name.substring(1, pos).trim();
								String childStr = name.substring(pos + 1).trim();
								MenuItem mnuItem = new MenuItem();
								mnuItem.setActionCommand(name);
								mnuItem.setLabel(childStr);
								for (int jj = 0; jj < subMenus.size(); jj++) {
									String aName = subMenus.get(jj).getName();
									if (aName.equals(parentStr))
										subMenu = subMenus.get(jj);
								}
								if (subMenu==null) {
									subMenu = new Menu(parentStr);
									subMenu.setName(parentStr);
									subMenu.addActionListener(this);
									subMenus.add(subMenu);
									parent.add(subMenu);
								}
								subMenu.add(mnuItem);
							} else
								macrosMenu.add(new MenuItem(name));
						}
					}
					count++;
				}					
			} else if (token==EOF)
				break;
		}
		nMacros = count;
		if (toolCount>0  && installTools) {
			Toolbar tb = Toolbar.getInstance();
			if (toolCount==1)
				tb.addMacroTool((String)tools.get(0), this);
			else {
				for (int i=0; i<tools.size(); i++) {
					String toolName = (String)tools.get(i);
					if (toolName.startsWith("Abort Macro or Plugin") && toolCount>6)
						toolName = "Unused "+toolName;
					tb.addMacroTool(toolName, this, i);
				}
			}
			if (toolCount>1 && Toolbar.getToolId()>=Toolbar.CUSTOM1)
				tb.setTool(Toolbar.RECTANGLE);
			tb.repaint();
			installTools = false;
		}
		if (macrosMenu!=null)
			this.instance = this;
		if (shortcutsInUse!=null && text!=null)
			IJ.showMessage("Install Macros", (inUseCount==1?"This keyboard shortcut is":"These keyboard shortcuts are")
			+ " already in use:"+shortcutsInUse);
		if (nMacros==0 && fileName!=null) {
			if (text==null||text.length()==0)
				return;
			int dotIndex = fileName.lastIndexOf('.');
			if (dotIndex>0)
				anonymousName = fileName.substring(0, dotIndex);
			else
				anonymousName =fileName;
			if (macrosMenu!=null)
				macrosMenu.add(new MenuItem(anonymousName));
			macroNames[0] = anonymousName;
			nMacros = 1;
		}
		String word = nMacros==1?" macro":" macros";
		if (isPluginsMacrosMenu)
			IJ.showStatus(nMacros + word + " installed");
	}
	
	public int install(String text) {
		if (text==null && pgm==null)
			return 0;
		this.text = text;
		macrosMenu = Menus.getMacrosMenu();
		if (listener!=null)
			macrosMenu.removeActionListener(listener);
		macrosMenu.addActionListener(this);
		listener = this;
		install();
		return nShortcuts;
	}
	
	public int install(String text, Menu menu) {
		this.text = text;
		macrosMenu = menu;
		install();
		return nShortcuts;
	}

	public void installFile(String path) {
		String text = open(path);
		if (text==null) return;
		boolean isStartupMacros = path.contains("StartupMacros");
		if (isStartupMacros && !Toolbar.installStartupMacrosTools())
			installTools = false;
		install(text);
		installTools = true;
		if (isStartupMacros) {
			Toolbar tb = Toolbar.getInstance();
			if (tb!=null)
				tb.installStartupTools();
		}
	}

	public void installTool(String path) {
		String text = open(path);
		if (text!=null)
			installSingleTool(text);
	}

	public void installLibrary(String path) {
			String text = open(path);
			if (text!=null)
				Interpreter.setAdditionalFunctions(text);
	}
	
	 /** Installs a macro set contained in ij.jar. */
	public static void installFromJar(String path) {
		try {
			(new MacroInstaller()).installFromIJJar(path);
		} catch (Exception e) {}
	}
	
	 /** Installs a macro set contained in ij.jar. */
	public void installFromIJJar(String path) {
		boolean installMacros = false;
		if (path.endsWith("MenuTool.txt+")) {
			path = path.substring(0,path.length()-1);
			installMacros = true;
		}
		String text = openFromIJJar(path);
		if (text==null) return;
		if (path.endsWith("StartupMacros.txt")) {
			if (Toolbar.installStartupMacrosTools())
				install(text);
			Toolbar tb = Toolbar.getInstance();
			if (tb!=null)
				tb.installStartupTools();
		} else if (path.contains("Tools") || installMacros) {
			install(text);
		} else
			installSingleTool(text);
	}

	public void installSingleTool(String text) {
		this.text = text;
		macrosMenu = null;
		install();
	}

	void installPopupMenu(String name, Program pgm) {
        Hashtable h = pgm.getMenus();
        if (h==null) return;
        String[] commands = (String[])h.get(name);
        if (commands==null) return;
        PopupMenu popup = Menus.getPopupMenu();
        if (popup==null) return;
		popup.removeAll();
        for (int i=0; i<commands.length; i++) {
			if (commands[i].equals("-"))
				popup.addSeparator();
			else {
				MenuItem mi = new MenuItem(commands[i]);
				mi.addActionListener(this);
				popup.add(mi);
			}
        }
	}

	void removeShortcuts() {
		Menus.getMacroShortcuts().clear();
		Hashtable shortcuts = Menus.getShortcuts();
		for (Enumeration en=shortcuts.keys(); en.hasMoreElements();) {
			Integer key = (Integer)en.nextElement();
			String value = (String)shortcuts.get(key);
			if (value.charAt(0)==commandPrefix)
				shortcuts.remove(key);
		}
	}

	void addShortcut(String name) {
		int index1 = name.indexOf('[');
		if (index1==-1)
			return;
		int index2 = name.lastIndexOf(']');
		if (index2<=(index1+1))
			return;
		String shortcut = name.substring(index1+1, index2);
		int len = shortcut.length();
		if (len>1)
			shortcut = shortcut.toUpperCase(Locale.US);;
		if (len>3 || (len>1 && shortcut.charAt(0)!='F' && shortcut.charAt(0)!='N' && shortcut.charAt(0)!='&'))
			return;
		boolean bothNumKeys = shortcut.startsWith("&");
		if (bothNumKeys){ //first handle num key of keyboard
			shortcut = shortcut.replace("&", "");
			len = shortcut.length();
		}			
		int code = Menus.convertShortcutToCode(shortcut);
		if (code==0)
			return;
		if (nShortcuts==0)
			removeShortcuts();
		// One character shortcuts go in a separate hash table to
		// avoid conflicts with ImageJ menu shortcuts.
		if (len==1 || shortcut.equals("N+") || shortcut.equals("N-") ) {
			Hashtable macroShortcuts = Menus.getMacroShortcuts();
			macroShortcuts.put(Integer.valueOf(code), commandPrefix+name);
			nShortcuts++;
			if(!bothNumKeys)
				return;
		}
		if(bothNumKeys){//now handle numerical keypad
			shortcut = "N" + shortcut;
			code = Menus.convertShortcutToCode(shortcut);
		}
		Hashtable shortcuts = Menus.getShortcuts();
		if (shortcuts.get(Integer.valueOf(code))!=null) {
			if (shortcutsInUse==null)
				shortcutsInUse = "\n \n";
			shortcutsInUse += "	  " + name + "\n";
			inUseCount++;
			return;
		}
		shortcuts.put(Integer.valueOf(code), commandPrefix+name);
		nShortcuts++;
	}
	
	 String showDialog() {
		if (defaultDir==null) defaultDir = Menus.getMacrosPath();
		OpenDialog od = new OpenDialog("Install Macros", defaultDir, fileName);
		String name = od.getFileName();
		if (name==null) return null;
		String dir = od.getDirectory();
		if (!(name.endsWith(".txt")||name.endsWith(".ijm"))) {
			IJ.showMessage("Macro Installer", "File name must end with \".txt\" or \".ijm\" .");
			return null;
		}
		fileName = name;
		defaultDir = dir;
		return dir+name;
	}

	String open(String path) {
		if (path==null) return null;
		try {
			StringBuffer sb = new StringBuffer(5000);
			BufferedReader r = new BufferedReader(new FileReader(path));
			while (true) {
				String s=r.readLine();
				if (s==null)
					break;
				else
					sb.append(s+"\n");
			}
			r.close();
			return new String(sb);
		}
		catch (Exception e) {
			IJ.error(e.getMessage());
			return null;
		}
	}
	 
	 /** Returns a text file contained in ij.jar. */
	 public String openFromIJJar(String path) {
		String text = null;
		  try {
			InputStream is = this.getClass().getResourceAsStream(path);
			if (is==null) return null;
				InputStreamReader isr = new InputStreamReader(is);
				StringBuffer sb = new StringBuffer();
				char [] b = new char [8192];
				int n;
				while ((n = isr.read(b)) > 0)
					 sb.append(b,0, n);
				text = sb.toString();
		  }
		  catch (IOException e) {}
		  return text;
	}
	
	public boolean runMacroTool(String name) {
		for (int i=0; i<nMacros; i++) {
			if (macroNames[i].startsWith(name)) {
				if (macroToolThread!=null && macroToolThread.getName().indexOf(name)!=-1 && macroToolThread.isAlive())
					return false; // do nothing if this tool is already running
				MacroRunner mw = new MacroRunner(pgm, macroStarts[i], name, (String)null);
				macroToolThread = mw.getThread();
				return true;
			}
		}
		return false;
	}
	
	public boolean runMenuTool(String name, String command) {
		for (int i=0; i<nMacros; i++) {
			if (macroNames[i].startsWith(name)) {
				Recorder.recordInMacros = true;
				new MacroRunner(pgm, macroStarts[i], name, command);
				return true;
			}
		}
		return false;
	}

	/** Runs a command in the Plugins/Macros submenu on the current thread. */
	public static boolean runMacroCommand(String name) {
		if (instance==null)
			return false;
		if (name.startsWith(commandPrefixS))
			name = name.substring(1);
		for (int i=0; i<instance.nMacros; i++) {
			if (name.equals(instance.macroNames[i])) {
				MacroRunner mm = new MacroRunner();
				mm.run(instance.pgm, instance.macroStarts[i], name);
				return true;
			}
		}
		return false;
	}
	
	public static void runMacroShortcut(String name) {
		if (instance==null)
			return;
		if (name.startsWith(commandPrefixS))
			name = name.substring(1);
		for (int i=0; i<instance.nMacros; i++) {
			if (name.equals(instance.macroNames[i])) {
				(new MacroRunner()).runShortcut(instance.pgm, instance.macroStarts[i], name);
				return;
			}
		}
	}

	public void runMacro(String name) {
		runMacro(name, null);
	}

	public void runMacro(String name, Editor editor) {
		if (anonymousName!=null && name.equals(anonymousName)) {
			ImageJ.setCommandName(name);
			new MacroRunner(pgm, 0, anonymousName, editor);
			return;
		}
		for (int i=0; i<nMacros; i++)
			if (name.equals(macroNames[i])) {
				ImageJ.setCommandName(name);
				Interpreter.abort(); // abort any currently running macro
				new MacroRunner(pgm, macroStarts[i], name, editor);
				return;
			}
	}
		
	public int getMacroCount() {
		return nMacros;
	}
    
    public Program getProgram() {
		return pgm;
	}

		
	/** Returns true if an "AutoRunAndHide" macro was run/installed. */
	public boolean isAutoRunAndHide() {
		return autoRunAndHideCount>0;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
		openingStartupMacrosInEditor = fileName.startsWith("StartupMacros");
	}

	public static String getFileName() {
		return fileName;
	}
	
	public void actionPerformed(ActionEvent evt) {
		String cmd = evt.getActionCommand();
		ImageJ.setCommandName(cmd);
		MenuItem item = (MenuItem)evt.getSource();
		MenuContainer parent = item.getParent();
		if (parent instanceof PopupMenu) {
			for (int i=0; i<nMacros; i++) {
				if (macroNames[i].equals("Popup Menu")) {
					new MacroRunner(pgm, macroStarts[i], "Popup Menu", cmd);
					return;
				}
			}
		}
		runMacro(cmd);
	}
	
	/** Installs startup macros and runs AutoRun macro on current thread. */
	public void installStartupMacros(String path) {
		autoRunOnCurrentThread = true;
		installFile(path);
		autoRunOnCurrentThread = false;
	}
	
	/** Runs the StartupMacros AutoRun macro on the current thread. */
	public static void autoRun() {
		if (autoRunPgm!=null)
			(new MacroRunner()).run(autoRunPgm, autoRunAddress, autoRunName);
		autoRunPgm = null;
	}

} 



