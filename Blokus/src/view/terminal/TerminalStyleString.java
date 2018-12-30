package view.terminal;

import java.util.ArrayList;

/**
 * Class to display color strings in the terminal
 * @author Etienne MARAIS
 * @version 1.1
 */
public class TerminalStyleString{

	private String str;
	private	ArrayList<String> style = new ArrayList<String>();

	/**
	 * Constructor
	 * @param str the string to stylized
	 * @param color the color of the string
	 * @param bgColor the color of the background
	 * @param effect all the effect you want to use
	 */
	public TerminalStyleString(String str, Colors color, BgColors bgColor, Effects effect){
		this.str = str;
		style.add(0, color.toString());
		style.add(1, bgColor.toString());
		if(effect != Effects.NONE){
			style.add(effect.toString());
		}
	}

	/**
	 * Constructor
	 * @param str the string to stylized
	 * @param color the color of the string
	 * @param bgColor the color of the background
	 */
	public TerminalStyleString(String str, Colors color, BgColors bgColor){
		this(str, color, bgColor, Effects.NONE);
	}

	/**
	 * Constructor
	 * @param str the string to stylized
	 * @param bgColor the color of the background
	 * @param effect all the effect you want to use
	 */
	public TerminalStyleString(String str, BgColors bgColor, Effects effect){
		this(str, Colors.DEFAULT, bgColor, effect);
	}

	/**
	 * Constructor
	 * @param str the string to stylized
	 * @param color the color of the string
	 * @param effect all the effect you want to use
	 */
	public TerminalStyleString(String str, Colors color, Effects effect){
		this(str, color, BgColors.NONE, effect);
	}

	/**
	 * Constructor
	 * @param str the string to stylized
	 * @param color the color of the string
	 */
	public TerminalStyleString(String str, Colors color){
		this(str, color, BgColors.NONE, Effects.NONE);
	}

	/**
	 * Constructor
	 * @param str the string to stylized
	 * @param bgColor the color of the background
	 */
	public TerminalStyleString(String str, BgColors bgColor){
		this(str, Colors.DEFAULT, bgColor, Effects.NONE);
	}

	/**
	 * Constructor
	 * @param str the string to stylized
	 * @param effect all the effect you want to use
	 */
	public TerminalStyleString(String str, Effects effect){
		this(str, Colors.DEFAULT, BgColors.NONE, effect);
	}
	
	/**
	 * Constructor
	 * @param str the string to stylized
	 */
	public TerminalStyleString(String str){
		this(str, Colors.DEFAULT, BgColors.NONE, Effects.NONE);
	}

	/**
	 * Constructor
	 * @param bgColor the color of the background
	 */
	public TerminalStyleString(BgColors bgColor){
		this("", Colors.DEFAULT, bgColor, Effects.NONE);
	}

	/**
	 * Constructor
	 * @param color the color of the string
	 */
	public TerminalStyleString(Colors color){
		this("", color, BgColors.NONE, Effects.NONE);
	}

	/**
	 * Constructor
	 * @param effect all the effect you want to use
	 */
	public TerminalStyleString(Effects effect){
		this("", Colors.DEFAULT, BgColors.NONE, effect);
	}

	/**
	 * Default Constructor
	 */
	public TerminalStyleString(){
		this("", Colors.DEFAULT, BgColors.NONE, Effects.NONE);
	}










	/**
	 * Create a String readable by the terminal
	 * @return the string
	 */
	public String toString(){
		String str = "";
		for (int i = 0 ; i < style.size() ; i++ ) {
			str += style.get(i);
		}
		str += this.str + "\u001b[0m";
		return str;
	}


	/**
	 * Change string's color
	 * @param color the color from Colors enumeration
	 * @return TerminalStyleString corresponding
	 */
	public TerminalStyleString changeColor(Colors color){
		this.style.set(0,color.toString());
		return this;
	}

	/**
	 * Change string's color
	 * @param bgColor the background's color from BgColors enumeration
	 * @return TerminalStyleString corresponding
	 */
	public TerminalStyleString changeBackground(BgColors bgColor){
		this.style.set(1,bgColor.toString());
		return this;
	}

	/**
	 * add string's effect
	 * @param effect the effect from Effects enumeration
	 * @return TerminalStyleString corresponding
	 */
	public TerminalStyleString addEffect(Effects effect){
		if(effect == Effects.NONE){
			for(int i = 2 ; i< style.size() ; i++){
				style.remove(i);
			}
		} else if(!style.contains(effect.toString())){
			this.style.add(effect.toString());
		}
		return this;
	}

	/**
	 * Change string's effect
	 * @param effect the effect from Effects enum
	 * @return TerminalStyleString corresponding
	 */
	public TerminalStyleString removeEffect(Effects effect){
		for(int i = 2 ; i < style.size() ; i++){
			if(style.contains(effect.toString())){
				style.remove(effect.toString());
			}
		}
		return this;
	}

	/**
	 * Change the string
	 * @param str the new string
	 * @return TerminalStyleString corresponding
	 */
	public TerminalStyleString changeString(String str){
		this.str = str;
		return this;
	}











	/**
	 * Method that return an already configurated object of color class
	 * @param str the string you want to color
	 * @return TerminalStyleString BLACK.NONE
	 */
	public static TerminalStyleString BLACK(String str){
		return new TerminalStyleString(str, Colors.BLACK, BgColors.NONE, Effects.NONE);
	}

	/**
	 * Method that return an already configurated object of color class
	 * @param str the string you want to color
	 * @return TerminalStyleString RED.NONE
	 */
	public static TerminalStyleString RED(String str){
		return new TerminalStyleString(str, Colors.RED, BgColors.NONE, Effects.NONE);
	}

	/**
	 * Method that return an already configurated object of color class
	 * @param str the string you want to color
	 * @return TerminalStyleString GREEN.NONE
	 */
	public static TerminalStyleString GREEN(String str){
		return new TerminalStyleString(str, Colors.GREEN, BgColors.NONE, Effects.NONE);
	}

	/**
	 * Method that return an already configurated object of color class
	 * @param str the string you want to color
	 * @return TerminalStyleString YELLOW.NONE
	 */
	public static TerminalStyleString YELLOW(String str){
		return new TerminalStyleString(str, Colors.YELLOW, BgColors.NONE, Effects.NONE);
	}

	/**
	 * Method that return an already configurated object of color class
	 * @param str the string you want to color
	 * @return TerminalStyleString BLUE.NONE
	 */
	public static TerminalStyleString BLUE(String str){
		return new TerminalStyleString(str, Colors.BLUE, BgColors.NONE, Effects.NONE);
	}


	/**
	 * Method that return an already configurated object of color class
	 * @param str the string you want to color
	 * @return TerminalStyleString PURPLE.NONE
	 */
	public static TerminalStyleString PURPLE(String str){
		return new TerminalStyleString(str, Colors.PURPLE, BgColors.NONE, Effects.NONE);
	}

	/**
	 * Method that return an already configurated object of color class
	 * @param str the string you want to color
	 * @return TerminalStyleString BLUE.NONE
	 */
	public static TerminalStyleString LIGHTBLUE(String str){
		return new TerminalStyleString(str, Colors.LIGHTBLUE, BgColors.NONE, Effects.NONE);
	}

	/**
	 * Method that return an already configurated object of color class
	 * @param str the string you want to color
	 * @return TerminalStyleString WHITE.NONE
	 */
	public static TerminalStyleString WHITE(String str){
		return new TerminalStyleString(str, Colors.WHITE, BgColors.NONE, Effects.NONE);
	}










	/**
	 * Enumeration for TerminalStyleString
	 * @version 1.0
	 * @see TerminalStyleString
	 */
	public enum Effects{
		
		NONE("0"),
		BOLD("1"),
		UNDERLIGNE("4"),
		REVERSE("7"),
		CROSS("9");


		private String value;

		/**
		 * Default constructor
		 * @String value the terminal's value
		 */
		private Effects(String value){
			this.value = "\u001b[" + value + "m";
		}

		/**
		 * print the value of the enum constant
		 * @return the value's string
		 */
		@Override
		public String toString(){
			return value;
		}	
	}


	/**
	 * Enumeration for TerminalStyleString
	 * @version 1.0
	 * @see TerminalStyleString
	 */
	public enum Colors{

		BLACK("30"),
		RED("31"),
		GREEN("32"),
		YELLOW("33"),
		BLUE("34"),
		PURPLE("34"),
		LIGHTBLUE("36"),
		WHITE("37"),
		DEFAULT("0");

		private String value;

		/**
		 * Default constructor
		 * @String value the terminal's value
		 */
		private Colors(String value){
			this.value = "\u001b[" + value + "m";
		}

		/**
		 * print the value of the enum constant
		 * @return the value's string
		 */
		@Override
		public String toString(){
			return value;
		}	
	}

		/**
	 * Enumeration for TerminalStyleString
	 * @version 1.0
	 * @see TerminalStyleString
	 */
	public enum BgColors{

		BLACK("40"),
		RED("41"),
		GREEN("42"),
		YELLOW("43"),
		BLUE("44"),
		PURPLE("45"),
		LIGHTBLUE("46"),
		WHITE("47"),
		NONE("0");

		private String value;

		/**
		 * Default constructor
		 * @String value the terminal's value
		 */
		private BgColors(String value){
			if(!value.equals("0")){
				this.value = "\u001b[" + value + "m";
			} else{
				this.value = "";
			}
		}

		/**
		 * print the value of the enum constant
		 * @return the value's string
		 */
		@Override
		public String toString(){
			return value;
		}	
	}

}
